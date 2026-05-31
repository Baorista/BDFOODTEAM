package com.example.khoga.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.khoga.model.BrowsingHistory;
import com.example.khoga.model.ChatMessage;
import com.example.khoga.model.Order;
import com.example.khoga.model.Product;
import com.example.khoga.repository.ChatRepository;

import java.util.List;
import java.util.Map;

/**
 * ChatViewModel — Kết nối ChatRepository với ChatFragment qua LiveData
 */
public class ChatViewModel extends AndroidViewModel {

    private final ChatRepository chatRepository;

    // LiveData cho Fragment observe
    private final MutableLiveData<List<ChatMessage>> chatHistory = new MutableLiveData<>();
    private final MutableLiveData<String> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<String>> suggestedProductIds = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> orderIntentLiveData = new MutableLiveData<>();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        chatRepository = new ChatRepository();
    }

    // ===== Getters cho LiveData =====

    public MutableLiveData<List<ChatMessage>> getChatHistory() {
        return chatHistory;
    }

    public MutableLiveData<String> getAiResponse() {
        return aiResponse;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<List<String>> getSuggestedProductIds() {
        return suggestedProductIds;
    }

    public MutableLiveData<Map<String, Integer>> getOrderIntentLiveData() {
        return orderIntentLiveData;
    }

    // ===== Actions =====

    public void loadHistory(String userId) {
        chatRepository.getChatHistory(userId, chatHistory);
    }

    public void sendMessage(String userId,
                            String text,
                            List<ChatMessage> recentHistory,
                            List<BrowsingHistory> browsingCtx,
                            List<Order> orderCtx,
                            List<Product> productCatalog) {
        chatRepository.sendMessage(
                userId, text,
                recentHistory, browsingCtx, orderCtx, productCatalog,
                aiResponse, isLoading, suggestedProductIds, orderIntentLiveData
        );
    }

    public void trimOldMessages(String userId) {
        chatRepository.trimOldMessages(userId);
    }

    public void clearHistory(String userId) {
        chatRepository.clearHistory(userId);
        aiResponse.setValue(null);
        suggestedProductIds.setValue(null);
    }
}
