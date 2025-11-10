package net.runelite.client.plugins.aiassistant;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("aiassistant")
public interface AiAssistantConfig extends Config
{
	@ConfigSection(
		name = "AI Provider",
		description = "AI provider settings",
		position = 0
	)
	String aiProviderSection = "aiProvider";

	@ConfigSection(
		name = "Snapshots",
		description = "Snapshot settings",
		position = 1
	)
	String snapshotsSection = "snapshots";

	// AI Provider Settings
	@ConfigItem(
		keyName = "aiProvider",
		name = "AI Provider",
		description = "Choose which AI provider to use",
		position = 0,
		section = aiProviderSection
	)
	default AiProviderType aiProvider()
	{
		return AiProviderType.OPENAI;
	}

	@ConfigItem(
		keyName = "apiKey",
		name = "API Key",
		description = "Your API key for the selected provider",
		position = 1,
		section = aiProviderSection,
		secret = true
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "openaiModel",
		name = "OpenAI Model",
		description = "Which OpenAI model to use",
		position = 2,
		section = aiProviderSection
	)
	default OpenAiModel openaiModel()
	{
		return OpenAiModel.GPT_4O_MINI;
	}

	@ConfigItem(
		keyName = "anthropicModel",
		name = "Anthropic Model",
		description = "Which Anthropic model to use",
		position = 3,
		section = aiProviderSection
	)
	default AnthropicModel anthropicModel()
	{
		return AnthropicModel.CLAUDE_SONNET;
	}

	// Snapshot Settings
	@ConfigItem(
		keyName = "autoSnapshot",
		name = "Auto-save Snapshots",
		description = "Automatically save player snapshots periodically",
		position = 0,
		section = snapshotsSection
	)
	default boolean autoSnapshot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "snapshotInterval",
		name = "Snapshot Interval (minutes)",
		description = "How often to auto-save snapshots (in minutes)",
		position = 1,
		section = snapshotsSection
	)
	default int snapshotInterval()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "maxSnapshots",
		name = "Max Snapshots to Keep",
		description = "Maximum number of snapshots to keep per player",
		position = 2,
		section = snapshotsSection
	)
	default int maxSnapshots()
	{
		return 20;
	}

	enum AiProviderType
	{
		OPENAI("OpenAI"),
		ANTHROPIC("Anthropic (Claude)");

		private final String displayName;

		AiProviderType(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	enum OpenAiModel
	{
		GPT_4O_MINI("gpt-4o-mini"),
		GPT_4O("gpt-4o");

		private final String modelName;

		OpenAiModel(String modelName)
		{
			this.modelName = modelName;
		}

		public String getModelName()
		{
			return modelName;
		}

		@Override
		public String toString()
		{
			return modelName;
		}
	}

	enum AnthropicModel
	{
		CLAUDE_SONNET("claude-sonnet-4-20250514"),
		CLAUDE_HAIKU("claude-3-5-haiku-20241022");

		private final String modelName;

		AnthropicModel(String modelName)
		{
			this.modelName = modelName;
		}

		public String getModelName()
		{
			return modelName;
		}

		@Override
		public String toString()
		{
			return modelName;
		}
	}
}
