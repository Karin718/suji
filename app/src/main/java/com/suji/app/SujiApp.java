package com.suji.app;

import android.app.Application;
import android.content.Context;

import com.suji.app.data.Repo;
import com.suji.app.data.SampleData;
import com.suji.app.util.Async;
import com.suji.app.util.Reminder;

public class SujiApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        final Context context = this;
        Async.io(new Runnable() {
            @Override
            public void run() {
                try {
                    Repo repo = Repo.get(context);
                    if (!repo.prefs().seeded()) {
                        if (repo.db().decks().count() == 0) {
                            SampleData.seed(repo.db());
                        }
                        repo.prefs().setSeeded(true);
                    }
                    repo.ensureDeck();
                    Reminder.ensureChannel(context);
                    Reminder.schedule(context);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }
}
