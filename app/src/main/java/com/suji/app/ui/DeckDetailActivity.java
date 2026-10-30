package com.suji.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.suji.app.R;
import com.suji.app.data.Card;
import com.suji.app.data.Deck;
import com.suji.app.data.Repo;
import com.suji.app.ui.adapter.CardAdapter;
import com.suji.app.util.Async;

import java.util.ArrayList;
import java.util.List;

public class DeckDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "deck_id";

    private long deckId;
    private Repo repo;
    private Deck deck;
    private CardAdapter adapter;
    private TextView subtitle;
    private RecyclerView list;
    private View empty;
    private EditText search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck_detail);

        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, -1L);
        repo = Repo.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        subtitle = findViewById(R.id.subtitle);
        list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        search = findViewById(R.id.search);

        list.setLayoutManager(new LinearLayoutManager(this));
        int gap = (int) (getResources().getDisplayMetrics().density * 2);
        list.addItemDecoration(new SpaceItemDecoration(gap));

        adapter = new CardAdapter(new CardAdapter.Listener() {
            @Override
            public void onClick(Card card) {
                startActivity(EditCardActivity.intent(DeckDetailActivity.this, card.deckId, card.id));
            }

            @Override
            public void onLongClick(Card card) {
                showCardOptions(card);
            }
        });
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnStudy).setOnClickListener(v -> openStudy(false));
        findViewById(R.id.btnListStudy).setOnClickListener(v -> openStudy(true));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> startActivity(EditCardActivity.intent(this, deckId, -1L)));

        repo.db().decks().observe(deckId).observe(this, d -> {
            if (d == null) {
                finish();
                return;
            }
            deck = d;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(d.name);
            }
        });

        repo.db().cards().observeByDeck(deckId).observe(this, cards -> {
            adapter.submit(cards, repo.intervals());
            boolean isEmpty = cards == null || cards.isEmpty();
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            updateSubtitle(cards);
        });
    }

    private void updateSubtitle(List<Card> cards) {
        if (cards == null) {
            subtitle.setText("共 0 张");
            return;
        }
        long now = System.currentTimeMillis();
        int due = 0;
        int fresh = 0;
        for (Card c : cards) {
            if (c.reviews == 0) {
                fresh++;
            } else if (c.dueAt <= now) {
                due++;
            }
        }
        String text = "共 " + cards.size() + " 张";
        if (due > 0) {
            text += " · 待复习 " + due;
        }
        if (fresh > 0) {
            text += " · 未学 " + fresh;
        }
        subtitle.setText(text);
    }

    private void openStudy(boolean allCards) {
        Intent intent = new Intent(this, StudyActivity.class);
        intent.putExtra(StudyActivity.EXTRA_DECK_ID, deckId);
        intent.putExtra(StudyActivity.EXTRA_ALL_CARDS, allCards);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_deck, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_import) {
            Intent intent = new Intent(this, ImportActivity.class);
            intent.putExtra(ImportActivity.EXTRA_DECK_ID, deckId);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_rename) {
            rename();
            return true;
        }
        if (id == R.id.action_delete) {
            delete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCardOptions(final Card card) {
        String[] options = {"重置这张卡的进度", "删除这张卡"};
        new AlertDialog.Builder(this)
                .setTitle(card.front)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Async.io(() -> repo.resetCard(card));
                        Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show();
                    } else {
                        Async.io(() -> repo.deleteCard(card));
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void rename() {
        if (deck == null) {
            return;
        }
        final EditText input = new EditText(this);
        input.setText(deck.name);
        input.setSelection(deck.name.length());
        new AlertDialog.Builder(this)
                .setTitle("重命名背诵本")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Async.io(() -> repo.renameDeck(deck, name));
                    }
                })
                .show();
    }

    private void delete() {
        if (deck == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("删除背诵本")
                .setMessage("「" + deck.name + "」和里面的 " + adapter.getItemCount() + " 张卡片都会被删除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    final Deck target = deck;
                    Async.io(() -> repo.deleteDeck(target));
                    finish();
                })
                .show();
    }
}
