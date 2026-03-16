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
 * Anthropic (Claude) API provider implementation.
 */
@Slf4j
public class AnthropicProvider implements AiProvider
{
	private static final String API_URL = "https://api.anthropic.com/v1/messages";
	private static final String API_VERSION = "2023-06-01";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private final String apiKey;
	private final String model;
	private final OkHttpClient httpClient;
	private final Gson gson;

	public AnthropicProvider(String apiKey, String model)
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
		JsonObject requestBody = buildRequestBody(message, context, history, customSystemPrompt, true);

		// Build the HTTP request
		RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
		Request request = new Request.Builder()
			.url(API_URL)
			.post(body)
			.addHeader("x-api-key", apiKey)
			.addHeader("anthropic-version", API_VERSION)
			.addHeader("content-type", "application/json")
			.build();

		// Execute the streaming request
		StringBuilder fullResponse = new StringBuilder();
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				String errorBody = response.body() != null ? response.body().string() : "Unknown error";
				log.error("Anthropic API error: {} - {}", response.code(), errorBody);
				throw new IOException("Anthropic API request failed: " + response.code());
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
						try
						{
							JsonObject chunk = gson.fromJson(data, JsonObject.class);
							String type = chunk.has("type") ? chunk.get("type").getAsString() : "";

							if ("content_block_delta".equals(type))
							{
								JsonObject delta = chunk.getAsJsonObject("delta");
								if (delta != null && delta.has("text"))
								{
									String text = delta.get("text").getAsString();
									fullResponse.append(text);
									if (onChunk != null)
									{
										onChunk.accept(text);
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
		JsonObject requestBody = buildRequestBody(message, context, history, customSystemPrompt, false);

		// Build the HTTP request
		RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
		Request request = new Request.Builder()
			.url(API_URL)
			.post(body)
			.addHeader("x-api-key", apiKey)
			.addHeader("anthropic-version", API_VERSION)
			.addHeader("content-type", "application/json")
			.build();

		// Execute the request
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				String errorBody = response.body() != null ? response.body().string() : "Unknown error";
				log.error("Anthropic API error: {} - {}", response.code(), errorBody);
				throw new IOException("Anthropic API request failed: " + response.code() + " - " + errorBody);
			}

			String responseBody = response.body().string();
			JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

			// Extract the response message
			JsonArray content = responseJson.getAsJsonArray("content");
			if (content.size() == 0)
			{
				throw new IOException("No content returned from Anthropic");
			}

			JsonObject firstContent = content.get(0).getAsJsonObject();
			String text = firstContent.get("text").getAsString();

			log.debug("Received response from Anthropic: {}", text);
			return text;
		}
	}

	@Override
	public String getProviderName()
	{
		return "Anthropic (" + model + ")";
	}

	@Override
	public boolean validateApiKey(String apiKey)
	{
		// Anthropic API keys start with "sk-ant-"
		return apiKey != null && apiKey.startsWith("sk-ant-") && apiKey.length() > 20;
	}

	/**
	 * Builds the request body for the API request.
	 */
	private JsonObject buildRequestBody(String message, String context, ConversationHistory history,
										String customSystemPrompt, boolean streaming)
	{
		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", model);
		requestBody.addProperty("max_tokens", 1024);

		if (streaming)
		{
			requestBody.addProperty("stream", true);
		}

		// Add system message with context
		if (context != null && !context.isEmpty())
		{
			String systemPrompt;
			if (customSystemPrompt != null && !customSystemPrompt.trim().isEmpty())
			{
				// Use custom system prompt
				systemPrompt = customSystemPrompt + "\n\nPlayer information:\n\n" + context;
			}
			else
			{
				// Use default system prompt
				systemPrompt = "You are a helpful OSRS (Old School RuneScape) assistant. " +
					"Use the following player information to provide personalized assistance:\n\n" + context;
			}
			requestBody.addProperty("system", systemPrompt);
		}

		requestBody.add("messages", buildMessagesArray(message, history));
		return requestBody;
	}

	/**
	 * Builds the messages array for the API request.
	 */
	private JsonArray buildMessagesArray(String message, ConversationHistory history)
	{
		JsonArray messages = new JsonArray();

		// Add conversation history
		if (history != null)
		{
			for (ConversationHistory.Message historyMessage : history.getMessages())
			{
				// Skip system messages in history (system is separate in Anthropic API)
				if (!"system".equals(historyMessage.getRole()))
				{
					JsonObject msg = new JsonObject();
					msg.addProperty("role", historyMessage.getRole());
					msg.addProperty("content", historyMessage.getContent());
					messages.add(msg);
				}
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
