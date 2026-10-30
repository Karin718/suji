package com.suji.app.data;

import android.content.Context;
import android.content.SharedPreferences;

/** 所有本地设置。 */
public class Prefs {

    public static final String STD_INTERVALS = "5,30,720,1440,2880,5760,10080,21600,43200,86400,172800";
    public static final String EASY_INTERVALS = "10,60,1440,2880,7200,20160,43200,86400,172800";
    public static final String HARD_INTERVALS = "1,5,30,120,720,1440,2880,5760,10080,21600";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences("suji_prefs", Context.MODE_PRIVATE);
    }

    public int newPerDay() {
        return sp.getInt("newPerDay", 20);
    }

    public void setNewPerDay(int v) {
        sp.edit().putInt("newPerDay", clamp(v, 0, 500)).apply();
    }

    public int reviewPerDay() {
        return sp.getInt("reviewPerDay", 200);
    }

    public void setReviewPerDay(int v) {
        sp.edit().putInt("reviewPerDay", clamp(v, 1, 2000)).apply();
    }

    public String intervals() {
        String s = sp.getString("intervals", STD_INTERVALS);
        return (s == null || s.trim().isEmpty()) ? STD_INTERVALS : s;
    }

    public void setIntervals(String s) {
        sp.edit().putString("intervals", s).apply();
    }

    public boolean reminderEnabled() {
        return sp.getBoolean("reminderEnabled", true);
    }

    public void setReminderEnabled(boolean v) {
        sp.edit().putBoolean("reminderEnabled", v).apply();
    }

    public int reminderHour() {
        return sp.getInt("reminderHour", 20);
    }

    public int reminderMinute() {
        return sp.getInt("reminderMinute", 0);
    }

    public void setReminderTime(int hour, int minute) {
        sp.edit().putInt("reminderHour", clamp(hour, 0, 23)).putInt("reminderMinute", clamp(minute, 0, 59)).apply();
    }

    public String aiBaseUrl() {
        return sp.getString("aiBaseUrl", "https://api.deepseek.com/v1");
    }

    public void setAiBaseUrl(String v) {
        sp.edit().putString("aiBaseUrl", v == null ? "" : v.trim()).apply();
    }

    public String aiModel() {
        return sp.getString("aiModel", "deepseek-chat");
    }

    public void setAiModel(String v) {
        sp.edit().putString("aiModel", v == null ? "" : v.trim()).apply();
    }

    public String aiKey() {
        return sp.getString("aiKey", "");
    }

    public void setAiKey(String v) {
        sp.edit().putString("aiKey", v == null ? "" : v.trim()).apply();
    }

    /** 检查更新的仓库地址，例如 https://github.com/你的用户名/仓库名。 */
    public String updateUrl() {
        return sp.getString("updateUrl", "");
    }

    public void setUpdateUrl(String v) {
        sp.edit().putString("updateUrl", v == null ? "" : v.trim()).apply();
    }

    /** 是否已经撒过示例数据。 */
    public boolean seeded() {
        return sp.getBoolean("seeded", false);
    }

    public void setSeeded(boolean v) {
        sp.edit().putBoolean("seeded", v).apply();
    }

    public boolean shuffle() {
        return sp.getBoolean("shuffle", false);
    }

    public void setShuffle(boolean v) {
        sp.edit().putBoolean("shuffle", v).apply();
    }

    /** 双向复习：开启后每张卡随机从正面或背面开始，正反两个方向都练。 */
    public boolean bidirectional() {
        return sp.getBoolean("bidirectional", false);
    }

    public void setBidirectional(boolean v) {
        sp.edit().putBoolean("bidirectional", v).apply();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
