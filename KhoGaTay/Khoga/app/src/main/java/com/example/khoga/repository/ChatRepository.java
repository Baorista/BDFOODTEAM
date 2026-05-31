package com.example.khoga.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.example.khoga.api.GeminiApiService;
import com.example.khoga.api.GeminiRequest;
import com.example.khoga.api.GeminiResponse;
import com.example.khoga.api.RetrofitClient;
import com.example.khoga.model.BrowsingHistory;
import com.example.khoga.model.ChatMessage;
import com.example.khoga.model.Order;
import com.example.khoga.model.Product;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ChatRepository — Xử lý toàn bộ logic AI Chatbot
 *
 * Luồng hoạt động:
 * 1. Thu thập context (browsing history + orders + product catalog)
 * 2. Build system prompt + conversation history
 * 3. Gọi Gemini API qua Retrofit
 * 4. Parse response, tách SUGGESTIONS nếu có
 * 5. Lưu tin nhắn vào chatHistory/{userId}
 */
public class ChatRepository {

    private static final String TAG = "ChatRepository";

    // ===== THAY API KEY CỦA BẠN VÀO ĐÂY =====
    //AIzaSyBztIVuhUWPQykNwa2Jb6P3M50XgCTqgb0
    private static final String GEMINI_API_KEY = "AIzaSyBZGsLyby1OTVfRGHhyuUIcb5u6TZp3VAA";

    private static final int CHAT_HISTORY_LIMIT = 20;
    private static final int MAX_MESSAGES_KEPT = 50;

    // Regex để tách SUGGESTIONS:{...} từ response
    private static final Pattern SUGGESTIONS_PATTERN =
            Pattern.compile("SUGGESTIONS:\\{.*?\"productIds\"\\s*:\\s*\\[(.*?)\\].*?\\}");
    private static final Pattern ORDER_INTENT_PATTERN = Pattern.compile("ORDER_INTENT:\\s*(.*)");

    private final DatabaseReference chatRef;
    private final GeminiApiService apiService;

    public ChatRepository() {
        chatRef = FirebaseDatabase.getInstance().getReference("chatHistory");
        apiService = RetrofitClient.getApiService();
    }

    // ====================================================================
    // 1. GỬI TIN NHẮN + GỌI GEMINI API
    // ====================================================================

    /**
     * Gửi tin nhắn cho AI và nhận phản hồi
     *
     * @param userId          UID người dùng
     * @param userMessage     Tin nhắn user vừa gõ
     * @param recentHistory   Lịch sử chat gần đây (để duy trì ngữ cảnh)
     * @param browsingContext Sản phẩm user đã xem
     * @param orderContext    Đơn hàng gần đây
     * @param productCatalog  Danh sách sản phẩm trong shop (để AI biết tồn kho, giá)
     * @param responseLiveData LiveData nhận text phản hồi
     * @param loadingLiveData  LiveData trạng thái loading
     */
    public void sendMessage(String userId,
                            String userMessage,
                            List<ChatMessage> recentHistory,
                            List<BrowsingHistory> browsingContext,
                            List<Order> orderContext,
                            List<Product> productCatalog,
                            MutableLiveData<String> responseLiveData,
                            MutableLiveData<Boolean> loadingLiveData,
                            MutableLiveData<List<String>> suggestedIdsLiveData,
                            MutableLiveData<Map<String, Integer>> orderIntentLiveData) {

        loadingLiveData.postValue(true);

        // ========== Bước 1: Build system prompt ==========
        String systemPrompt = buildSystemPrompt(browsingContext, orderContext, productCatalog);

        // ========== Bước 2: Build request ==========
        GeminiRequest.Builder requestBuilder = GeminiRequest.builder()
                .systemPrompt(systemPrompt)
                .temperature(0.7)
                .maxOutputTokens(1000);

        // Thêm lịch sử chat (chuyển role "assistant" → "model" cho Gemini)
        if (recentHistory != null) {
            for (ChatMessage msg : recentHistory) {
                String geminiRole = msg.isUser() ? "user" : "model";
                requestBuilder.addMessage(geminiRole, msg.getContent());
            }
        }

        // Thêm tin nhắn hiện tại
        requestBuilder.addUserMessage(userMessage);

        GeminiRequest request = requestBuilder.build();

        // ========== Bước 3: Gọi API ==========
        apiService.generateContent(GEMINI_API_KEY, request)
                .enqueue(new Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GeminiResponse> call,
                                           @NonNull Response<GeminiResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().hasError()) {

                            String rawText = response.body().getResponseText();
                            if (rawText == null) rawText = "Xin lỗi, tôi không hiểu câu hỏi.";

                            // ========== Bước 4: Parse SUGGESTIONS & ORDER_INTENT ==========
                            List<String> suggestedIds = extractSuggestions(rawText);
                            Map<String, Integer> orderIntent = extractOrderIntent(rawText);
                            String cleanText = stripOrderIntent(stripSuggestions(rawText));

                            // ========== Bước 5: Lưu vào Firebase ==========
                            saveUserMessage(userId, userMessage);
                            saveAssistantMessage(userId, cleanText, suggestedIds);

                            responseLiveData.postValue(cleanText);
                            suggestedIdsLiveData.postValue(suggestedIds);
                            if (!orderIntent.isEmpty()) {
                                orderIntentLiveData.postValue(orderIntent);
                            }
                        } else {
                        String errMsg = "Xin lỗi, tôi không thể trả lời lúc này.";

                        // Đọc mã lỗi và nội dung lỗi thực sự từ errorBody
                        try {
                            String errorDetails = "Unknown error";
                            if (response.errorBody() != null) {
                                errorDetails = response.errorBody().string(); // Chú ý: .string() chỉ gọi được 1 lần
                            }
                            Log.e(TAG, "HTTP Error Code: " + response.code() + " - Chi tiết: " + errorDetails);
                        } catch (Exception e) {
                            Log.e(TAG, "Không thể đọc error body", e);
                        }

                        responseLiveData.postValue(errMsg);
                        saveUserMessage(userId, userMessage);
                    }
                        loadingLiveData.postValue(false);
                    }

                    @Override
                    public void onFailure(@NonNull Call<GeminiResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "API call failed", t);
                        responseLiveData.postValue(
                                "Xin lỗi, tôi không thể trả lời lúc này. Vui lòng thử lại.");
                        saveUserMessage(userId, userMessage);
                        loadingLiveData.postValue(false);
                    }
                });
    }

    // ====================================================================
    // 2. BUILD SYSTEM PROMPT + CONTEXT
    // ====================================================================

    // ====================================================================
    // 2. BUILD SYSTEM PROMPT + CONTEXT
    // ====================================================================

    private String buildSystemPrompt(List<BrowsingHistory> browsingContext,
                                     List<Order> orderContext,
                                     List<Product> productCatalog) {

        StringBuilder sb = new StringBuilder();

        // Vai trò chatbot
        sb.append("Bạn là trợ lý mua sắm AI của ứng dụng ShopApp. ");
        sb.append("Luôn trả lời bằng tiếng Việt, thân thiện và ngắn gọn. ");
        sb.append("Khi người dùng hỏi về sản phẩm, hãy đề xuất dựa trên lịch sử và danh sách sản phẩm. ");
        sb.append("Khi người dùng hỏi về tồn kho hoặc số lượng, hãy trả lời chính xác dựa trên field stock. ");
        sb.append("Khi đề xuất sản phẩm, hãy mô tả ngắn gọn tại sao sản phẩm phù hợp. ");

        // --- QUY TẮC ĐỊNH DẠNG ĐỂ TRÁNH LỖI CẮT CHỮ DO JSON ---
        sb.append("\n\nQUY TẮC ĐỊNH DẠNG BẮT BUỘC (VI PHẠM SẼ GÂY LỖI HỆ THỐNG):\n");
        sb.append("1. LUÔN hoàn thành trọn vẹn câu trả lời bằng văn bản tự nhiên trước. ");
        sb.append("KHÔNG BAO GIỜ ĐƯỢC chèn JSON, mã code, hoặc dòng SUGGESTIONS vào giữa câu văn. ");
        sb.append("Toàn bộ phần văn bản phải kết thúc bằng dấu chấm (.) trước khi có bất kỳ dữ liệu kỹ thuật nào.\n");
        sb.append("2. CHỈ KHI đã viết xong TOÀN BỘ câu trả lời văn bản (có dấu chấm kết thúc), ");
        sb.append("hãy xuống dòng 2 lần (\\n\\n) rồi in dòng cuối cùng ĐÚNG định dạng sau:\n");
        sb.append("SUGGESTIONS:{\"productIds\":[\"id1\",\"id2\"]}\n");
        sb.append("3. Tối đa 3 sản phẩm mỗi lần gợi ý. Dùng đúng productId từ danh sách sản phẩm.\n");
        sb.append("4. Nếu không có sản phẩm phù hợp, KHÔNG thêm dòng SUGGESTIONS.\n");
        sb.append("5. VÍ DỤ CÂU ĐÚNG NẾU KHÁCH KHÔNG MUA MÀ CHỈ HỎI:\n");
        sb.append("Dạ, em gợi ý cho anh/chị mấy sản phẩm phù hợp nè. Khô gà MixiFood rất ngon và đang được ưa chuộng.\n\n");
        sb.append("SUGGESTIONS:{\"productIds\":[\"prod001\",\"prod002\"]}\n");
        sb.append("6. VÍ DỤ SAI (KHÔNG ĐƯỢC LÀM):\n");
        sb.append("Dạ, em gợi ý SUGGESTIONS:{\"productIds\":[\"prod001\"]} cho anh/chị mấy sản phẩm...\n\n");
        sb.append("7. TẠO ĐƠN HÀNG (RẤT QUAN TRỌNG):\n");
        sb.append("CHỈ KHI NÀO khách ĐÃ XÁC NHẬN CHÍNH XÁC sản phẩm cụ thể muốn mua, bạn mới chốt đơn bằng cách in THÊM một dòng DƯỚI CÙNG theo cú pháp:\n");
        sb.append("ORDER_INTENT:productId_1:số_lượng_1,productId_2:số_lượng_2\n");
        sb.append("Ví dụ: Khách muốn mua 2 sản phẩm có id là -Nw012 và 1 sản phẩm có id là -Nwxyz:\n");
        sb.append("Dạ em đã chuẩn bị đơn hàng cho anh/chị rồi ạ. Anh/chị xem và thanh toán nhé!\n\n");
        sb.append("ORDER_INTENT:-Nw012:2,-Nwxyz:1\n");
        sb.append("LƯU Ý: Nếu khách gọi chung chung (ví dụ '2 khô gà') mà shop có nhiều loại khô gà, bạn PHẢI HỎI LẠI rõ khách lấy loại nào. TRONG LÚC HỎI LẠI, TUYỆT ĐỐI KHÔNG ĐƯỢC IN RA DÒNG ORDER_INTENT.\n");
        // ----------------------------------------------------------

        // Thời gian hiện tại
        sb.append("Ngày giờ hiện tại: ").append(getCurrentDateTimeVietnamese()).append("\n");
        if (productCatalog != null && !productCatalog.isEmpty()) {
            sb.append("\n--- DANH SÁCH SẢN PHẨM TRONG SHOP ---\n");
            for (Product p : productCatalog) {
                sb.append("- ID:").append(p.getProductId())
                        .append(", Tên:").append(p.getName())
                        .append(", Giá:").append(formatCurrency(p.getPrice()))
                        .append(", Tồn kho:").append(p.getStock())
                        .append(", Đánh giá:").append(p.getAvgRating()).append("★")
                        .append(", Đã bán:").append(p.getTotalSold())
                        .append("\n");
            }
        }

        // Context: Sản phẩm user quan tâm
        if (browsingContext != null && !browsingContext.isEmpty()) {
            sb.append("\n--- SẢN PHẨM NGƯỜI DÙNG QUAN TÂM GẦN ĐÂY ---\n");
            for (BrowsingHistory bh : browsingContext) {
                sb.append("- ").append(bh.getProductName())
                        .append(" (xem ").append(bh.getViewCount()).append(" lần)\n");
            }
        }

        // Context: Đơn hàng gần đây
        if (orderContext != null && !orderContext.isEmpty()) {
            sb.append("\n--- LỊCH SỬ ĐƠN HÀNG GẦN ĐÂY ---\n");
            int count = Math.min(orderContext.size(), 5);
            for (int i = 0; i < count; i++) {
                Order o = orderContext.get(i);
                sb.append("- Đơn ").append(o.getOrderId())
                        .append(": ").append(o.getOrderStatus())
                        .append(", ").append(formatCurrency(o.getTotalAmount()))
                        .append("\n");
            }
        }

        return sb.toString();
    }

    // ====================================================================
    // 3. PARSE SUGGESTIONS TỪ RESPONSE
    // ====================================================================

    /**
     * Tách danh sách productId từ dòng SUGGESTIONS:{...}
     * Ví dụ: SUGGESTIONS:{"productIds":["abc123","def456"]}
     * → ["abc123", "def456"]
     */
    private List<String> extractSuggestions(String text) {
        List<String> ids = new ArrayList<>();
        Matcher matcher = SUGGESTIONS_PATTERN.matcher(text);
        if (matcher.find()) {
            String idsStr = matcher.group(1); // "abc123","def456"
            if (idsStr != null) {
                // Parse từng ID (loại bỏ dấu nháy và khoảng trắng)
                String[] parts = idsStr.split(",");
                for (String part : parts) {
                    String id = part.trim().replace("\"", "").replace("'", "");
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Xóa dòng SUGGESTIONS:{...} khỏi text hiển thị cho user
     */
    private String stripSuggestions(String text) {
        return text.replaceAll("\\n?SUGGESTIONS:\\{.*?\\}", "").trim();
    }

    private Map<String, Integer> extractOrderIntent(String text) {
        Map<String, Integer> map = new HashMap<>();
        Matcher matcher = ORDER_INTENT_PATTERN.matcher(text);
        if (matcher.find()) {
            String intentStr = matcher.group(1).trim();
            String[] parts = intentStr.split(",");
            for (String part : parts) {
                int lastColon = part.lastIndexOf(":");
                if (lastColon > 0 && lastColon < part.length() - 1) {
                    try {
                        String id = part.substring(0, lastColon).trim();
                        int qty = Integer.parseInt(part.substring(lastColon + 1).trim());
                        map.put(id, qty);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return map;
    }

    private String stripOrderIntent(String text) {
        return text.replaceAll("\\n?ORDER_INTENT:.*", "").trim();
    }

    // ====================================================================
    // 4. LƯU TIN NHẮN VÀO FIREBASE
    // ====================================================================

    private void saveUserMessage(String userId, String content) {
        DatabaseReference ref = chatRef.child(userId).push();
        ChatMessage msg = new ChatMessage("user", content);
        msg.setMessageId(ref.getKey());
        ref.setValue(msg);
    }

    private void saveAssistantMessage(String userId, String content,
                                      List<String> suggestedProductIds) {
        DatabaseReference ref = chatRef.child(userId).push();
        ChatMessage msg = new ChatMessage("assistant", content);
        msg.setMessageId(ref.getKey());

        // Lưu metadata nếu có gợi ý sản phẩm
        if (suggestedProductIds != null && !suggestedProductIds.isEmpty()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("suggestedProductIds", suggestedProductIds);
            msg.setMetadata(metadata);
        }

        ref.setValue(msg);
    }

    // ====================================================================
    // 5. LẤY LỊCH SỬ CHAT
    // ====================================================================

    public void getChatHistory(String userId,
                               MutableLiveData<List<ChatMessage>> liveData) {
        chatRef.child(userId)
                .orderByChild("timestamp")
                .limitToLast(CHAT_HISTORY_LIMIT)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ChatMessage> messages = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ChatMessage msg = child.getValue(ChatMessage.class);
                            if (msg != null) {
                                messages.add(msg);
                            }
                        }
                        // Firebase limitToLast đã sort theo timestamp asc
                        liveData.postValue(messages);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Load chat history failed", error.toException());
                        liveData.postValue(new ArrayList<>());
                    }
                });
    }

    // ====================================================================
    // 6. DỌN TIN NHẮN CŨ (giữ tối đa 50 tin)
    // ====================================================================

    public void trimOldMessages(String userId) {
        chatRef.child(userId)
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (count <= MAX_MESSAGES_KEPT) return;

                        // Xóa (count - 50) tin cũ nhất
                        long toDelete = count - MAX_MESSAGES_KEPT;
                        long deleted = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (deleted >= toDelete) break;
                            child.getRef().removeValue();
                            deleted++;
                        }
                        Log.d(TAG, "Trimmed " + deleted + " old messages");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Trim failed", error.toException());
                    }
                });
    }

    // ====================================================================
    // 7. XÓA LỊCH SỬ CHAT (tạo cuộc trò chuyện mới)
    // ====================================================================

    public void clearHistory(String userId) {
        chatRef.child(userId).removeValue();
    }

    // ====================================================================
    // HELPER METHODS
    // ====================================================================

    private String getCurrentDateTimeVietnamese() {
        String[] dayNames = {"Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư",
                "Thứ năm", "Thứ sáu", "Thứ bảy"};
        Calendar cal = Calendar.getInstance();
        String dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1];
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault());
        return dayName + ", " + sdf.format(new Date());
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.US, "%,.0fđ", amount);
    }
}
