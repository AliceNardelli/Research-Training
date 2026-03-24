package com.rice.momentocostruzionisolari;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class OpenAIClient {
    private final String API_KEY;
    private final String ORGANIZATION_ID;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;


    public OpenAIClient(String apiKey, String organizationId) {
        this.API_KEY = apiKey;
        this.ORGANIZATION_ID = organizationId;
        client = new OkHttpClient();
    }

    public void queryGPT(String userInput, String systemMessage, JSONArray messages_history, Callback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("model", "gpt-4o"); // or "gpt-3.5-turbo"

            JSONArray messages = new JSONArray();

            // Add system message as the first message
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemMessage);
            messages.put(systemMsg);

            // Append the message history
            for (int i = 0; i < messages_history.length(); i++) {
                messages.put(messages_history.getJSONObject(i));
            }

            // Add new user message
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userInput);
            messages.put(userMsg);

            json.put("messages", messages);
            json.put("max_tokens", 150);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("OpenAI-Organization", ORGANIZATION_ID)
                    .build();

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public void queryGPTMeteo(String systemMessage, Callback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("model", "gpt-4o"); // or "gpt-3.5-turbo"

            JSONArray messages = new JSONArray();

            // Add system message as the first message
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemMessage);
            messages.put(systemMsg);

            json.put("messages", messages);
            json.put("max_tokens", 150);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("OpenAI-Organization", ORGANIZATION_ID)
                    .build();

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

