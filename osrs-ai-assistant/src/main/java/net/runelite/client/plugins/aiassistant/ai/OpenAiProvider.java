package net.runelite.client.plugins.aiassistant.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
		// Build the request body
		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", model);

		JsonArray messages = new JsonArray();

		// Add system message with context
		if (context != null && !context.isEmpty())
		{
			JsonObject systemMessage = new JsonObject();
			systemMessage.addProperty("role", "system");
			systemMessage.addProperty("content", "You are a helpful OSRS (Old School RuneScape) assistant. " +
				"Use the following player information to provide personalized assistance:\n\n" + context);
			messages.add(systemMessage);
		}

		// Add user message
		JsonObject userMessage = new JsonObject();
		userMessage.addProperty("role", "user");
		userMessage.addProperty("content", message);
		messages.add(userMessage);

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
}
