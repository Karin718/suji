package com.suji.app.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.suji.app.data.Prefs;
import com.suji.app.data.Repo;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Prefs prefs = new Prefs(context);
        if (!prefs.reminderEnabled()) {
            return Result.success();
        }
        try {
            Repo repo = Repo.get(context);
            int due = repo.dueNow(-1);
            int fresh = repo.newLeftToday();
            if (due + fresh > 0) {
                Reminder.show(context, due, fresh);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return Result.success();
    }
}
