package com.suji.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.suji.app.R;
import com.suji.app.core.Scheduler;
import com.suji.app.data.Card;
import com.suji.app.data.Deck;
import com.suji.app.data.Repo;
import com.suji.app.util.Async;

import java.util.ArrayList;
import java.util.List;

public class StudyActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "deck_id";
    public static final String EXTRA_ALL_CARDS = "all_cards";

    private Repo repo;
    private long deckId;
    private boolean allCards;
    private final List<Card> queue = new ArrayList<>();
    private int index = 0;
    private boolean revealed = false;
    private final int[] counts = new int[3];
    private long editingCardId = -1L;

    private TextView tvProgress;
    private TextView tvDeckName;
    private LinearProgressIndicator progress;
    private MaterialCardView cardView;
    private TextView tvFront;
    private TextView tvBack;
    private TextView tvNote;
    private View divider;
    private TextView tvTapHint;
    private MaterialButton btnReveal;
    private View gradeBox;
    private TextView tvNextHint;
    private View doneBox;
    private TextView tvDoneTitle;
    private TextView tvDoneStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study);

        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, -1L);
        allCards = getIntent().getBooleanExtra(EXTRA_ALL_CARDS, false);
        repo = Repo.get(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvProgress = findViewById(R.id.tvProgress);
        tvDeckName = findViewById(R.id.tvDeckName);
        progress = findViewById(R.id.progress);
        cardView = findViewById(R.id.cardView);
        tvFront = findViewById(R.id.tvFront);
        tvBack = findViewById(R.id.tvBack);
        tvNote = findViewById(R.id.tvNote);
        divider = findViewById(R.id.divider);
        tvTapHint = findViewById(R.id.tvTapHint);
        btnReveal = findViewById(R.id.btnReveal);
        gradeBox = findViewById(R.id.gradeBox);
        tvNextHint = findViewById(R.id.tvNextHint);
        doneBox = findViewById(R.id.doneBox);
        tvDoneTitle = findViewById(R.id.tvDoneTitle);
        tvDoneStats = findViewById(R.id.tvDoneStats);

        cardView.setOnClickListener(v -> reveal());
        btnReveal.setOnClickListener(v -> reveal());
        findViewById(R.id.btnForgot).setOnClickListener(v -> grade(Scheduler.GRADE_FORGOT));
        findViewById(R.id.btnVague).setOnClickListener(v -> grade(Scheduler.GRADE_VAGUE));
        findViewById(R.id.btnGood).setOnClickListener(v -> grade(Scheduler.GRADE_GOOD));
        findViewById(R.id.btnDoneBack).setOnClickListener(v -> finish());

        loadDeckName();
        loadQueue();
    }

    private void loadDeckName() {
        if (deckId <= 0) {
            tvDeckName.setText("全部背诵本");
            return;
        }
        Async.task(() -> repo.db().decks().byId(deckId), deck -> {
            Deck d = (Deck) deck;
            if (d != null) {
                tvDeckName.setText(d.name);
            }
        });
    }

    private void loadQueue() {
        Async.task(() -> {
            if (allCards && deckId > 0) {
                return repo.db().cards().byDeck(deckId);
            }
            return repo.buildQueue(deckId);
        }, cards -> {
            queue.clear();
            if (cards != null) {
                queue.addAll(cards);
            }
            index = 0;
            if (queue.isEmpty()) {
                showDone(true);
            } else {
                showCard();
            }
        });
    }

    private void showCard() {
        if (index < 0 || index >= queue.size()) {
            showDone(false);
            return;
        }
        Card card = queue.get(index);
        revealed = false;

        // 双向复习：随机决定从哪一面开始
        boolean hasBack = card.back != null && !card.back.trim().isEmpty();
        boolean swapped = hasBack && repo.prefs().bidirectional() && Math.random() < 0.5;
        String question = swapped ? card.back : card.front;
        String answer = swapped ? card.front : card.back;

        tvFront.setText(question);
        tvBack.setText(answer == null || answer.trim().isEmpty() ? "（这张卡没有背面）" : answer);
        tvBack.setVisibility(View.GONE);
        divider.setVisibility(View.GONE);

        if (card.note != null && !card.note.trim().isEmpty()) {
            tvNote.setText(card.note);
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setVisibility(View.GONE);
        }

        tvTapHint.setVisibility(View.VISIBLE);
        btnReveal.setVisibility(View.VISIBLE);
        gradeBox.setVisibility(View.GONE);
        tvNextHint.setVisibility(View.GONE);
        cardView.setVisibility(View.VISIBLE);
        doneBox.setVisibility(View.GONE);

        int total = queue.size();
        int percent = total == 0 ? 0 : (index * 100 / total);
        progress.setProgressCompat(percent, true);
        tvProgress.setText((index + 1) + " / " + total);
    }

    private void reveal() {
        if (revealed || queue.isEmpty() || index >= queue.size()) {
            return;
        }
        revealed = true;
        tvBack.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);
        if (tvNote.getText() != null && tvNote.getText().length() > 0) {
            tvNote.setVisibility(View.VISIBLE);
        }
        tvTapHint.setVisibility(View.GONE);
        btnReveal.setVisibility(View.GONE);
        gradeBox.setVisibility(View.VISIBLE);
        tvNextHint.setVisibility(View.VISIBLE);

        Card card = queue.get(index);
        long[] intervals = repo.intervals();
        String hint = "记得 → " + Scheduler.nextText(card.stage, Scheduler.GRADE_GOOD, intervals, System.currentTimeMillis())
                + "   模糊 → " + Scheduler.nextText(card.stage, Scheduler.GRADE_VAGUE, intervals, System.currentTimeMillis())
                + "   忘记 → " + Scheduler.nextText(card.stage, Scheduler.GRADE_FORGOT, intervals, System.currentTimeMillis());
        tvNextHint.setText(hint);
    }

    private void grade(final int grade) {
        if (!revealed || index >= queue.size()) {
            return;
        }
        counts[grade]++;
        final Card card = queue.get(index);
        Async.io(() -> repo.grade(card, grade));
        advance();
    }

    private void advance() {
        index++;
        if (index >= queue.size()) {
            showDone(false);
        } else {
            showCard();
        }
    }

    private void showDone(boolean nothingToDo) {
        cardView.setVisibility(View.GONE);
        doneBox.setVisibility(View.VISIBLE);
        btnReveal.setVisibility(View.GONE);
        gradeBox.setVisibility(View.GONE);
        tvNextHint.setVisibility(View.GONE);
        progress.setProgressCompat(100, true);

        if (nothingToDo) {
            tvProgress.setText("0 / 0");
            tvDoneTitle.setText("今天没有要复习的卡片");
            tvDoneStats.setText("可以去「导入内容」里加新东西，或者休息一下。");
            return;
        }

        int total = counts[0] + counts[1] + counts[2];
        int rate = total == 0 ? 0 : Math.round((counts[1] + counts[2]) * 100f / total);
        tvProgress.setText(queue.size() + " / " + queue.size());
        tvDoneTitle.setText("这一轮完成了");
        tvDoneStats.setText("复习 " + total + " 张\n记得 " + counts[2] + " · 模糊 " + counts[1]
                + " · 忘记 " + counts[0] + "\n记忆保持率 " + rate + "%");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_study, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_card) {
            if (!queue.isEmpty() && index < queue.size()) {
                Card card = queue.get(index);
                editingCardId = card.id;
                startActivity(EditCardActivity.intent(this, card.deckId, card.id));
            }
            return true;
        }
        if (id == R.id.action_skip) {
            if (!queue.isEmpty() && index < queue.size()) {
                Toast.makeText(this, "已跳过，下次还会出现", Toast.LENGTH_SHORT).show();
                advance();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (editingCardId > 0) {
            final long id = editingCardId;
            editingCardId = -1L;
            Async.task(() -> repo.db().cards().byId(id), card -> {
                Card fresh = (Card) card;
                if (fresh == null) {
                    return;
                }
                for (int i = 0; i < queue.size(); i++) {
                    if (queue.get(i).id == fresh.id) {
                        queue.set(i, fresh);
                        break;
                    }
                }
                if (index < queue.size() && queue.get(index).id == fresh.id) {
                    showCard();
                }
            });
        }
    }
}
