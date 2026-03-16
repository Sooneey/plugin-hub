package net.runelite.client.plugins.aiassistant.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI API provider implementation.
 */
@Slf4j
public class OpenAiProvider implements AiProvider
{
	private static final String API_URL = "https://api.openai.com/v1/chat/completions";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private final String apiKey;
	private final String model;
	private final OkHttpClient httpClient;
	private final Gson gson;

	public OpenAiProvider(String apiKey, String model)
	{
		this.apiKey = apiKey;
		this.model = model;
		this.gson = new Gson();
		this.httpClient = new OkHttpClient.Builder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.build();
	}

	@Override
	public String sendMessage(String message, String context) throws Exception
	{
		// Delegate to the new method with empty history
		return sendMessageWithHistory(message, context, new ConversationHistory(0));
	}

	@Override
	public String sendMessageWithHistory(String message, String context, ConversationHistory history) throws Exception
	{
		return sendMessageWithHistory(message, context, history, null);
	}

	/**
	 * Sends a message with conversation history and optional custom system prompt.
	 */
	@Override
	public boolean supportsStreaming()
	{
		return true;
	}

	@Override
	public String sendMessageWithStreaming(String message, String context, ConversationHistory history,
										   String customSystemPrompt, Consumer<String> onChunk) throws Exception
	{
		// Build the request body
		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", model);
		requestBody.addProperty("stream", true); // Enable streaming

		JsonArray messages = buildMessagesArray(message, context, history, customSystemPrompt);
		requestBody.add("messages", messages);
		requestBody.addProperty("max_tokens", 500);
		requestBody.addProperty("temperature", 0.7);

		// Build the HTTP request
		RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
		Request request = new Request.Builder()
			.url(API_URL)
			.post(body)
			.addHeader("Authorization", "Bearer " + apiKey)
			.addHeader("Content-Type", "application/json")
			.build();

		// Execute the streaming request
		StringBuilder fullResponse = new StringBuilder();
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				String errorBody = response.body() != null ? response.body().string() : "Unknown error";
				log.error("OpenAI API error: {} - {}", response.code(), errorBody);
				throw new IOException("OpenAI API request failed: " + response.code());
			}

			// Read streaming response
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream())))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					if (line.startsWith("data: "))
					{
						String data = line.substring(6);
						if ("[DONE]".equals(data))
						{
							break;
						}

						try
						{
							JsonObject chunk = gson.fromJson(data, JsonObject.class);
							JsonArray choices = chunk.getAsJsonArray("choices");
							if (choices != null && choices.size() > 0)
							{
								JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
								if (delta != null && delta.has("content"))
								{
									String content = delta.get("content").getAsString();
									fullResponse.append(content);
									if (onChunk != null)
									{
										onChunk.accept(content);
									}
								}
							}
						}
						catch (Exception e)
						{
							log.warn("Failed to parse streaming chunk: {}", data, e);
						}
					}
				}
			}
		}

		return fullResponse.toString();
	}

	public String sendMessageWithHistory(String message, String context, ConversationHistory history, String customSystemPrompt) throws Exception
	{
		// Build the request body
		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", model);

		JsonArray messages = buildMessagesArray(message, context, history, customSystemPrompt);

		requestBody.add("messages", messages);
		requestBody.addProperty("max_tokens", 500);
		requestBody.addProperty("temperature", 0.7);

		// Build the HTTP request
		RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
		Request request = new Request.Builder()
			.url(API_URL)
			.post(body)
			.addHeader("Authorization", "Bearer " + apiKey)
			.addHeader("Content-Type", "application/json")
			.build();

		// Execute the request
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				String errorBody = response.body() != null ? response.body().string() : "Unknown error";
				log.error("OpenAI API error: {} - {}", response.code(), errorBody);
				throw new IOException("OpenAI API request failed: " + response.code() + " - " + errorBody);
			}

			String responseBody = response.body().string();
			JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

			// Extract the response message
			JsonArray choices = responseJson.getAsJsonArray("choices");
			if (choices.size() == 0)
			{
				throw new IOException("No response choices returned from OpenAI");
			}

			JsonObject firstChoice = choices.get(0).getAsJsonObject();
			JsonObject responseMessage = firstChoice.getAsJsonObject("message");
			String content = responseMessage.get("content").getAsString();

			log.debug("Received response from OpenAI: {}", content);
			return content;
		}
	}

	@Override
	public String getProviderName()
	{
		return "OpenAI (" + model + ")";
	}

	@Override
	public boolean validateApiKey(String apiKey)
	{
		// OpenAI API keys start with "sk-"
		return apiKey != null && apiKey.startsWith("sk-") && apiKey.length() > 20;
	}

	/**
	 * Builds the messages array for the API request.
	 */
	private JsonArray buildMessagesArray(String message, String context, ConversationHistory history, String customSystemPrompt)
	{
		JsonArray messages = new JsonArray();

		// Add system message with context
		if (context != null && !context.isEmpty())
		{
			JsonObject systemMessage = new JsonObject();
			systemMessage.addProperty("role", "system");

			String systemContent;
			if (customSystemPrompt != null && !customSystemPrompt.trim().isEmpty())
			{
				// Use custom system prompt
				systemContent = customSystemPrompt + "\n\nPlayer information:\n\n" + context;
			}
			else
			{
				// Use default system prompt
				systemContent = "You are a helpful OSRS (Old School RuneScape) assistant. " +
					"Use the following player information to provide personalized assistance:\n\n" + context;
			}

			systemMessage.addProperty("content", systemContent);
			messages.add(systemMessage);
		}

		// Add conversation history
		if (history != null)
		{
			for (ConversationHistory.Message historyMessage : history.getMessages())
			{
				JsonObject msg = new JsonObject();
				msg.addProperty("role", historyMessage.getRole());
				msg.addProperty("content", historyMessage.getContent());
				messages.add(msg);
			}
		}

		// Add current user message
		JsonObject userMessage = new JsonObject();
		userMessage.addProperty("role", "user");
		userMessage.addProperty("content", message);
		messages.add(userMessage);

		return messages;
	}
}
