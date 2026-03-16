package net.runelite.client.plugins.aiassistant.ai;

import java.util.function.Consumer;

/**
 * Interface for AI providers (OpenAI, Anthropic, etc.).
 */
public interface AiProvider
{
	/**
	 * Sends a message to the AI and receives a response.
	 *
	 * @param message The user's message
	 * @param context Additional context to include in the prompt
	 * @return The AI's response
	 * @throws Exception if the request fails
	 */
	String sendMessage(String message, String context) throws Exception;

	/**
	 * Sends a message with conversation history to the AI.
	 *
	 * @param message The user's message
	 * @param context Additional context to include in the prompt
	 * @param history Conversation history
	 * @return The AI's response
	 * @throws Exception if the request fails
	 */
	String sendMessageWithHistory(String message, String context, ConversationHistory history) throws Exception;

	/**
	 * Sends a message with streaming response.
	 *
	 * @param message The user's message
	 * @param context Additional context to include in the prompt
	 * @param history Conversation history
	 * @param customSystemPrompt Optional custom system prompt
	 * @param onChunk Callback for each chunk of the response
	 * @return The complete response
	 * @throws Exception if the request fails
	 */
	default String sendMessageWithStreaming(String message, String context, ConversationHistory history,
											String customSystemPrompt, Consumer<String> onChunk) throws Exception
	{
		// Default implementation: non-streaming
		throw new UnsupportedOperationException("Streaming not supported by this provider");
	}

	/**
	 * Checks if this provider supports streaming.
	 */
	default boolean supportsStreaming()
	{
		return false;
	}

	/**
	 * Gets the provider name.
	 *
	 * @return The provider name (e.g., "OpenAI", "Anthropic")
	 */
	String getProviderName();

	/**
	 * Validates the API key format.
	 *
	 * @param apiKey The API key to validate
	 * @return true if the API key appears valid, false otherwise
	 */
	boolean validateApiKey(String apiKey);
}
