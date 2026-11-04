package com.mixia.vault.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.mixia.vault.VaultApp;

/** Clipboard helper that can wipe the clipboard after a delay. */
public final class ClipboardHelper {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static String lastCopied;

    private ClipboardHelper() {
    }

    public static void copy(Context context, String label, String value) {
        if (value == null) value = "";
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;
        ClipData clip = ClipData.newPlainText(label, value);
        if (Build.VERSION.SDK_INT >= 28) {
            clip.getDescription().setExtras(new android.os.PersistableBundle());
        }
        cm.setPrimaryClip(clip);
        lastCopied = value;
        Toast.makeText(context, "已复制" + label, Toast.LENGTH_SHORT).show();
        if (VaultApp.get(context).clipboardClear()) {
            HANDLER.removeCallbacksAndMessages("clip");
            HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    clearIfUnchanged(context);
                }
            }, 45_000L);
        }
    }

    private static void clearIfUnchanged(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return;
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence text = clip.getItemAt(0).coerceToText(context);
        if (text != null && text.toString().equals(lastCopied)) {
            if (Build.VERSION.SDK_INT >= 28) {
                cm.clearPrimaryClip();
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""));
            }
            lastCopied = null;
        }
    }
}
