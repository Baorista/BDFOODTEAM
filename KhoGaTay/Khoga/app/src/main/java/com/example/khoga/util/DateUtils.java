package com.example.khoga.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    private static final Locale VI = new Locale("vi", "VN");
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", VI);

    private DateUtils() {}

    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "";
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static String formatCurrency(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(VI);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount) + "đ";
    }

    public static String getCurrentDateTimeVietnamese() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy, HH:mm", VI);
        return sdf.format(new Date());
    }

    public static String getRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 1) return days + " ngày trước";
        if (days == 1) return "Hôm qua";
        if (hours > 0) return hours + " giờ trước";
        if (minutes > 0) return minutes + " phút trước";
        return "Vừa xong";
    }

    public static String formatDateOnly(long timestamp) {
        if (timestamp <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", VI);
        return sdf.format(new Date(timestamp));
    }
}
