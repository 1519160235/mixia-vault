package com.mixia.vault;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.mixia.vault.core.AesGcm;
import com.mixia.vault.core.Bip39;
import com.mixia.vault.core.VaultFile;
import com.mixia.vault.data.VaultStore;
import com.mixia.vault.model.Vault;

import java.io.IOException;

/** Application singleton: holds the decrypted vault in memory and drives auto-lock. */
public class VaultApp extends Application implements Application.ActivityLifecycleCallbacks {
    public static final String PREFS = "mixia";
    public static final String K_AUTOLOCK = "autolock_seconds";
    public static final String K_CLIPBOARD = "clipboard_clear";
    public static final String K_BIOMETRIC = "biometric_enabled";
    public static final String K_HINT = "password_hint";
    public static final String K_FAILS = "failed_attempts";
    public static final String K_LOCKOUT = "lockout_until";

    private static VaultApp instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable lockRunnable;
    private int started = 0;

    private Vault vault;
    private VaultFile vaultFile;
    private boolean unlocked;

    public static VaultApp get(Context context) {
        return (VaultApp) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        registerActivityLifecycleCallbacks(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bip39.load(getAssets().open("bip39-english.txt"));
                } catch (IOException ignored) {
                }
            }
        }, "bip39-loader").start();
    }

    public static VaultApp get() {
        return instance;
    }

    public SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    public int autolockSeconds() {
        return prefs().getInt(K_AUTOLOCK, 60);
    }

    public boolean clipboardClear() {
        return prefs().getBoolean(K_CLIPBOARD, true);
    }

    public boolean biometricEnabled() {
        return prefs().getBoolean(K_BIOMETRIC, false);
    }

    public VaultStore store() {
        return new VaultStore(this);
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public Vault vault() {
        return vault;
    }

    public VaultFile vaultFile() {
        return vaultFile;
    }

    public void onUnlocked(Vault v, VaultFile vf) {
        this.vault = v;
        this.vaultFile = vf;
        this.unlocked = true;
    }

    public void lock() {
        vault = null;
        vaultFile = null;
        unlocked = false;
    }

    public void saveAsync(final Runnable after) {
        final Vault v = vault;
        final VaultFile vf = vaultFile;
        if (v == null || vf == null) return;
        final VaultStore store = store();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Exception error = null;
                try {
                    store.save(vf, v);
                } catch (Exception e) {
                    error = e;
                }
                final Exception finalError = error;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (after != null) after.run();
                        if (finalError != null) {
                            android.widget.Toast.makeText(VaultApp.this, "保存失败：" + finalError.getMessage(),
                                    android.widget.Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }, "vault-save").start();
    }

    /** Verify a candidate master password against the file on disk. */
    public VaultFile unlockWith(char[] password) throws AesGcm.BadPasswordException, IOException {
        VaultStore store = store();
        byte[] data = store.readBytes();
        VaultFile vf = VaultFile.open(password, data);
        Vault v = store.load(vf, data);
        onUnlocked(v, vf);
        resetFailures();
        return vf;
    }

    public void noteFailure() {
        SharedPreferences p = prefs();
        int fails = p.getInt(K_FAILS, 0) + 1;
        long lockout = 0;
        if (fails >= 5) {
            lockout = System.currentTimeMillis() + 60_000L;
            fails = 0;
        }
        p.edit().putInt(K_FAILS, fails).putLong(K_LOCKOUT, lockout).apply();
    }

    public void resetFailures() {
        prefs().edit().putInt(K_FAILS, 0).putLong(K_LOCKOUT, 0).apply();
    }

    public long lockoutRemainingMs() {
        long until = prefs().getLong(K_LOCKOUT, 0);
        return Math.max(0, until - System.currentTimeMillis());
    }

    // ---- auto lock ----

    @Override
    public void onActivityStarted(Activity activity) {
        started++;
        if (lockRunnable != null) {
            handler.removeCallbacks(lockRunnable);
            lockRunnable = null;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        started--;
        if (started <= 0 && unlocked) {
            started = 0;
            scheduleLock();
        }
    }

    private void scheduleLock() {
        if (lockRunnable != null) handler.removeCallbacks(lockRunnable);
        int secs = autolockSeconds();
        lockRunnable = new Runnable() {
            @Override
            public void run() {
                if (started <= 0 && unlocked) {
                    lock();
                    Intent intent = new Intent(VaultApp.this, LockActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }
        };
        if (secs <= 0) {
            handler.post(lockRunnable);
        } else {
            handler.postDelayed(lockRunnable, secs * 1000L);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
