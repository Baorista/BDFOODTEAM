package com.example.khoga.payment.api;

import org.json.JSONObject;
import okhttp3.FormBody;
import okhttp3.RequestBody;

public class CreateOrder {
    private class CreateOrderData {
        String AppId;
        String AppUser;
        String AppTime;
        String Amount;
        String AppTransId;
        String EmbedData;
        String Items;
        String BankCode;
        String Description;
        String Mac;

        private CreateOrderData(String amount) throws Exception {
            AppId = String.valueOf(AppInfo.APP_ID);
            AppUser = "Android_Demo";
            AppTime = Helpers.getAppTime();
            Amount = amount;
            AppTransId = Helpers.getAppTransId();
            EmbedData = "{}";
            Items = "[]";
            BankCode = "zalopayapp"; // Chỉ định gọi qua App ZaloPay
            Description = "Thanh toan don hang #" + AppTransId;

            // Công thức mã hóa bắt buộc của ZaloPay
            String inputHMac = String.format("%s|%s|%s|%s|%s|%s|%s",
                    AppId, AppTransId, AppUser, Amount, AppTime, EmbedData, Items);
            Mac = com.example.khoga.payment.api.Mac.computeMac(AppInfo.MAC_KEY, inputHMac);
        }
    }

    public JSONObject createOrder(String amount) throws Exception {
        CreateOrderData input = new CreateOrderData(amount);

        // Đóng gói dữ liệu lại
        RequestBody formBody = new FormBody.Builder()
                .add("app_id", input.AppId)
                .add("app_user", input.AppUser)
                .add("app_time", input.AppTime)
                .add("amount", input.Amount)
                .add("app_trans_id", input.AppTransId)
                .add("embed_data", input.EmbedData)
                .add("item", input.Items)
                .add("bank_code", input.BankCode)
                .add("description", input.Description)
                .add("mac", input.Mac)
                .build();

        // Gửi lên ZaloPay bằng HttpProvider
        return HttpProvider.sendPost(AppInfo.URL_CREATE_ORDER, formBody);
    }
}
