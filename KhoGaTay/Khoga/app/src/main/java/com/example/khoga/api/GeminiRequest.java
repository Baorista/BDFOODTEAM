package com.example.khoga.api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GeminiRequest — Body gửi lên Gemini REST API
 *
 * POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
 *
 * JSON structure:
 * {
 *   "systemInstruction": { "parts": [{ "text": "..." }] },
 *   "contents": [
 *     { "role": "user", "parts": [{ "text": "..." }] },
 *     { "role": "model", "parts": [{ "text": "..." }] }
 *   ],
 *   "generationConfig": { "temperature": 0.7, "maxOutputTokens": 1000 }
 * }
 */
public class GeminiRequest {

    @SerializedName("systemInstruction")
    private Content systemInstruction;

    @SerializedName("contents")
    private List<Content> contents;

    @SerializedName("generationConfig")
    private GenerationConfig generationConfig;

    // ===== Builder pattern cho dễ dùng =====

    private GeminiRequest() {}

    public static Builder builder() {
        return new Builder();
    }

    // ===== Getters =====

    public Content getSystemInstruction() { return systemInstruction; }
    public List<Content> getContents() { return contents; }
    public GenerationConfig getGenerationConfig() { return generationConfig; }

    // ===== Inner classes =====

    public static class Content {
        @SerializedName("role")
        private String role;

        @SerializedName("parts")
        private List<Part> parts;

        public Content(String role, String text) {
            this.role = role;
            this.parts = Collections.singletonList(new Part(text));
        }

        // Constructor cho systemInstruction (không có role)
        public Content(String text) {
            this.parts = Collections.singletonList(new Part(text));
        }

        public String getRole() { return role; }
        public List<Part> getParts() { return parts; }
    }

    public static class Part {
        @SerializedName("text")
        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
    }

    public static class GenerationConfig {
        @SerializedName("temperature")
        private double temperature;

        @SerializedName("maxOutputTokens")
        private int maxOutputTokens;

        public GenerationConfig(double temperature, int maxOutputTokens) {
            this.temperature = temperature;
            this.maxOutputTokens = maxOutputTokens;
        }
    }

    // ===== Builder =====

    public static class Builder {
        private String systemPrompt;
        private final List<Content> contents = new ArrayList<>();
        private double temperature = 0.7;
        private int maxOutputTokens = 1000;

        public Builder systemPrompt(String prompt) {
            this.systemPrompt = prompt;
            return this;
        }

        /**
         * Thêm tin nhắn vào conversation
         * @param role "user" hoặc "model" (Gemini dùng "model", không phải "assistant")
         * @param text nội dung tin nhắn
         */
        public Builder addMessage(String role, String text) {
            contents.add(new Content(role, text));
            return this;
        }

        public Builder addUserMessage(String text) {
            return addMessage("user", text);
        }

        public Builder addModelMessage(String text) {
            return addMessage("model", text);
        }

        public Builder temperature(double temp) {
            this.temperature = temp;
            return this;
        }

        public Builder maxOutputTokens(int tokens) {
            this.maxOutputTokens = tokens;
            return this;
        }

        public GeminiRequest build() {
            GeminiRequest request = new GeminiRequest();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                request.systemInstruction = new Content(systemPrompt);
            }

            request.contents = new ArrayList<>(contents);
            request.generationConfig = new GenerationConfig(temperature, maxOutputTokens);

            return request;
        }
    }
}
