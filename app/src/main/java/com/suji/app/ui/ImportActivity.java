package com.suji.app.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.suji.app.R;
import com.suji.app.ai.AiClient;
import com.suji.app.ai.AiParser;
import com.suji.app.ai.AiPrompts;
import com.suji.app.data.Card;
import com.suji.app.data.Deck;
import com.suji.app.data.Prefs;
import com.suji.app.data.Repo;
import com.suji.app.ui.adapter.PreviewAdapter;
import com.suji.app.util.Async;
import com.suji.app.util.FileIo;
import com.suji.app.util.Parser;

import java.util.ArrayList;
import java.util.List;

public class ImportActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "deck_id";

    private Repo repo;
    private long deckId = -1L;
    private final List<Deck> decks = new ArrayList<>();

    private Spinner spinnerDeck;
    private Spinner spinnerMode;
    private EditText edRaw;
    private View inputBox;
    private View previewBox;
    private RecyclerView list;
    private TextView tvStatus;
    private TextView tvPreviewInfo;
    private MaterialButton btnImport;
    private MaterialButton btnAi;
    private PreviewAdapter adapter;

    private final ActivityResultLauncher<String[]> openFile = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    readFile(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, -1L);
        repo = Repo.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        spinnerDeck = findViewById(R.id.spinnerDeck);
        spinnerMode = findViewById(R.id.spinnerMode);
        edRaw = findViewById(R.id.edRaw);
        inputBox = findViewById(R.id.inputBox);
        previewBox = findViewById(R.id.previewBox);
        list = findViewById(R.id.list);
        tvStatus = findViewById(R.id.tvStatus);
        tvPreviewInfo = findViewById(R.id.tvPreviewInfo);
        btnImport = findViewById(R.id.btnImport);
        btnAi = findViewById(R.id.btnAi);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new SpaceItemDecoration((int) (getResources().getDisplayMetrics().density * 2)));

        setupModeSpinner();
        loadDecks();

        btnAi.setOnClickListener(v -> runAi());
        findViewById(R.id.btnParse).setOnClickListener(v -> parseLocal());
        findViewById(R.id.btnFile).setOnClickListener(v -> openFile.launch(new String[]{"text/plain", "text/*", "application/json"}));
        findViewById(R.id.btnImport).setOnClickListener(v -> doImport());
        findViewById(R.id.btnBack).setOnClickListener(v -> backToInput());
        findViewById(R.id.btnAll).setOnClickListener(v -> {
            if (adapter != null) {
                adapter.selectAll(true);
                updatePreviewInfo();
            }
        });
        findViewById(R.id.btnNone).setOnClickListener(v -> {
            if (adapter != null) {
                adapter.selectAll(false);
                updatePreviewInfo();
            }
        });

        spinnerDeck.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == decks.size()) {
                    showNewDeckDialog();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupModeSpinner() {
        String[] modes = {"AI：自动判断", "AI：问答式", "AI：术语解释", "AI：填空挖空"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, modes);
        adapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerMode.setAdapter(adapter);
    }

    private void loadDecks() {
        Async.task(() -> {
            List<Deck> result = repo.db().decks().all();
            if (result.isEmpty()) {
                repo.ensureDeck();
                result = repo.db().decks().all();
            }
            return result;
        }, loaded -> {
            decks.clear();
            if (loaded != null) {
                decks.addAll(loaded);
            }
            if (decks.isEmpty()) {
                return;
            }
            List<String> names = new ArrayList<>();
            for (Deck d : decks) {
                names.add(d.name);
            }
            names.add("＋ 新建背诵本…");
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, names);
            spinnerAdapter.setDropDownViewResource(R.layout.item_spinner);
            spinnerDeck.setAdapter(spinnerAdapter);

            int selected = 0;
            for (int i = 0; i < decks.size(); i++) {
                if (decks.get(i).id == deckId) {
                    selected = i;
                }
            }
            spinnerDeck.setSelection(selected);
        });
    }

    private Deck currentDeck() {
        int pos = spinnerDeck.getSelectedItemPosition();
        if (pos < 0 || pos >= decks.size()) {
            return decks.isEmpty() ? null : decks.get(0);
        }
        return decks.get(pos);
    }

    private void showNewDeckDialog() {
        final EditText input = new EditText(this);
        input.setHint("新背诵本的名字");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("新建背诵本")
                .setView(input)
                .setNegativeButton("取消", (dialog, which) -> spinnerDeck.setSelection(0))
                .setPositiveButton("创建", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "名字不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Async.task(() -> repo.createDeck(name, ""), deck -> {
                        if (deck != null) {
                            deckId = deck.id;
                            loadDecks();
                            Toast.makeText(this, "已创建：" + deck.name, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void readFile(final Uri uri) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("正在读取文件…");
        final String name = displayName(uri);
        Async.task(() -> FileIo.readText(this, uri), text -> {
            if (text == null) {
                tvStatus.setText("读取失败，换一个文件试试。");
                return;
            }
            edRaw.setText(text);
            tvStatus.setVisibility(View.GONE);
            parseFileContent(text, name);
        });
        if (name != null) {
            tvStatus.setText("正在读取 " + name + "…");
        }
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
            // 忽略
        }
        return null;
    }

    private void parseLocal() {
        String raw = edRaw.getText() == null ? "" : edRaw.getText().toString();
        if (raw.trim().isEmpty()) {
            Toast.makeText(this, "先把要背的内容粘贴进来", Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();
        List<Card> cards = parseContent(raw, null);
        if (cards.isEmpty()) {
            Toast.makeText(this, "没解析出内容，检查一下格式", Toast.LENGTH_SHORT).show();
            return;
        }
        showPreview(cards, "解析出 " + cards.size() + " 条");
    }

    /** 根据文件后缀选择解析方式：.json / .csv / 其它按纯文本。 */
    private List<Card> parseContent(String content, String fileName) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".json")) {
                return Parser.parseJson(content);
            }
            if (lower.endsWith(".csv")) {
                return Parser.parseCsv(content);
            }
        }
        return Parser.parse(content);
    }

    private void parseFileContent(String content, String fileName) {
        hideKeyboard();
        List<Card> cards = parseContent(content, fileName);
        if (cards.isEmpty()) {
            Toast.makeText(this, "没解析出内容，检查一下文件格式", Toast.LENGTH_SHORT).show();
            return;
        }
        showPreview(cards, "解析出 " + cards.size() + " 条");
    }

    private void runAi() {
        Prefs prefs = repo.prefs();
        final String raw = edRaw.getText() == null ? "" : edRaw.getText().toString();
        if (raw.trim().isEmpty()) {
            Toast.makeText(this, "先把原始资料粘贴进来", Toast.LENGTH_SHORT).show();
            return;
        }
        if (prefs.aiKey().trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("还没配置 AI")
                    .setMessage("到「设置 → AI 整理」里填一个 API Key（支持 DeepSeek、OpenAI 等兼容接口），回来就能用了。")
                    .setNegativeButton("以后再说", null)
                    .setPositiveButton("去设置", (dialog, which) ->
                            startActivity(new Intent(this, SettingsActivity.class)))
                    .show();
            return;
        }

        String mode = AiPrompts.MODE_AUTO;
        int modePos = spinnerMode.getSelectedItemPosition();
        if (modePos == 1) {
            mode = AiPrompts.MODE_QA;
        } else if (modePos == 2) {
            mode = AiPrompts.MODE_TERM;
        } else if (modePos == 3) {
            mode = AiPrompts.MODE_CLOZE;
        }

        hideKeyboard();
        btnAi.setEnabled(false);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("AI 正在整理资料，内容多的话需要十几秒…");

        final String finalMode = mode;
        Async.io(() -> {
            AiClient.Result result = AiClient.chat(
                    prefs.aiBaseUrl(), prefs.aiKey(), prefs.aiModel(),
                    AiPrompts.system(), AiPrompts.user(raw, finalMode, 80));
            Async.ui(() -> {
                btnAi.setEnabled(true);
                if (!result.ok) {
                    tvStatus.setText(result.error);
                    return;
                }
                List<Card> cards = AiParser.parse(result.text);
                if (cards.isEmpty()) {
                    tvStatus.setText("AI 没有返回可用的卡片，试试减少内容、或换个整理方式。");
                    return;
                }
                tvStatus.setVisibility(View.GONE);
                showPreview(cards, "AI 整理出 " + cards.size() + " 条");
            });
        });
    }

    private void showPreview(List<Card> cards, String info) {
        adapter = new PreviewAdapter(cards, position -> editCard(position));
        list.setAdapter(adapter);
        inputBox.setVisibility(View.GONE);
        previewBox.setVisibility(View.VISIBLE);
        tvPreviewInfo.setText("共 " + cards.size() + " 条，点右侧铅笔可以改");
        updatePreviewInfo();
        btnImport.setText("导入 " + cards.size() + " 条");
        if (info != null && !info.isEmpty()) {
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePreviewInfo() {
        if (adapter == null) {
            return;
        }
        btnImport.setText("导入选中 " + adapter.selectedCount() + " 条");
    }

    private void editCard(final int position) {
        if (adapter == null) {
            return;
        }
        final Card card = adapter.get(position);
        if (card == null) {
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 20);
        box.setPadding(pad, pad / 2, pad, 0);

        final EditText edFront = new EditText(this);
        edFront.setHint("正面");
        edFront.setText(card.front);
        edFront.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edFront.setMinLines(2);

        final EditText edBack = new EditText(this);
        edBack.setHint("背面");
        edBack.setText(card.back);
        edBack.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edBack.setMinLines(2);

        box.addView(edFront);
        box.addView(edBack);

        new AlertDialog.Builder(this)
                .setTitle("修改这一条")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String f = edFront.getText().toString().trim();
                    String b = edBack.getText().toString().trim();
                    if (!f.isEmpty()) {
                        card.front = f;
                        card.back = b;
                        adapter.notifyItemChanged(position);
                    }
                })
                .show();
    }

    private void doImport() {
        if (adapter == null) {
            return;
        }
        final List<Card> selected = adapter.selected();
        if (selected.isEmpty()) {
            Toast.makeText(this, "一条都没选中", Toast.LENGTH_SHORT).show();
            return;
        }
        final Deck deck = currentDeck();
        if (deck == null) {
            Toast.makeText(this, "先选一个背诵本", Toast.LENGTH_SHORT).show();
            return;
        }
        Async.io(() -> repo.addCards(deck.id, selected));
        Toast.makeText(this, "已导入 " + selected.size() + " 条到「" + deck.name + "」", Toast.LENGTH_LONG).show();
        finish();
    }

    private void backToInput() {
        previewBox.setVisibility(View.GONE);
        inputBox.setVisibility(View.VISIBLE);
        adapter = null;
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (previewBox.getVisibility() == View.VISIBLE) {
            backToInput();
            return;
        }
        super.onBackPressed();
    }
}
