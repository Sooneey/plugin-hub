package net.runelite.client.plugins.aiassistant;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * UI panel for the AI Assistant chat interface.
 */
@Slf4j
public class AiAssistantPanel extends PluginPanel
{
	private final JTextArea chatHistory;
	private final JTextField messageInput;
	private final JButton sendButton;
	private final JButton snapshotButton;
	private final JButton clearHistoryButton;
	private final AiAssistantPlugin plugin;

	public AiAssistantPanel(AiAssistantPlugin plugin)
	{
		super(false);
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Title panel
		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new BorderLayout());
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel titleLabel = new JLabel("AI Assistant");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		titlePanel.add(titleLabel, BorderLayout.WEST);

		add(titlePanel, BorderLayout.NORTH);

		// Chat history area
		chatHistory = new JTextArea();
		chatHistory.setEditable(false);
		chatHistory.setLineWrap(true);
		chatHistory.setWrapStyleWord(true);
		chatHistory.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		chatHistory.setForeground(Color.WHITE);
		chatHistory.setFont(new Font("Arial", Font.PLAIN, 12));
		chatHistory.setBorder(new EmptyBorder(5, 5, 5, 5));

		JScrollPane scrollPane = new JScrollPane(chatHistory);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(scrollPane, BorderLayout.CENTER);

		// Input panel
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BorderLayout(5, 5));
		inputPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Button panel (for snapshot and clear history buttons)
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		snapshotButton = new JButton("Save Snapshot");
		snapshotButton.setToolTipText("Save current player state");
		snapshotButton.addActionListener(e -> plugin.saveSnapshot());
		buttonPanel.add(snapshotButton);

		clearHistoryButton = new JButton("Clear History");
		clearHistoryButton.setToolTipText("Clear conversation history");
		clearHistoryButton.addActionListener(e ->
		{
			plugin.clearConversationHistory();
			clearChat();
			addSystemMessage("Conversation history cleared!");
		});
		buttonPanel.add(clearHistoryButton);

		inputPanel.add(buttonPanel, BorderLayout.NORTH);

		// Message input area
		JPanel messagePanel = new JPanel();
		messagePanel.setLayout(new BorderLayout(5, 0));
		messagePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		messageInput = new JTextField();
		messageInput.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		messageInput.setForeground(Color.WHITE);
		messageInput.setCaretColor(Color.WHITE);
		messageInput.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(5, 5, 5, 5)
		));

		// Send message on Enter key
		messageInput.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					sendMessage();
				}
			}
		});

		sendButton = new JButton("Send");
		sendButton.addActionListener(e -> sendMessage());

		messagePanel.add(messageInput, BorderLayout.CENTER);
		messagePanel.add(sendButton, BorderLayout.EAST);

		inputPanel.add(messagePanel, BorderLayout.CENTER);
		add(inputPanel, BorderLayout.SOUTH);

		// Initial welcome message
		addSystemMessage("Welcome to AI Assistant!");
		addSystemMessage("Configure your API key in the plugin settings to get started.");
		addSystemMessage("Ask me anything about OSRS!");
	}

	/**
	 * Sends the current message to the AI.
	 */
	private void sendMessage()
	{
		String message = messageInput.getText().trim();
		if (message.isEmpty())
		{
			return;
		}

		// Clear input field
		messageInput.setText("");

		// Add user message to chat
		addUserMessage(message);

		// Disable input while processing
		setInputEnabled(false);

		// Send to plugin for processing
		plugin.processMessage(message, this::addAiMessage, this::addErrorMessage);
	}

	/**
	 * Adds a user message to the chat history.
	 */
	public void addUserMessage(String message)
	{
		appendMessage("You", message, ColorScheme.LIGHT_GRAY_COLOR);
	}

	/**
	 * Adds an AI message to the chat history.
	 */
	public void addAiMessage(String message)
	{
		appendMessage("AI", message, ColorScheme.PROGRESS_COMPLETE_COLOR);
		setInputEnabled(true);
	}

	/**
	 * Starts a streaming AI message.
	 */
	public void startStreamingAiMessage()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (chatHistory.getText().length() > 0)
			{
				chatHistory.append("\n\n");
			}
			chatHistory.append("[AI]: ");
		});
	}

	/**
	 * Appends a chunk to the current streaming message.
	 */
	public void appendStreamingChunk(String chunk)
	{
		SwingUtilities.invokeLater(() ->
		{
			chatHistory.append(chunk);
			chatHistory.setCaretPosition(chatHistory.getDocument().getLength());
		});
	}

	/**
	 * Finishes a streaming AI message.
	 */
	public void finishStreamingAiMessage()
	{
		setInputEnabled(true);
	}

	/**
	 * Adds an error message to the chat history.
	 */
	public void addErrorMessage(String message)
	{
		appendMessage("Error", message, ColorScheme.PROGRESS_ERROR_COLOR);
		setInputEnabled(true);
	}

	/**
	 * Adds a system message to the chat history.
	 */
	public void addSystemMessage(String message)
	{
		appendMessage("System", message, Color.GRAY);
	}

	/**
	 * Appends a message to the chat history.
	 */
	private void appendMessage(String sender, String message, Color senderColor)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (chatHistory.getText().length() > 0)
			{
				chatHistory.append("\n\n");
			}

			// This is a simplified version - in a real implementation you might want to use
			// a JTextPane with StyledDocument for better formatting
			chatHistory.append(String.format("[%s]: %s", sender, message));

			// Auto-scroll to bottom
			chatHistory.setCaretPosition(chatHistory.getDocument().getLength());
		});
	}

	/**
	 * Enables or disables the input controls.
	 */
	private void setInputEnabled(boolean enabled)
	{
		SwingUtilities.invokeLater(() ->
		{
			messageInput.setEnabled(enabled);
			sendButton.setEnabled(enabled);
			if (enabled)
			{
				messageInput.requestFocus();
			}
		});
	}

	/**
	 * Clears the chat history.
	 */
	public void clearChat()
	{
		SwingUtilities.invokeLater(() -> chatHistory.setText(""));
	}
}
