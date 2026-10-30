package com.suji.app.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.suji.app.R;
import com.suji.app.ai.AiClient;
import com.suji.app.ai.AiPrompts;
import com.suji.app.core.Scheduler;
import com.suji.app.data.Prefs;
import com.suji.app.data.Repo;
import com.suji.app.data.SampleData;
import com.suji.app.util.Async;
import com.suji.app.util.Backup;
import com.suji.app.util.FileIo;
import com.suji.app.util.Reminder;
import com.suji.app.util.Updater;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Repo repo;
    private Prefs prefs;

    private EditText edNewPerDay;
    private EditText edReviewPerDay;
    private SwitchMaterial swShuffle;
    private SwitchMaterial swReminder;
    private TextView tvReminderTime;
    private TextView tvIntervals;
    private TextView tvTest;
    private Spinner spinnerPreset;
    private TextInputEditText edBaseUrl;
    private TextInputEditText edModel;
    private TextInputEditText edKey;
    private MaterialButton btnTest;
    private TextView tvCurrentVersion;
    private TextView tvUpdateResult;
    private TextInputEditText edUpdateUrl;
    private MaterialButton btnCheckUpdate;

    private boolean binding = false;

    private final ActivityResultLauncher<String> createDoc = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    exportTo(uri);
                }
            });

    private final ActivityResultLauncher<String[]> openDoc = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    importFrom(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        repo = Repo.get(this);
        prefs = repo.prefs();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edNewPerDay = findViewById(R.id.edNewPerDay);
        edReviewPerDay = findViewById(R.id.edReviewPerDay);
        swShuffle = findViewById(R.id.swShuffle);
        swReminder = findViewById(R.id.swReminder);
        tvReminderTime = findViewById(R.id.tvReminderTime);
        tvIntervals = findViewById(R.id.tvIntervals);
        tvTest = findViewById(R.id.tvTest);
        spinnerPreset = findViewById(R.id.spinnerPreset);
        edBaseUrl = findViewById(R.id.edBaseUrl);
        edModel = findViewById(R.id.edModel);
        edKey = findViewById(R.id.edKey);
        btnTest = findViewById(R.id.btnTest);
        tvCurrentVersion = findViewById(R.id.tvCurrentVersion);
        tvUpdateResult = findViewById(R.id.tvUpdateResult);
        edUpdateUrl = findViewById(R.id.edUpdateUrl);
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate);

        setupPresetSpinner();
        load();

        tvReminderTime.setOnClickListener(v -> pickTime());
        swReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvReminderTime.setAlpha(isChecked ? 1f : 0.5f);
            if (!binding) {
                prefs.setReminderEnabled(isChecked);
                Reminder.schedule(this);
            }
        });

        btnTest.setOnClickListener(v -> testConnection());
        btnCheckUpdate.setOnClickListener(v -> checkUpdate());
        findViewById(R.id.btnExport).setOnClickListener(v -> exportBackup());
        findViewById(R.id.btnRestore).setOnClickListener(v -> confirmRestore());
        findViewById(R.id.btnSample).setOnClickListener(v -> confirmSample());
        findViewById(R.id.btnClear).setOnClickListener(v -> confirmClear());
    }

    private void setupPresetSpinner() {
        String[] items = {"标准艾宾浩斯（推荐）", "轻松一些", "高强度", "自定义"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, items);
        adapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerPreset.setAdapter(adapter);
        spinnerPreset.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (binding || position == 3) {
                    return;
                }
                if (position == 0) {
                    prefs.setIntervals(Prefs.STD_INTERVALS);
                } else if (position == 1) {
                    prefs.setIntervals(Prefs.EASY_INTERVALS);
                } else if (position == 2) {
                    prefs.setIntervals(Prefs.HARD_INTERVALS);
                }
                updateIntervalsText();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void load() {
        binding = true;
        edNewPerDay.setText(String.valueOf(prefs.newPerDay()));
        edReviewPerDay.setText(String.valueOf(prefs.reviewPerDay()));
        swShuffle.setChecked(prefs.shuffle());
        swReminder.setChecked(prefs.reminderEnabled());
        tvReminderTime.setText(formatTime(prefs.reminderHour(), prefs.reminderMinute()));
        tvReminderTime.setAlpha(prefs.reminderEnabled() ? 1f : 0.5f);

        String iv = prefs.intervals();
        int preset = 0;
        if (iv.equals(Prefs.EASY_INTERVALS)) {
            preset = 1;
        } else if (iv.equals(Prefs.HARD_INTERVALS)) {
            preset = 2;
        } else if (!iv.equals(Prefs.STD_INTERVALS)) {
            preset = 3;
        }
        spinnerPreset.setSelection(preset);
        updateIntervalsText();

        edBaseUrl.setText(prefs.aiBaseUrl());
        edModel.setText(prefs.aiModel());
        edKey.setText(prefs.aiKey());
        edUpdateUrl.setText(prefs.updateUrl());
        tvCurrentVersion.setText("当前版本 " + currentVersionName());
        binding = false;
    }

    private void updateIntervalsText() {
        long[] values = Scheduler.parse(prefs.intervals());
        StringBuilder sb = new StringBuilder("当前曲线：");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(Scheduler.human(values[i]));
        }
        tvIntervals.setText(sb.toString());
    }

    private void pickTime() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            prefs.setReminderTime(hourOfDay, minute);
            tvReminderTime.setText(formatTime(hourOfDay, minute));
            Reminder.schedule(this);
        }, prefs.reminderHour(), prefs.reminderMinute(), true).show();
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private void save() {
        if (edNewPerDay == null) {
            return;
        }
        int newPerDay;
        int reviewPerDay;
        try {
            newPerDay = Integer.parseInt(edNewPerDay.getText().toString().trim());
        } catch (Exception e) {
            newPerDay = prefs.newPerDay();
        }
        try {
            reviewPerDay = Integer.parseInt(edReviewPerDay.getText().toString().trim());
        } catch (Exception e) {
            reviewPerDay = prefs.reviewPerDay();
        }
        prefs.setNewPerDay(newPerDay);
        prefs.setReviewPerDay(reviewPerDay);
        prefs.setShuffle(swShuffle.isChecked());
        prefs.setReminderEnabled(swReminder.isChecked());
        prefs.setAiBaseUrl(text(edBaseUrl));
        prefs.setAiModel(text(edModel));
        prefs.setAiKey(text(edKey));
        prefs.setUpdateUrl(text(edUpdateUrl));
        Reminder.schedule(this);
    }

    private String text(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }

    private void testConnection() {
        final String base = text(edBaseUrl);
        final String model = text(edModel);
        final String key = text(edKey);
        if (key.isEmpty()) {
            tvTest.setText("先填 API Key 再测。");
            return;
        }
        btnTest.setEnabled(false);
        tvTest.setText("正在连接…");
        Async.io(() -> {
            AiClient.Result result = AiClient.chat(base, key, model, "", AiPrompts.pingUser());
            Async.ui(() -> {
                btnTest.setEnabled(true);
                if (result.ok) {
                    String reply = result.text == null ? "" : result.text.trim();
                    if (reply.length() > 60) {
                        reply = reply.substring(0, 60) + "…";
                    }
                    tvTest.setText("连接成功，模型回复：" + reply);
                } else {
                    tvTest.setText(result.error);
                }
            });
        });
    }

    private String currentVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private void checkUpdate() {
        final String repo = text(edUpdateUrl);
        prefs.setUpdateUrl(repo);
        if (repo.isEmpty()) {
            tvUpdateResult.setText("先在下面填好 GitHub 仓库地址，例如 https://github.com/你的用户名/suji");
            return;
        }
        final String current = currentVersionName();
        btnCheckUpdate.setEnabled(false);
        tvUpdateResult.setText("正在检查更新…");
        Async.io(() -> {
            Updater.Info info = Updater.check(repo);
            Async.ui(() -> {
                btnCheckUpdate.setEnabled(true);
                if (!info.ok) {
                    tvUpdateResult.setText(info.error);
                    return;
                }
                if (info.version.isEmpty() || Updater.compareVersion(info.version, current) <= 0) {
                    tvUpdateResult.setText("已是最新版本（" + current + "）");
                    return;
                }
                tvUpdateResult.setText("发现新版本 v" + info.version);
                showUpdateDialog(info, current);
            });
        });
    }

    private void showUpdateDialog(final Updater.Info info, String current) {
        String msg = "当前 v" + current + " → 最新 v" + info.version;
        if (info.changelog != null && !info.changelog.trim().isEmpty()) {
            String c = info.changelog.trim();
            if (c.length() > 200) {
                c = c.substring(0, 200) + "…";
            }
            msg += "\n\n" + c;
        }
        final String url = (info.apkUrl != null && !info.apkUrl.isEmpty()) ? info.apkUrl : info.releaseUrl;
        new AlertDialog.Builder(this)
                .setTitle("发现新版本 v" + info.version)
                .setMessage(msg)
                .setNegativeButton("稍后再说", null)
                .setNeutralButton("浏览器下载", (dialog, which) -> openBrowser(url))
                .setPositiveButton("立即更新", (dialog, which) -> downloadAndInstall(url))
                .show();
    }

    private void openBrowser(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "没有找到下载链接", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadAndInstall(final String apkUrl) {
        if (apkUrl == null || apkUrl.isEmpty()) {
            Toast.makeText(this, "没有找到下载链接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this)
                    .setTitle("需要允许安装")
                    .setMessage("为了能直接在 App 里升级，需要允许「速记」安装应用。请在随后打开的设置页里开启。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("去开启", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "请到系统设置里允许「速记」安装未知应用", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
            return;
        }
        tvUpdateResult.setText("正在下载更新…");
        final File dir = new File(getExternalFilesDir(null), "apk");
        Async.io(() -> {
            try {
                File apk = Updater.downloadApk(apkUrl, dir, percent ->
                        Async.ui(() -> tvUpdateResult.setText("正在下载更新 " + percent + "%")));
                Async.ui(() -> {
                    tvUpdateResult.setText("下载完成，正在安装…");
                    installApk(apk);
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "未知错误" : e.getMessage();
                Async.ui(() -> tvUpdateResult.setText("下载失败：" + msg + "。可点「浏览器下载」或到 GitHub 网页手动下载。"));
            }
        });
    }

    private void installApk(File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(this, "com.suji.app.fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法启动安装，请到 GitHub 网页手动下载", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportBackup() {
        String name = "suji-backup-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(new Date()) + ".json";
        createDoc.launch(name);
    }

    private void exportTo(final Uri uri) {
        Async.task(() -> {
            String json = Backup.exportJson(repo);
            FileIo.writeText(this, uri, json);
            return Boolean.TRUE;
        }, ok -> {
            if (ok == null) {
                Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "备份已保存", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmRestore() {
        new AlertDialog.Builder(this)
                .setTitle("从备份恢复")
                .setMessage("会把你选的文件里的背诵本追加进来，不会覆盖现有内容。")
                .setNegativeButton("取消", null)
                .setPositiveButton("选择文件", (dialog, which) ->
                        openDoc.launch(new String[]{"application/json", "text/*"}))
                .show();
    }

    private void importFrom(final Uri uri) {
        Async.task(() -> {
            String json = FileIo.readText(this, uri);
            return Backup.importJson(repo, json);
        }, count -> {
            if (count == null) {
                Toast.makeText(this, "恢复失败：文件格式不对？", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "已恢复 " + count + " 张卡片", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmSample() {
        new AlertDialog.Builder(this)
                .setTitle("重新导入示例数据")
                .setMessage("会新增三本示例背诵本（英语词、古诗文、概念速记），不影响你已有的内容。")
                .setNegativeButton("取消", null)
                .setPositiveButton("导入", (dialog, which) -> {
                    Async.io(() -> SampleData.seed(repo.db()));
                    Toast.makeText(this, "示例数据已导入", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("清空所有数据")
                .setMessage("所有背诵本、卡片和复习记录都会被删除，不能恢复。建议先导出备份。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (dialog, which) -> {
                    Async.io(() -> repo.clearAll());
                    Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }
}
