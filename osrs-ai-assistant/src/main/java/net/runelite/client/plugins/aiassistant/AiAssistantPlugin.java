package net.runelite.client.plugins.aiassistant;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.aiassistant.ai.AiProvider;
import net.runelite.client.plugins.aiassistant.ai.AnthropicProvider;
import net.runelite.client.plugins.aiassistant.ai.ConversationHistory;
import net.runelite.client.plugins.aiassistant.ai.OpenAiProvider;
import net.runelite.client.plugins.aiassistant.ai.PromptBuilder;
import net.runelite.client.plugins.aiassistant.ai.UsageTracker;
import net.runelite.client.plugins.aiassistant.data.PlayerDataCollector;
import net.runelite.client.plugins.aiassistant.data.PlayerSnapshot;
import net.runelite.client.plugins.aiassistant.data.SnapshotManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@PluginDescriptor(
	name = "AI Assistant",
	description = "In-game AI chat assistant with context-aware responses",
	tags = {"ai", "assistant", "chat", "openai", "claude"}
)
public class AiAssistantPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private AiAssistantConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private PlayerDataCollector dataCollector;

	@Inject
	private SnapshotManager snapshotManager;

	@Inject
	private ScheduledExecutorService executor;

	private AiAssistantPanel panel;
	private NavigationButton navButton;
	private AiProvider currentProvider;
	private ScheduledFuture<?> snapshotTask;
	private ConversationHistory conversationHistory;
	private UsageTracker usageTracker;

	@Override
	protected void startUp() throws Exception
	{
		log.info("AI Assistant started!");

		// Initialize conversation history and usage tracker
		conversationHistory = new ConversationHistory();
		usageTracker = new UsageTracker();

		// Create UI panel
		panel = new AiAssistantPanel(this);

		// Load icon
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/net/runelite/client/plugins/aiassistant/icon.png");

		// Create navigation button
		navButton = NavigationButton.builder()
			.tooltip("AI Assistant")
			.icon(icon != null ? icon : new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
			.priority(10)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		// Initialize AI provider
		updateAiProvider();

		// Start auto-snapshot if enabled
		if (config.autoSnapshot())
		{
			startAutoSnapshot();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("AI Assistant stopped!");

		// Stop auto-snapshot
		stopAutoSnapshot();

		// Remove UI
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Player logged in");
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			log.debug("Player logged out");
		}
	}

	/**
	 * Processes a user message and sends it to the AI.
	 */
	public void processMessage(String message, Consumer<String> onSuccess, Consumer<String> onError)
	{
		executor.submit(() ->
		{
			try
			{
				// Validate API key
				if (config.apiKey() == null || config.apiKey().isEmpty())
				{
					onError.accept("Please configure your API key in the plugin settings.");
					return;
				}

				// Update provider if needed
				updateAiProvider();

				if (currentProvider == null)
				{
					onError.accept("AI provider not initialized. Please check your configuration.");
					return;
				}

				// Check rate limit (max 10 calls per minute)
				if (usageTracker.isRateLimitExceeded(10))
				{
					onError.accept("Rate limit exceeded. Please wait a moment before sending another message.");
					return;
				}

				// Collect current player data
				PlayerSnapshot snapshot = dataCollector.collectSnapshot();
				String context = snapshot != null ? PromptBuilder.buildContext(snapshot) : "No player data available.";

				// Add user message to history
				conversationHistory.addUserMessage(message);

				// Send message to AI with history and custom system prompt
				log.debug("Sending message to AI: {}", message);
				String response;
				String customPrompt = config.customSystemPrompt();
				boolean useStreaming = config.enableStreaming() && currentProvider.supportsStreaming();

				// Use streaming if enabled and supported
				if (useStreaming)
				{
					// Start streaming display
					if (panel != null)
					{
						panel.startStreamingAiMessage();
					}

					// Send with streaming
					response = currentProvider.sendMessageWithStreaming(
						message,
						context,
						conversationHistory,
						customPrompt,
						chunk -> {
							// Display each chunk as it arrives
							if (panel != null)
							{
								panel.appendStreamingChunk(chunk);
							}
						}
					);

					// Finish streaming display
					if (panel != null)
					{
						panel.finishStreamingAiMessage();
					}
				}
				else
				{
					// Non-streaming fallback
					if (currentProvider instanceof OpenAiProvider)
					{
						response = ((OpenAiProvider) currentProvider).sendMessageWithHistory(message, context, conversationHistory, customPrompt);
					}
					else if (currentProvider instanceof AnthropicProvider)
					{
						response = ((AnthropicProvider) currentProvider).sendMessageWithHistory(message, context, conversationHistory, customPrompt);
					}
					else
					{
						response = currentProvider.sendMessageWithHistory(message, context, conversationHistory);
					}

					// Display complete response
					onSuccess.accept(response);
				}

				// Add AI response to history
				conversationHistory.addAssistantMessage(response);

				// Track API usage (estimate ~500 tokens per call)
				usageTracker.recordCall(500);

				// Log usage stats
				UsageTracker.UsageStats stats = usageTracker.getStats();
				log.debug("Usage stats: {}", stats);
			}
			catch (Exception e)
			{
				log.error("Error processing message", e);
				onError.accept("Error: " + e.getMessage());
			}
		});
	}

	/**
	 * Clears the conversation history.
	 */
	public void clearConversationHistory()
	{
		if (conversationHistory != null)
		{
			conversationHistory.clear();
			log.debug("Conversation history cleared");
		}
	}

	/**
	 * Gets current usage statistics.
	 */
	public UsageTracker.UsageStats getUsageStats()
	{
		return usageTracker != null ? usageTracker.getStats() : null;
	}

	/**
	 * Resets usage statistics.
	 */
	public void resetUsageStats()
	{
		if (usageTracker != null)
		{
			usageTracker.reset();
		}
	}

	/**
	 * Saves a snapshot of the current player state.
	 */
	public void saveSnapshot()
	{
		executor.submit(() ->
		{
			try
			{
				PlayerSnapshot snapshot = dataCollector.collectSnapshot();
				if (snapshot != null)
				{
					boolean success = snapshotManager.saveSnapshot(snapshot);
					if (success)
					{
						log.info("Snapshot saved successfully");
						if (panel != null)
						{
							panel.addSystemMessage("Snapshot saved successfully!");
						}

						// Prune old snapshots
						if (client.getLocalPlayer() != null)
						{
							snapshotManager.pruneSnapshots(
								client.getLocalPlayer().getName(),
								config.maxSnapshots()
							);
						}
					}
					else
					{
						log.warn("Failed to save snapshot");
						if (panel != null)
						{
							panel.addSystemMessage("Failed to save snapshot.");
						}
					}
				}
				else
				{
					log.warn("Cannot save snapshot - player not logged in");
					if (panel != null)
					{
						panel.addSystemMessage("Cannot save snapshot - player not logged in.");
					}
				}
			}
			catch (Exception e)
			{
				log.error("Error saving snapshot", e);
			}
		});
	}

	/**
	 * Updates the AI provider based on current configuration.
	 */
	private void updateAiProvider()
	{
		try
		{
			String apiKey = config.apiKey();
			if (apiKey == null || apiKey.isEmpty())
			{
				currentProvider = null;
				return;
			}

			switch (config.aiProvider())
			{
				case OPENAI:
					String openaiModel = config.openaiModel().getModelName();
					currentProvider = new OpenAiProvider(apiKey, openaiModel);
					log.debug("Using OpenAI provider with model: {}", openaiModel);
					break;

				case ANTHROPIC:
					String anthropicModel = config.anthropicModel().getModelName();
					currentProvider = new AnthropicProvider(apiKey, anthropicModel);
					log.debug("Using Anthropic provider with model: {}", anthropicModel);
					break;

				default:
					log.warn("Unknown AI provider: {}", config.aiProvider());
					currentProvider = null;
					break;
			}
		}
		catch (Exception e)
		{
			log.error("Error updating AI provider", e);
			currentProvider = null;
		}
	}

	/**
	 * Starts the auto-snapshot task.
	 */
	private void startAutoSnapshot()
	{
		if (snapshotTask != null)
		{
			return; // Already running
		}

		int intervalMinutes = config.snapshotInterval();
		log.debug("Starting auto-snapshot with interval: {} minutes", intervalMinutes);

		snapshotTask = executor.scheduleAtFixedRate(
			this::saveSnapshot,
			intervalMinutes,
			intervalMinutes,
			TimeUnit.MINUTES
		);
	}

	/**
	 * Stops the auto-snapshot task.
	 */
	private void stopAutoSnapshot()
	{
		if (snapshotTask != null)
		{
			snapshotTask.cancel(false);
			snapshotTask = null;
			log.debug("Stopped auto-snapshot");
		}
	}

	@Provides
	AiAssistantConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AiAssistantConfig.class);
	}
}
