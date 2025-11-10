package net.runelite.client.plugins.aiassistant.ai;

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
