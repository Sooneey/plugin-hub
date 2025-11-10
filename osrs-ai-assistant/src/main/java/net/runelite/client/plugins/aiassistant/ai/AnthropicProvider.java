package net.runelite.client.plugins.aiassistant.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
		// Build the request body
		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", model);
		requestBody.addProperty("max_tokens", 1024);

		// Add system message with context
		if (context != null && !context.isEmpty())
		{
			String systemPrompt = "You are a helpful OSRS (Old School RuneScape) assistant. " +
				"Use the following player information to provide personalized assistance:\n\n" + context;
			requestBody.addProperty("system", systemPrompt);
		}

		// Add user message
		JsonArray messages = new JsonArray();
		JsonObject userMessage = new JsonObject();
		userMessage.addProperty("role", "user");
		userMessage.addProperty("content", message);
		messages.add(userMessage);

		requestBody.add("messages", messages);

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
}
