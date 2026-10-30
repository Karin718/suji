package com.suji.app.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.suji.app.R;
import com.suji.app.data.Repo;
import com.suji.app.ui.view.BarChartView;
import com.suji.app.util.Async;
import com.suji.app.util.DateUtil;

public class StatsActivity extends AppCompatActivity {

    private Repo repo;
    private TextView tvToday;
    private TextView tvStreak;
    private TextView tvTotal;
    private TextView tvMastered;
    private TextView tvMastered2;
    private TextView tvNew;
    private TextView tvLearning;
    private TextView tvTip;
    private BarChartView chart;

    private static class Data {
        int[] nums;
        int[] week;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        repo = Repo.get(this);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvToday = findViewById(R.id.tvToday);
        tvStreak = findViewById(R.id.tvStreak);
        tvTotal = findViewById(R.id.tvTotal);
        tvMastered = findViewById(R.id.tvMastered);
        tvMastered2 = findViewById(R.id.tvMastered2);
        tvNew = findViewById(R.id.tvNew);
        tvLearning = findViewById(R.id.tvLearning);
        tvTip = findViewById(R.id.tvTip);
        chart = findViewById(R.id.chart);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Async.task(() -> {
            Data data = new Data();
            data.nums = new int[]{
                    repo.todayReviewed(),
                    repo.streak(),
                    repo.totalCards(),
                    repo.masteredCards(),
                    repo.learningCards(),
                    repo.newCards()
            };
            data.week = repo.last7Days();
            return data;
        }, data -> {
            if (data == null || data.nums == null) {
                return;
            }
            tvToday.setText(String.valueOf(data.nums[0]));
            tvStreak.setText(String.valueOf(data.nums[1]));
            tvTotal.setText(String.valueOf(data.nums[2]));
            tvMastered.setText(String.valueOf(data.nums[3]));
            tvMastered2.setText(data.nums[3] + " 张");
            tvLearning.setText(data.nums[4] + " 张");
            tvNew.setText(data.nums[5] + " 张");

            long today = DateUtil.startOfDay(System.currentTimeMillis());
            String[] labels = new String[7];
            for (int i = 0; i < 7; i++) {
                labels[i] = DateUtil.weekdayShort(DateUtil.addDays(today, i - 6));
            }
            if (data.week != null) {
                chart.setData(data.week, labels);
            }

            if (data.nums[2] == 0) {
                tvTip.setText("还没有卡片。去首页新建一个背诵本，把要背的东西导进来吧。");
            } else if (data.nums[0] == 0) {
                tvTip.setText("今天还没开始复习。先做一轮，保持连续性比一次背很多更重要。");
            } else if (data.nums[1] >= 7) {
                tvTip.setText("已经连续 " + data.nums[1] + " 天了，节奏保持得不错。");
            } else {
                tvTip.setText("记忆的关键是「在快忘的时候复习」。按提醒来，比临时抱佛脚省力得多。");
            }
        });
    }
}
