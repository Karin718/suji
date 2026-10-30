package com.suji.app.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.suji.app.R;
import com.suji.app.data.Card;
import com.suji.app.data.Deck;
import com.suji.app.data.DeckStat;
import com.suji.app.data.Repo;
import com.suji.app.ui.adapter.DeckAdapter;
import com.suji.app.util.Async;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Repo repo;
    private DeckAdapter adapter;
    private RecyclerView list;
    private TextView tvDueCount;
    private TextView tvNewCount;
    private TextView tvDeckCount;
    private View empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = Repo.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvDueCount = findViewById(R.id.tvDueCount);
        tvNewCount = findViewById(R.id.tvNewCount);
        tvDeckCount = findViewById(R.id.tvDeckCount);
        empty = findViewById(R.id.empty);
        list = findViewById(R.id.list);
        FloatingActionButton fab = findViewById(R.id.fab);

        list.setLayoutManager(new LinearLayoutManager(this));
        int gap = (int) (getResources().getDisplayMetrics().density * 2);
        list.addItemDecoration(new SpaceItemDecoration(gap));

        adapter = new DeckAdapter(new DeckAdapter.Listener() {
            @Override
            public void onClick(Deck deck) {
                openDeck(deck);
            }

            @Override
            public void onLongClick(Deck deck) {
                showDeckOptions(deck);
            }
        });
        list.setAdapter(adapter);

        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStudy(-1);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewDeckDialog();
            }
        });

        repo.db().decks().observeStats().observe(this, stats -> {
            adapter.submit(stats);
            boolean isEmpty = stats == null || stats.isEmpty();
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            tvDeckCount.setText(stats == null || stats.isEmpty() ? "" : stats.size() + " 个");
        });

        askNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshToday();
    }

    private void refreshToday() {
        Async.task(() -> new int[]{repo.dueNow(-1), repo.newLeftToday()}, counts -> {
            if (counts == null) {
                return;
            }
            tvDueCount.setText(String.valueOf(counts[0]));
            tvNewCount.setText(String.valueOf(counts[1]));
        });
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 10);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_stats) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openDeck(Deck deck) {
        Intent intent = new Intent(this, DeckDetailActivity.class);
        intent.putExtra(DeckDetailActivity.EXTRA_DECK_ID, deck.id);
        startActivity(intent);
    }

    private void openStudy(long deckId) {
        Intent intent = new Intent(this, StudyActivity.class);
        intent.putExtra(StudyActivity.EXTRA_DECK_ID, deckId);
        startActivity(intent);
    }

    private void showNewDeckDialog() {
        final EditText input = new EditText(this);
        input.setHint("例如：考研英语核心词");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        int pad = (int) (getResources().getDisplayMetrics().density * 20);
        input.setPadding(pad, pad / 2, pad, pad / 2);

        new AlertDialog.Builder(this)
                .setTitle("新建背诵本")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("创建", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "名字不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Async.task(() -> repo.createDeck(name, ""), deck -> {
                        if (deck != null) {
                            Toast.makeText(this, "已创建：" + deck.name, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void showDeckOptions(final Deck deck) {
        final String[] options = {"重命名", "导入内容", "清空复习进度", "删除背诵本"};
        new AlertDialog.Builder(this)
                .setTitle(deck.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        renameDeck(deck);
                    } else if (which == 1) {
                        Intent intent = new Intent(this, ImportActivity.class);
                        intent.putExtra(ImportActivity.EXTRA_DECK_ID, deck.id);
                        startActivity(intent);
                    } else if (which == 2) {
                        resetDeck(deck);
                    } else {
                        deleteDeck(deck);
                    }
                })
                .show();
    }

    private void renameDeck(final Deck deck) {
        final EditText input = new EditText(this);
        input.setText(deck.name);
        input.setSelection(deck.name.length());
        new AlertDialog.Builder(this)
                .setTitle("重命名")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        return;
                    }
                    Async.io(() -> repo.renameDeck(deck, name));
                })
                .show();
    }

    private void resetDeck(final Deck deck) {
        new AlertDialog.Builder(this)
                .setTitle("清空复习进度")
                .setMessage("会把「" + deck.name + "」里所有卡片变回未学状态，卡片内容不会删除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> Async.io(() -> {
                    List<Card> cards = repo.db().cards().byDeck(deck.id);
                    for (Card c : cards) {
                        repo.db().cards().resetProgress(c.id, System.currentTimeMillis());
                    }
                }))
                .show();
    }

    private void deleteDeck(final Deck deck) {
        new AlertDialog.Builder(this)
                .setTitle("删除背诵本")
                .setMessage("「" + deck.name + "」和里面的所有卡片都会被删除，不能恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> Async.io(() -> repo.deleteDeck(deck)))
                .show();
    }
}
