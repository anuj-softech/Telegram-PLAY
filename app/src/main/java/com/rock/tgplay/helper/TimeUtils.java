package com.rock.tgplay.helper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static String formatMessageTime(long unixSeconds) {
        if (unixSeconds == 0) return "Scheduled";
        Date messageDate = new Date(unixSeconds * 1000);
        Calendar messageCal = Calendar.getInstance();
        messageCal.setTime(messageDate);
        Calendar now = Calendar.getInstance();
        if (isSameDay(now, messageCal)) {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(messageDate);
        }
        Calendar yesterday = (Calendar) now.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(yesterday, messageCal)) {
            return "Yesterday";
        }
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(messageDate);
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}

