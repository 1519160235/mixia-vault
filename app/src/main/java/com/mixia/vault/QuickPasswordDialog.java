package com.mixia.vault;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mixia.vault.model.Entry;
import com.mixia.vault.util.AppDialog;
import com.mixia.vault.util.ClipboardHelper;
import com.mixia.vault.util.PressFeedback;
import com.mixia.vault.util.Ui;

/** Tap on a row: show only the password, nothing else. */
public final class QuickPasswordDialog {

    private QuickPasswordDialog() {
    }

    public static void show(final BaseActivity activity, final Entry entry) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_quick_password);

        TextView title = dialog.findViewById(R.id.quickTitle);
        TextView subtitle = dialog.findViewById(R.id.quickSubtitle);
        final TextView password = dialog.findViewById(R.id.quickPassword);
        final ImageButton eye = dialog.findViewById(R.id.quickEye);
        View copy = dialog.findViewById(R.id.quickCopy);
        View close = dialog.findViewById(R.id.quickClose);

        title.setText(entry.title);
        String sub = entry.username == null ? "" : entry.username.trim();
        if (sub.isEmpty()) sub = entry.category;
        subtitle.setText(sub);

        final String value = entry.password == null ? "" : entry.password;
        if (value.isEmpty()) {
            password.setText("（没有保存密码）");
            password.setTextSize(16);
            copy.setVisibility(View.GONE);
            eye.setVisibility(View.GONE);
        } else {
            password.setText(mask(value));
            final boolean[] shown = {false};
            eye.setOnClickListener(v -> {
                shown[0] = !shown[0];
                password.setText(shown[0] ? value : mask(value));
                eye.setImageResource(shown[0] ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            });
            copy.setOnClickListener(v -> {
                ClipboardHelper.copySensitive(activity, "密码", value);
                dialog.dismiss();
                // 复制后给出「立刻清除」的快捷入口
                Toast.makeText(activity,
                        "密码已复制，最多 15 秒后自动清除",
                        Toast.LENGTH_LONG).show();
            });
        }

        View clearClip = dialog.findViewById(R.id.quickClearClip);
        clearClip.setOnClickListener(v -> {
            ClipboardHelper.clearNow(activity);
            Toast.makeText(activity, "已清除系统剪贴板", Toast.LENGTH_SHORT).show();
        });
        PressFeedback.attach(clearClip);

        close.setOnClickListener(v -> dialog.dismiss());
        PressFeedback.attach(copy);
        PressFeedback.attach(close);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.6f;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            int width = Math.min((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f),
                    Ui.dp(activity, 420));
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private static String mask(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(value.length(), 28); i++) sb.append('•');
        return sb.toString();
    }
}
