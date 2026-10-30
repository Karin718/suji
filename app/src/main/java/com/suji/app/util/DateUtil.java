package com.suji.app.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtil {

    private DateUtil() {
    }

    public static long startOfDay(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long startOfNextDay(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(t));
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static long addDays(long dayStart, int days) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayStart);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTimeInMillis();
    }

    public static String hhmm(long t) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(t));
    }

    public static String mmdd(long t) {
        return new SimpleDateFormat("MM-dd", Locale.getDefault()).format(new Date(t));
    }

    public static String datetime(long t) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(t));
    }

    public static String weekdayShort(long dayStart) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayStart);
        int d = c.get(Calendar.DAY_OF_WEEK);
        String[] names = {"日", "一", "二", "三", "四", "五", "六"};
        return names[Math.max(0, Math.min(6, d - 1))];
    }

    /** 距离下次复习还有多久。 */
    public static String dueLabel(long dueAt, long now) {
        long diff = dueAt - now;
        if (diff <= 0) {
            return "现在";
        }
        long min = Math.max(1L, diff / 60000L);
        if (min < 60) {
            return min + " 分钟后";
        }
        long h = min / 60;
        if (h < 24) {
            return h + " 小时后";
        }
        long d = h / 24;
        if (d < 30) {
            return d + " 天后";
        }
        return (d / 30) + " 个月后";
    }
}
