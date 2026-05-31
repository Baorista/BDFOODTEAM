package com.example.khoga.payment.api;

import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpProvider {
    public static JSONObject sendPost(String URL, RequestBody formBody) {
        JSONObject data = new JSONObject();
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(URL)
                    .post(formBody)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                data.put("return_code", 0);
                data.put("return_message", "Lỗi Server: " + response.code());
            } else {
                data = new JSONObject(response.body().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}
