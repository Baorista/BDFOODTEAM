package com.example.khoga.payment;

import android.net.Uri;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VnPayHelper {
    // Thông tin Sandbox VnPay
    public static final String VNP_TMN_CODE = "98J5PY76";
    public static final String VNP_HASH_SECRET = "JFTNK0GK6XE0V7RO63GJLQB16W8U27GL";
    public static final String VNP_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    // Deep Link callback — VnPay sẽ redirect về URL này sau khi thanh toán
    public static final String VNP_RETURN_URL = "myapp://vnpay/result";

    // ══════════════════════════════════════════════════════════════════════
    // HMAC SHA-512
    // ══════════════════════════════════════════════════════════════════════
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TẠO URL THANH TOÁN
    // FIX: amount dùng long thay vì int (tránh tràn số khi > 2.1 tỷ VND)
    // FIX: Timezone dùng Asia/Ho_Chi_Minh (UTC+7) thay vì Etc/GMT+7 (= UTC-7)
    // FIX: Thêm vnp_SecureHashType = SHA512
    // ══════════════════════════════════════════════════════════════════════
    public static String generatePaymentUrl(long amount, String orderId) throws Exception {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", VNP_TMN_CODE);

        // VnPay yêu cầu số tiền nhân 100 (đơn vị nhỏ nhất)
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNP_RETURN_URL);
        vnp_Params.put("vnp_IpAddr", "192.168.1.1");

        // FIX: Dùng Asia/Ho_Chi_Minh = UTC+7 (Việt Nam)
        // Etc/GMT+7 theo chuẩn POSIX ngược dấu, thực chất là UTC-7 (Mỹ)
        TimeZone vnTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(vnTimeZone);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        formatter.setTimeZone(vnTimeZone); // Quan trọng: set timezone cho formatter luôn

        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build chuỗi hash và query string (sort theo key)
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());

        // FIX: Thêm vnp_SecureHashType trước vnp_SecureHash
        queryUrl += "&vnp_SecureHashType=SHA512";
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return VNP_URL + "?" + queryUrl;
    }

    // ══════════════════════════════════════════════════════════════════════
    // XÁC THỰC CALLBACK TỪ VNPAY (Deep Link)
    // FIX MỚI: Phải verify hash để đảm bảo response không bị giả mạo
    // Trả về true nếu hash hợp lệ, false nếu bị tamper
    // ══════════════════════════════════════════════════════════════════════
    public static boolean verifyReturnUrl(Uri returnUri) {
        if (returnUri == null) return false;

        try {
            // Lấy vnp_SecureHash từ URL callback
            String receivedHash = returnUri.getQueryParameter("vnp_SecureHash");
            if (receivedHash == null || receivedHash.isEmpty()) return false;

            // Thu thập tất cả param trừ vnp_SecureHash và vnp_SecureHashType
            Map<String, String> params = new HashMap<>();
            for (String paramName : returnUri.getQueryParameterNames()) {
                if (!"vnp_SecureHash".equals(paramName)
                        && !"vnp_SecureHashType".equals(paramName)) {
                    String value = returnUri.getQueryParameter(paramName);
                    if (value != null && !value.isEmpty()) {
                        params.put(paramName, value);
                    }
                }
            }

            // Sort theo key và build hashData giống lúc tạo URL
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue,
                            StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            // Tính hash và so sánh (case-insensitive)
            String computedHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
            return computedHash.equalsIgnoreCase(receivedHash);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}