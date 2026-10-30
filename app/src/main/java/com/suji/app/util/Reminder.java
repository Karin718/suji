package com.suji.app.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.suji.app.R;
import com.suji.app.data.Prefs;
import com.suji.app.ui.MainActivity;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/** 每日提醒：用 WorkManager 每 24 小时检查一次，有待复习内容才发通知。 */
public final class Reminder {

    public static final String CHANNEL_ID = "suji_reminder";
    public static final String WORK_NAME = "suji_daily_reminder";

    private Reminder() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "每日复习提醒", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("到点提醒你复习今天该复习的卡片");
        nm.createNotificationChannel(channel);
    }

    public static void schedule(Context context) {
        Prefs prefs = new Prefs(context);
        WorkManager wm = WorkManager.getInstance(context);
        if (!prefs.reminderEnabled()) {
            wm.cancelUniqueWork(WORK_NAME);
            return;
        }
        long delay = delayUntilNext(prefs.reminderHour(), prefs.reminderMinute());
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build();
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request);
    }

    public static long delayUntilNext(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }
        return Math.max(1000L, target.getTimeInMillis() - now.getTimeInMillis());
    }

    public static void show(Context context, int due, int fresh) {
        ensureChannel(context);
        StringBuilder text = new StringBuilder();
        if (due > 0) {
            text.append(due).append(" 张到期");
        }
        if (fresh > 0) {
            if (text.length() > 0) {
                text.append(" · ");
            }
            text.append(fresh).append(" 张新卡");
        }
        if (text.length() == 0) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_suji)
                .setContentTitle("该复习了")
                .setContentText(text.toString())
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(1001, builder.build());
        } catch (SecurityException ignored) {
            // 用户没有给通知权限，静默跳过
        }
    }
}
