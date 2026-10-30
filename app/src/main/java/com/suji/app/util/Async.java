package com.suji.app.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 极简的线程调度工具：IO 线程跑数据库，主线程更新 UI。 */
public final class Async {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() {
    }

    public static void io(Runnable r) {
        POOL.execute(r);
    }

    public static void ui(Runnable r) {
        MAIN.post(r);
    }

    public static <T> void task(final Task<T> work, final Callback<T> callback) {
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                T value = null;
                try {
                    value = work.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                final T result = value;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(result);
                    }
                });
            }
        });
    }

    public interface Task<T> {
        T run() throws Exception;
    }

    public interface Callback<T> {
        void onResult(T value);
    }
}
