package com.mixia.vault.util;

import android.app.Activity;
import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Optional fingerprint unlock.
 * The vault key is wrapped by an AES key that lives in the Android Keystore and can only be
 * used after a successful biometric authentication.
 */
public final class BiometricHelper {
    private static final String TAG = "BiometricHelper";
    private static final String ALIAS = "mixia_bio_wrap_v1";
    private static final String FILE = "bio.bin";

    private BiometricHelper() {
    }

    public interface Callback {
        void onSuccess(byte[] vaultKey);

        void onFailure(String message);
    }

    public static boolean isAvailable(Context context) {
        if (Build.VERSION.SDK_INT < 30) return false;
        try {
            BiometricManager bm = context.getSystemService(BiometricManager.class);
            return bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasEnrollment(Context context) {
        return new File(context.getFilesDir(), FILE).exists();
    }

    public static void clear(Context context) {
        //noinspection ResultOfMethodCallIgnored
        new File(context.getFilesDir(), FILE).delete();
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            return (SecretKey) ks.getKey(ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(false)
                .build());
        return generator.generateKey();
    }

    /** Wrap the vault key; requires a biometric prompt because the Keystore key is auth-bound. */
    public static void enable(final Activity activity, final byte[] vaultKey, final Callback callback) {
        try {
            final SecretKey key = getOrCreateKey();
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                    .setTitle("启用指纹解锁")
                    .setSubtitle("验证指纹以保存解锁密钥")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButton("取消", activity.getMainExecutor(),
                            (dialog, which) -> callback.onFailure("已取消"))
                    .build();

            prompt.authenticate(new BiometricPrompt.CryptoObject(cipher), new android.os.CancellationSignal(),
                    activity.getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            try {
                                Cipher used = result.getCryptoObject().getCipher();
                                byte[] iv = used.getIV();
                                byte[] ct = used.doFinal(vaultKey);
                                File f = new File(activity.getFilesDir(), FILE);
                                FileOutputStream out = new FileOutputStream(f);
                                out.write(iv);
                                out.write(ct);
                                out.close();
                                callback.onSuccess(vaultKey);
                            } catch (Exception e) {
                                Log.w(TAG, "enable failed", e);
                                callback.onFailure("保存失败：" + e.getMessage());
                            }
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            callback.onFailure(errString == null ? "指纹验证失败" : errString.toString());
                        }
                    });
        } catch (Throwable t) {
            Log.w(TAG, "enable unavailable", t);
            callback.onFailure("此设备暂不支持指纹解锁");
        }
    }

    /** Unwrap the vault key using biometric authentication. */
    public static void unlock(final Activity activity, final Callback callback) {
        try {
            File f = new File(activity.getFilesDir(), FILE);
            if (!f.exists()) {
                callback.onFailure("尚未启用指纹解锁");
                return;
            }
            byte[] blob = new byte[(int) f.length()];
            FileInputStream in = new FileInputStream(f);
            int off = 0;
            while (off < blob.length) {
                int n = in.read(blob, off, blob.length - off);
                if (n < 0) break;
                off += n;
            }
            in.close();
            if (blob.length < 28) {
                callback.onFailure("指纹数据不完整");
                return;
            }
            byte[] iv = Arrays.copyOfRange(blob, 0, 12);
            final byte[] ct = Arrays.copyOfRange(blob, 12, blob.length);

            SecretKey key = getOrCreateKey();
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

            BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                    .setTitle("解锁密匣")
                    .setSubtitle("验证指纹")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButton("使用主密码", activity.getMainExecutor(),
                            (dialog, which) -> callback.onFailure("已取消"))
                    .build();

            prompt.authenticate(new BiometricPrompt.CryptoObject(cipher), new android.os.CancellationSignal(),
                    activity.getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            try {
                                Cipher used = result.getCryptoObject().getCipher();
                                byte[] plain = used.doFinal(ct);
                                callback.onSuccess(plain);
                            } catch (Exception e) {
                                Log.w(TAG, "unlock failed", e);
                                callback.onFailure("指纹解锁失败，请使用主密码");
                            }
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            callback.onFailure(errString == null ? "指纹验证失败" : errString.toString());
                        }
                    });
        } catch (Throwable t) {
            Log.w(TAG, "unlock unavailable", t);
            callback.onFailure("指纹解锁不可用，请使用主密码");
        }
    }

    /** Helper for tests/manual reset. */
    public static void resetKey() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS);
        } catch (Exception ignored) {
        }
    }

    static {
        // touch SecureRandom so the class is not stripped accidentally
        new SecureRandom().nextInt();
    }
}
