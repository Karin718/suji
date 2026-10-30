package com.suji.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.suji.app.R;
import com.suji.app.core.Scheduler;
import com.suji.app.data.Card;
import com.suji.app.data.Repo;
import com.suji.app.util.Async;
import com.suji.app.util.DateUtil;

public class EditCardActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "deck_id";
    public static final String EXTRA_CARD_ID = "card_id";

    private long deckId;
    private long cardId;
    private Repo repo;
    private Card card;

    private TextInputEditText edFront;
    private TextInputEditText edBack;
    private TextInputEditText edNote;
    private TextInputEditText edTags;
    private TextView tvStageInfo;
    private MaterialButton btnReset;

    public static Intent intent(Context context, long deckId, long cardId) {
        Intent intent = new Intent(context, EditCardActivity.class);
        intent.putExtra(EXTRA_DECK_ID, deckId);
        intent.putExtra(EXTRA_CARD_ID, cardId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_card);

        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, -1L);
        cardId = getIntent().getLongExtra(EXTRA_CARD_ID, -1L);
        repo = Repo.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edFront = findViewById(R.id.edFront);
        edBack = findViewById(R.id.edBack);
        edNote = findViewById(R.id.edNote);
        edTags = findViewById(R.id.edTags);
        tvStageInfo = findViewById(R.id.tvStageInfo);
        btnReset = findViewById(R.id.btnReset);

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        btnReset.setOnClickListener(v -> reset());

        if (cardId > 0) {
            Async.task(() -> repo.db().cards().byId(cardId), loaded -> {
                if (loaded == null) {
                    Toast.makeText(this, "卡片不存在", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                card = loaded;
                fill();
            });
        } else {
            card = new Card(deckId, "", "");
            tvStageInfo.setVisibility(View.GONE);
            btnReset.setVisibility(View.GONE);
        }
    }

    private void fill() {
        edFront.setText(card.front);
        edBack.setText(card.back);
        edNote.setText(card.note);
        edTags.setText(card.tags);

        long[] intervals = repo.intervals();
        String status = Scheduler.statusText(card.stage, card.reviews, card.dueAt, intervals, System.currentTimeMillis());
        String info = "状态：" + status + " · 复习 " + card.reviews + " 次";
        if (card.lapses > 0) {
            info += " · 遗忘 " + card.lapses + " 次";
        }
        if (card.reviews > 0 && card.stage < intervals.length) {
            info += "\n下次复习：" + DateUtil.datetime(card.dueAt);
        }
        tvStageInfo.setText(info);
    }

    private void save() {
        String front = edFront.getText() == null ? "" : edFront.getText().toString().trim();
        String back = edBack.getText() == null ? "" : edBack.getText().toString().trim();
        if (front.isEmpty()) {
            Toast.makeText(this, "正面不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (card == null) {
            card = new Card(deckId, "", "");
        }
        if (card.deckId <= 0) {
            card.deckId = deckId;
        }
        card.front = front;
        card.back = back;
        card.note = edNote.getText() == null ? "" : edNote.getText().toString().trim();
        card.tags = edTags.getText() == null ? "" : edTags.getText().toString().trim();

        final Card target = card;
        Async.io(() -> {
            if (target.deckId <= 0) {
                target.deckId = repo.ensureDeck().id;
            }
            repo.saveCard(target);
        });
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void reset() {
        if (card == null || card.id == 0) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("重置复习进度")
                .setMessage("这张卡会变回未学状态，重新从第一阶开始。")
                .setNegativeButton("取消", null)
                .setPositiveButton("重置", (dialog, which) -> {
                    final Card target = card;
                    Async.task(() -> {
                        repo.resetCard(target);
                        return repo.db().cards().byId(target.id);
                    }, fresh -> {
                        if (fresh != null) {
                            card = fresh;
                            fill();
                        }
                        Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show();
                    });
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_card, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete_card) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        if (card == null || card.id == 0) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("删除这张卡")
                .setMessage("删除后不能恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    final Card target = card;
                    Async.io(() -> repo.deleteCard(target));
                    finish();
                })
                .show();
    }
}
