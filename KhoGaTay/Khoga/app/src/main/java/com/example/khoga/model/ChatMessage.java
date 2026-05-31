package com.example.khoga.model;

import java.util.List;
import java.util.Map;

/**
 * ChatMessage — Model cho node chatHistory/{userId}/{messageId}
 *

 */
public class ChatMessage {

    private String messageId;
    private String role;       // "user" hoặc "assistant"
    private String content;
    private long timestamp;
    private Map<String, Object> metadata;

    // Constructor rỗng cho Firebase
    public ChatMessage() {}

    // Constructor nhanh tạo tin nhắn mới
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // ===== Getters & Setters =====

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    // ===== Helper methods =====

    public boolean isUser() {
        return "user".equals(role);
    }

    public boolean isAssistant() {
        return "assistant".equals(role);
    }

    /**
     * Lấy danh sách productId được gợi ý (nếu có trong metadata)
     */
    @SuppressWarnings("unchecked")
    public List<String> getSuggestedProductIds() {
        if (metadata == null) return null;
        Object ids = metadata.get("suggestedProductIds");
        if (ids instanceof List) {
            return (List<String>) ids;
        }
        return null;
    }

    public boolean hasSuggestions() {
        List<String> ids = getSuggestedProductIds();
        return ids != null && !ids.isEmpty();
    }
}
