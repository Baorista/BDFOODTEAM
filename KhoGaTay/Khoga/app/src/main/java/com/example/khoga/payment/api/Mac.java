package com.example.khoga.payment.api;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class Mac {
    public static String computeMac(String key, String data) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(2 * rawHmac.length);
        for (byte b : rawHmac) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }
}
