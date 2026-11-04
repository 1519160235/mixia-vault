package com.mixia.vault.util;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.widget.Toast;

import com.mixia.vault.VaultApp;

/**
 * Clipboard helper.
 *
 * <p><b>安全边界说明：</b>剪贴板是系统级共享通道。复制的一瞬间，当前聚焦的输入法就能读到内容，
 * 并可能写入它自己的剪贴板历史——那是别的应用的私有数据，本应用没有任何 API 能删除。
 * 因此这里能做的是：标记为「敏感内容」（Android 13+ 系统浮层不预览、规范实现输入法会跳过）、
 * 以及按用户设置的时间自动清空系统剪贴板。</p>
 */
public final class ClipboardHelper {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static String lastCopied;

    private ClipboardHelper() {
    }

    /** 普通文本（账号、网址等）。 */
    public static void copy(Context context, String label, String value) {
        copyInternal(context, label, value, false);
    }

    /**
     * 敏感文本（密码）。带 EXTRA_IS_SENSITIVE 标记，缩短清除时间，
     * 并在第一次使用时提示输入法剪贴板的固有风险。
     */
    public static void copySensitive(Context context, String label, String value) {
        copyInternal(context, label, value, true);
    }

    private static void copyInternal(Context context, String label, String value, boolean sensitive) {
        if (value == null) value = "";
        if (value.isEmpty()) return;

        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;

        ClipData clip = ClipData.newPlainText(label, value);
        ClipDescription description = clip.getDescription();
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+：告诉系统这是敏感内容，系统剪贴板浮层不显示预览
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
            description.setExtras(extras);
        } else if (Build.VERSION.SDK_INT >= 28) {
            description.setExtras(new PersistableBundle());
        }
        cm.setPrimaryClip(clip);
        lastCopied = value;

        Toast.makeText(context, sensitive ? "已复制密码，用完请及时清除剪贴板" : "已复制" + label,
                Toast.LENGTH_SHORT).show();

        int seconds = VaultApp.get(context).clipboardClearSeconds();
        if (sensitive) {
            // 密码使用更短的窗口
            seconds = Math.min(seconds <= 0 ? 15 : seconds, 15);
        }
        if (seconds > 0) {
            HANDLER.removeCallbacksAndMessages("clip");
            HANDLER.postDelayed(() -> clearIfUnchanged(context), seconds * 1000L);
        }
    }

    /** 立即清空（设置页的「立刻清除剪贴板」用）。 */
    public static void clearNow(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return;
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                cm.clearPrimaryClip();
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""));
            }
            lastCopied = null;
        } catch (Exception ignored) {
        }
    }

    private static void clearIfUnchanged(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return;
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence text = clip.getItemAt(0).coerceToText(context);
        if (text != null && text.toString().equals(lastCopied)) {
            clearNow(context);
        }
    }
}
