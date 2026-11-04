package com.mixia.vault;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mixia.vault.core.AesGcm;
import com.mixia.vault.core.VaultFile;
import com.mixia.vault.model.Vault;

/** First run: create the master password. Afterwards: unlock. */
public class LockActivity extends BaseActivity {
    private VaultApp app;
    private EditText master1;
    private EditText master2;
    private EditText hintInput;
    private EditText masterInput;
    private TextView errorText;
    private TextView hintText;
    private ProgressBar progress;
    private Button btnUnlock;
    private Button btnFingerprint;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);
        app = VaultApp.get(this);

        master1 = findViewById(R.id.master1);
        master2 = findViewById(R.id.master2);
        hintInput = findViewById(R.id.hintInput);
        masterInput = findViewById(R.id.masterInput);
        errorText = findViewById(R.id.errorText);
        hintText = findViewById(R.id.hintText);
        progress = findViewById(R.id.progress);
        btnUnlock = findViewById(R.id.btnUnlock);
        btnFingerprint = findViewById(R.id.btnFingerprint);

        View createGroup = findViewById(R.id.createGroup);
        View unlockGroup = findViewById(R.id.unlockGroup);

        if (app.store().exists()) {
            createGroup.setVisibility(View.GONE);
            unlockGroup.setVisibility(View.VISIBLE);
            masterInput.requestFocus();
            btnUnlock.setOnClickListener(v -> doUnlock());
            masterInput.setOnEditorActionListener((v, actionId, event) -> {
                doUnlock();
                return true;
            });
            String hint = app.prefs().getString(VaultApp.K_HINT, "");
            if (!hint.isEmpty()) {
                hintText.setText("密码提示：" + hint);
                hintText.setVisibility(View.VISIBLE);
            }
            if (app.biometricEnabled() && com.mixia.vault.util.BiometricHelper.hasEnrollment(this)) {
                btnFingerprint.setVisibility(View.VISIBLE);
                btnFingerprint.setOnClickListener(v -> doBiometricUnlock());
            }
            long remain = app.lockoutRemainingMs();
            if (remain > 0) startLockoutCountdown(remain);
        } else {
            createGroup.setVisibility(View.VISIBLE);
            unlockGroup.setVisibility(View.GONE);
            master1.requestFocus();
            findViewById(R.id.btnCreate).setOnClickListener(v -> doCreate());
        }
    }

    private void doCreate() {
        if (busy) return;
        String p1 = master1.getText().toString();
        String p2 = master2.getText().toString();
        if (p1.length() < 10) {
            showError("主密码至少需要 10 个字符");
            return;
        }
        if (!p1.equals(p2)) {
            showError("两次输入的主密码不一致");
            return;
        }
        busy = true;
        showProgress(true);
        final String hint = hintInput.getText().toString().trim();
        new Thread(() -> {
            try {
                VaultFile vf = VaultFile.create(p1.toCharArray());
                Vault vault = new Vault();
                app.store().save(vf, vault);
                app.onUnlocked(vault, vf);
                app.prefs().edit().putString(VaultApp.K_HINT, hint).apply();
                handler.post(() -> {
                    showProgress(false);
                    busy = false;
                    startActivity(new Intent(this, VaultActivity.class));
                    finish();
                });
            } catch (Exception e) {
                handler.post(() -> {
                    showProgress(false);
                    busy = false;
                    showError("创建失败：" + e.getMessage());
                });
            }
        }, "create-vault").start();
    }

    private void doUnlock() {
        if (busy) return;
        long remain = app.lockoutRemainingMs();
        if (remain > 0) {
            showError("尝试次数过多，请 " + (remain / 1000) + " 秒后再试");
            return;
        }
        final String pw = masterInput.getText().toString();
        if (pw.isEmpty()) {
            showError("请输入主密码");
            return;
        }
        busy = true;
        showProgress(true);
        errorText.setVisibility(View.GONE);
        new Thread(() -> {
            try {
                app.unlockWith(pw.toCharArray());
                handler.post(() -> {
                    showProgress(false);
                    busy = false;
                    startActivity(new Intent(this, VaultActivity.class));
                    finish();
                });
            } catch (AesGcm.BadPasswordException e) {
                app.noteFailure();
                handler.post(() -> {
                    showProgress(false);
                    busy = false;
                    int fails = app.prefs().getInt(VaultApp.K_FAILS, 0);
                    long left = app.lockoutRemainingMs();
                    if (left > 0) {
                        showError("连续输错 5 次，已锁定 60 秒");
                        startLockoutCountdown(left);
                    } else {
                        showError("主密码错误，还可以尝试 " + (5 - fails) + " 次");
                    }
                    masterInput.setText("");
                });
            } catch (Exception e) {
                handler.post(() -> {
                    showProgress(false);
                    busy = false;
                    showError("解锁失败：" + e.getMessage());
                });
            }
        }, "unlock-vault").start();
    }

    private void doBiometricUnlock() {
        if (busy) return;
        busy = true;
        showProgress(true);
        new Thread(() -> {
            try {
                final byte[] data = app.store().readBytes();
                runOnUiThread(() -> com.mixia.vault.util.BiometricHelper.unlock(LockActivity.this,
                        new com.mixia.vault.util.BiometricHelper.Callback() {
                            @Override
                            public void onSuccess(byte[] vaultKey) {
                                try {
                                    VaultFile vf = VaultFile.fromKey(vaultKey, data);
                                    Vault vault = app.store().load(vf, data);
                                    app.onUnlocked(vault, vf);
                                    app.resetFailures();
                                    showProgress(false);
                                    busy = false;
                                    startActivity(new Intent(LockActivity.this, VaultActivity.class));
                                    finish();
                                } catch (Exception e) {
                                    showProgress(false);
                                    busy = false;
                                    showError("指纹数据已失效，请使用主密码");
                                    btnFingerprint.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onFailure(String message) {
                                showProgress(false);
                                busy = false;
                            }
                        }));
            } catch (Exception e) {
                showProgress(false);
                busy = false;
                showError("读取保险库失败");
            }
        }, "bio-unlock").start();
    }

    private void startLockoutCountdown(long initial) {
        btnUnlock.setEnabled(false);
        final long[] remain = {initial};
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                remain[0] -= 1000;
                if (remain[0] <= 0) {
                    btnUnlock.setEnabled(true);
                    btnUnlock.setText("解锁");
                    errorText.setVisibility(View.GONE);
                } else {
                    btnUnlock.setText("锁定中 " + (remain[0] / 1000) + "s");
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(tick, 1000);
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void showProgress(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        btnUnlock.setEnabled(!show);
        if (show) {
            btnUnlock.setText("处理中…");
        } else if (app.lockoutRemainingMs() <= 0) {
            btnUnlock.setText("解锁");
        }
    }
}
