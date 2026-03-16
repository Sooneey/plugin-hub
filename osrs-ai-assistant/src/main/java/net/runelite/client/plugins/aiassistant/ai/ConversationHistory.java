package net.runelite.client.plugins.aiassistant.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages conversation history for multi-turn chat.
 */
public class ConversationHistory
{
	private final List<Message> messages;
	private final int maxMessages;

	public ConversationHistory()
	{
		this(20); // Default to 20 messages max
	}

	public ConversationHistory(int maxMessages)
	{
		this.messages = new ArrayList<>();
		this.maxMessages = maxMessages;
	}

	/**
	 * Adds a user message to the history.
	 */
	public void addUserMessage(String content)
	{
		messages.add(new Message("user", content));
		trimIfNeeded();
	}

	/**
	 * Adds an assistant message to the history.
	 */
	public void addAssistantMessage(String content)
	{
		messages.add(new Message("assistant", content));
		trimIfNeeded();
	}

	/**
	 * Gets all messages.
	 */
	public List<Message> getMessages()
	{
		return new ArrayList<>(messages);
	}

	/**
	 * Clears all messages.
	 */
	public void clear()
	{
		messages.clear();
	}

	/**
	 * Trims the history if it exceeds max messages, keeping the most recent ones.
	 */
	private void trimIfNeeded()
	{
		while (messages.size() > maxMessages)
		{
			messages.remove(0);
		}
	}

	/**
	 * Represents a single message in the conversation.
	 */
	@Data
	public static class Message
	{
		private final String role; // "user", "assistant", or "system"
		private final String content;

		public Message(String role, String content)
		{
			this.role = role;
			this.content = content;
		}
	}
}
