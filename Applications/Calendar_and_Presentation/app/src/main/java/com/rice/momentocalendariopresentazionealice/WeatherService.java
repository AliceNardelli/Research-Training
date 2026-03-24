package com.rice.momentocalendariopresentazionealice;

import android.util.Log;

import org.json.JSONException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class WeatherService {
    private static final String API_KEY = "e35dbf5c4c04dad1382980f2299c8c7b"; // Replace with your actual API key
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";

    public static void getWeather(String country, final WeatherCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "weather?q=" + country + "&appid=" + API_KEY + "&units=metric";

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    try {
                        callback.onSuccess(jsonData);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    callback.onFailure("Request failed with code: " + response.code());
                }
            }
        });
    }

    public interface WeatherCallback {
        void onSuccess(String jsonResponse) throws JSONException;
        void onFailure(String errorMessage);
    }
}
