package com.mixia.vault;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mixia.vault.core.AesGcm;
import com.mixia.vault.core.VaultFile;
import com.mixia.vault.model.Entry;
import com.mixia.vault.model.Vault;
import com.mixia.vault.model.Wallet;
import com.mixia.vault.util.AppDialog;
import com.mixia.vault.util.BiometricHelper;
import com.mixia.vault.util.Ui;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Settings: lock timing, backup, master password, destructive actions. */
public class SettingsActivity extends BaseActivity {
    private static final int REQ_EXPORT = 1001;
    private static final int REQ_IMPORT = 1002;
    private VaultApp app;
    private LinearLayout container;
    private final String[] lockLabels = {"立即", "30 秒", "1 分钟", "5 分钟"};
    private final int[] lockValues = {0, 30, 60, 300};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        app = VaultApp.get(this);
        container = findViewById(R.id.settingsContainer);
        findViewById(R.id.btnBackS).setOnClickListener(v -> finish());
        build();
    }

    private void build() {
        container.removeAllViews();
        addSection("安全");
        addRow("自动锁定", "离开应用后多久重新上锁", lockLabel(app.autolockSeconds()), v -> chooseAutolock());
        addRow("剪贴板自动清除", "复制的密码 45 秒后自动清除", app.clipboardClear() ? "已开启" : "已关闭",
                v -> {
                    boolean next = !app.clipboardClear();
                    app.prefs().edit().putBoolean(VaultApp.K_CLIPBOARD, next).apply();
                    build();
                    toast(next ? "已开启剪贴板清除" : "已关闭剪贴板清除");
                });
        if (BiometricHelper.isAvailable(this)) {
            addRow("指纹解锁", "用指纹代替输入主密码", app.biometricEnabled() ? "已开启" : "已关闭",
                    v -> toggleBiometric());
        }
        addRow("修改主密码", "重新加密整个保险库", "", v -> changeMasterPassword());
        addRow("主密码提示", "连续输错 5 次后显示", "", v -> editHint());

        addSection("数据");
        addRow("导出加密备份", "生成一份仍是 AES-256 加密的备份文件", "", v -> exportBackup());
        addRow("从备份恢复", "导入备份会覆盖当前保险库", "", v -> importBackup());
        addRow("清空保险库", "删除全部记录，无法恢复", "", v -> wipeEverything());

        addSection("关于");
        addRow("密匣 " + versionName(), "纯本地密码管理器 · 不联网", "", v -> showAbout());
        addRow("加密方式", "AES-256-GCM · PBKDF2-HMAC-SHA256 250,000 次", "", v -> showCrypto());
        addRow("网络权限", "未申请 INTERNET 权限，物理上无法联网", "无网络", null);
    }

    private void addSection(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(12);
        tv.setTextColor(0xFF4EC9B0);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 18);
        lp.bottomMargin = Ui.dp(this, 8);
        container.addView(tv, lp);
    }

    private void addRow(String title, String subtitle, String value, View.OnClickListener listener) {
        View row = getLayoutInflater().inflate(R.layout.item_setting, container, false);
        TextView t = row.findViewById(R.id.setTitle);
        TextView s = row.findViewById(R.id.setSub);
        TextView v = row.findViewById(R.id.setValue);
        t.setText(title);
        if (subtitle != null && !subtitle.isEmpty()) {
            s.setText(subtitle);
            s.setVisibility(View.VISIBLE);
        }
        if (value != null && !value.isEmpty()) v.setText(value);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 8);
        row.setLayoutParams(lp);
        if (listener != null) row.setOnClickListener(listener);
        else row.findViewById(R.id.setChevron).setVisibility(View.GONE);
        container.addView(row);
    }

    private String lockLabel(int seconds) {
        for (int i = 0; i < lockValues.length; i++) {
            if (lockValues[i] == seconds) return lockLabels[i];
        }
        return seconds + " 秒";
    }

    private void chooseAutolock() {
        int current = 2;
        for (int i = 0; i < lockValues.length; i++) {
            if (lockValues[i] == app.autolockSeconds()) current = i;
        }
        AppDialog.singleChoice(this, "自动锁定", lockLabels, current, index -> {
            app.prefs().edit().putInt(VaultApp.K_AUTOLOCK, lockValues[index]).apply();
            build();
        });
    }

    private void toggleBiometric() {
        if (app.biometricEnabled()) {
            app.prefs().edit().putBoolean(VaultApp.K_BIOMETRIC, false).apply();
            BiometricHelper.clear(this);
            build();
            toast("已关闭指纹解锁");
            return;
        }
        if (app.vaultFile() == null) return;
        BiometricHelper.enable(this, app.vaultFile().key(), new BiometricHelper.Callback() {
            @Override
            public void onSuccess(byte[] vaultKey) {
                app.prefs().edit().putBoolean(VaultApp.K_BIOMETRIC, true).apply();
                build();
                toast("指纹解锁已启用");
            }

            @Override
            public void onFailure(String message) {
                toast(message);
            }
        });
    }

    private void editHint() {
        final EditText input = new EditText(this);
        input.setHint("留空表示不显示提示");
        input.setText(app.prefs().getString(VaultApp.K_HINT, ""));
        input.setTextColor(0xFFE9EEF8);
        input.setHintTextColor(0xFF5E6A7E);
        int pad = Ui.dp(this, 20);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(pad, Ui.dp(this, 8), pad, 0);
        wrap.addView(input);
        AppDialog.input(this, "主密码提示", "留空表示不显示提示",
                app.prefs().getString(VaultApp.K_HINT, ""), false, value -> {
                    app.prefs().edit().putString(VaultApp.K_HINT, value.trim()).apply();
                    toast("已保存");
                });
    }

    private void changeMasterPassword() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 20);
        wrap.setPadding(pad, Ui.dp(this, 8), pad, 0);
        final EditText current = passwordField("当前主密码");
        final EditText fresh = passwordField("新主密码（至少 10 位）");
        final EditText confirm = passwordField("再次输入新主密码");
        wrap.addView(current);
        wrap.addView(fresh);
        wrap.addView(confirm);
        AppDialog.Builder builder = new AppDialog.Builder(this)
                .title("修改主密码")
                .message("修改后旧密码立即失效，指纹解锁需要重新开启。")
                .content(wrap)
                .positive("修改", () -> doChangePassword(
                        current.getText().toString(), fresh.getText().toString(), confirm.getText().toString()));
        builder.show();
    }

    private EditText passwordField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setTextColor(0xFFE9EEF8);
        e.setHintTextColor(0xFF5E6A7E);
        e.setSingleLine(true);
        return e;
    }

    private void doChangePassword(final String currentPw, final String newPw, final String confirmPw) {
        if (newPw.length() < 10) {
            toast("新主密码至少 10 个字符");
            return;
        }
        if (!newPw.equals(confirmPw)) {
            toast("两次输入的新密码不一致");
            return;
        }
        new Thread(() -> {
            try {
                byte[] data = app.store().readBytes();
                VaultFile.open(currentPw.toCharArray(), data); // verifies the old password
                VaultFile vf = VaultFile.create(newPw.toCharArray());
                app.onUnlocked(app.vault(), vf);
                app.store().save(vf, app.vault());
                if (app.biometricEnabled()) {
                    BiometricHelper.clear(this);
                    app.prefs().edit().putBoolean(VaultApp.K_BIOMETRIC, false).apply();
                }
                runOnUiThread(() -> {
                    build();
                    toast("主密码已修改，旧密码立即失效");
                });
            } catch (AesGcm.BadPasswordException e) {
                runOnUiThread(() -> toast("当前主密码错误"));
            } catch (Exception e) {
                runOnUiThread(() -> toast("修改失败：" + e.getMessage()));
            }
        }, "change-password").start();
    }

    private void exportBackup() {
        String name = "mixia-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date()) + ".mxvault";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(intent, REQ_EXPORT);
    }

    private void importBackup() {
        AppDialog.confirm(this, "从备份恢复",
                "导入会覆盖当前保险库中的全部记录。建议先导出一份当前备份。继续？",
                "选择备份文件", false, () -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    startActivityForResult(intent, REQ_IMPORT);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT) {
            writeBackup(uri);
        } else if (requestCode == REQ_IMPORT) {
            readBackup(uri);
        }
    }

    private void writeBackup(final Uri uri) {
        new Thread(() -> {
            try {
                byte[] bytes = app.store().readBytes();
                OutputStream out = getContentResolver().openOutputStream(uri);
                if (out == null) throw new IllegalStateException("无法写入");
                out.write(bytes);
                out.close();
                runOnUiThread(() -> toast("加密备份已导出"));
            } catch (Exception e) {
                runOnUiThread(() -> toast("导出失败：" + e.getMessage()));
            }
        }, "export").start();
    }

    private void readBackup(final Uri uri) {
        new Thread(() -> {
            try {
                InputStream in = getContentResolver().openInputStream(uri);
                if (in == null) throw new IllegalStateException("无法读取");
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) > 0) buffer.write(chunk, 0, n);
                in.close();
                final byte[] bytes = buffer.toByteArray();

                Vault backup;
                try {
                    VaultFile vf = VaultFile.fromKey(app.vaultFile().key(), bytes);
                    backup = app.store().load(vf, bytes);
                } catch (AesGcm.BadPasswordException e) {
                    runOnUiThread(() -> askBackupPassword(bytes));
                    return;
                }
                applyBackup(backup);
            } catch (Exception e) {
                runOnUiThread(() -> toast("导入失败：" + e.getMessage()));
            }
        }, "import").start();
    }

    private void askBackupPassword(final byte[] bytes) {
        final EditText input = passwordField("备份文件的主密码");
        LinearLayout wrap = new LinearLayout(this);
        int pad = Ui.dp(this, 20);
        wrap.setPadding(pad, Ui.dp(this, 8), pad, 0);
        wrap.addView(input);
        AppDialog.Builder builder = new AppDialog.Builder(this)
                .title("备份使用了另一个主密码")
                .message("请输入该备份创建时的主密码。")
                .content(wrap)
                .positive("解密", () -> new Thread(() -> {
                    try {
                        VaultFile vf = VaultFile.open(input.getText().toString().toCharArray(), bytes);
                        final Vault backup = app.store().load(vf, bytes);
                        runOnUiThread(() -> applyBackup(backup));
                    } catch (AesGcm.BadPasswordException e) {
                        runOnUiThread(() -> toast("密码错误，无法解密备份"));
                    } catch (Exception e) {
                        runOnUiThread(() -> toast("导入失败：" + e.getMessage()));
                    }
                }, "import-pw").start());
        builder.show();
    }

    private void applyBackup(Vault backup) {
        Vault current = app.vault();
        current.entries.clear();
        current.entries.addAll(backup.entries);
        current.wallets.clear();
        current.wallets.addAll(backup.wallets);
        app.saveAsync(() -> {
            toast("已恢复 " + backup.entries.size() + " 条记录、" + backup.wallets.size() + " 份助记词");
            build();
        });
    }

    private void wipeEverything() {
        AppDialog.confirm(this, "清空保险库",
                "所有密码、照片与助记词将被永久删除，且无法恢复。确定继续？", "我已备份，继续", true,
                () -> AppDialog.confirm(this, "最后确认", "真的要删除全部数据吗？", "永久删除", true, () -> {
                    app.store().wipe();
                    com.mixia.vault.data.PhotoStore.deleteAll(this);
                    BiometricHelper.clear(this);
                    app.prefs().edit().clear().apply();
                    app.lock();
                    Intent intent = new Intent(this, LockActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }));
    }

    /** Real installed version, read from the package manager. */
    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private void showAbout() {
        AppDialog.alert(this, "密匣 " + versionName(),
                "一个不联网的本地密码管理器。\n\n"
                        + "· 密码、账号、Wi-Fi、银行卡、服务器信息\n"
                        + "· 照片附件（同样加密存放）\n"
                        + "· 自定义分类与排序\n"
                        + "· BIP39 助记词生成、校验与本地派生\n"
                        + "· AES-256-GCM 全库加密\n"
                        + "· 无 INTERNET 权限，数据不离开手机");
    }

    private void showCrypto() {
        AppDialog.alert(this, "加密细节",
                "主密码 → PBKDF2-HMAC-SHA256（250,000 次）→ 256 位密钥\n"
                        + "文字数据 → AES-256-GCM（每次保存使用新的随机 IV）\n"
                        + "照片 → 同一密钥 AES-256-GCM 单独加密成文件\n"
                        + "助记词 → BIP39（SHA-256 校验和）+ BIP32 派生\n\n"
                        + "主密码不会以任何形式保存在设备上。");
    }
}



