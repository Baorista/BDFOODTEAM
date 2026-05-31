
package com.example.khoga.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * RetrofitClient — Singleton Retrofit instance cho Gemini API
 */
public class RetrofitClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";

    private static Retrofit retrofit = null;
    private static GeminiApiService apiService = null;

    private RetrofitClient() {} // Ngăn tạo instance

    public static synchronized Retrofit getRetrofit() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)   // Gemini có thể mất thời gian generate
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static synchronized GeminiApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofit().create(GeminiApiService.class);
        }
        return apiService;
    }
}
