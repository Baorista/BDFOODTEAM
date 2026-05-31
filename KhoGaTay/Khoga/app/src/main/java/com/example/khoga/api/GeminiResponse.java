package com.example.khoga.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GeminiResponse — Parse response từ Gemini API
 *
 * JSON structure:
 * {
 *   "candidates": [{
 *     "content": {
 *       "parts": [{ "text": "Nội dung trả lời..." }],
 *       "role": "model"
 *     },
 *     "finishReason": "STOP"
 *   }]
 * }
 */
public class GeminiResponse {

    @SerializedName("candidates")
    private List<Candidate> candidates;

    @SerializedName("error")
    private ApiError error;

    /**
     * Lấy text trả lời từ response
     * @return nội dung text hoặc null nếu lỗi
     */
    public String getResponseText() {
        try {
            return candidates.get(0).getContent().getParts().get(0).getText();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasError() {
        return error != null || candidates == null || candidates.isEmpty();
    }

    public String getErrorMessage() {
        if (error != null) return error.getMessage();
        if (candidates == null || candidates.isEmpty()) return "Không có phản hồi từ AI";
        return null;
    }

    // ===== Getters =====

    public List<Candidate> getCandidates() { return candidates; }
    public ApiError getError() { return error; }

    // ===== Inner classes =====

    public static class Candidate {
        @SerializedName("content")
        private Content content;

        @SerializedName("finishReason")
        private String finishReason;

        public Content getContent() { return content; }
        public String getFinishReason() { return finishReason; }
    }

    public static class Content {
        @SerializedName("parts")
        private List<Part> parts;

        @SerializedName("role")
        private String role;

        public List<Part> getParts() { return parts; }
        public String getRole() { return role; }
    }

    public static class Part {
        @SerializedName("text")
        private String text;

        public String getText() { return text; }
    }

    public static class ApiError {
        @SerializedName("message")
        private String message;

        @SerializedName("code")
        private int code;

        public String getMessage() { return message; }
        public int getCode() { return code; }
    }
}
