package com.example.khoga.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * GeminiApiService — Retrofit interface gọi Gemini REST API
 *
 * Endpoint: POST /v1beta/models/gemini-2.5-flash:generateContent?key={API_KEY}
 */
public interface GeminiApiService {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest body
    );
}
