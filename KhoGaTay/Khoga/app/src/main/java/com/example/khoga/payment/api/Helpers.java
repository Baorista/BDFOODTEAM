package com.example.khoga.payment.api;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Helpers {
    private static int transIdDefault = 1;

    public static String getAppTransId() {
        if (transIdDefault >= 100000) {
            transIdDefault = 1;
        }
        transIdDefault += 1;
        SimpleDateFormat format = new SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault());
        return format.format(new Date()) + "_" + transIdDefault;
    }

    public static String getAppTime() {
        return String.valueOf(System.currentTimeMillis());
    }
}
