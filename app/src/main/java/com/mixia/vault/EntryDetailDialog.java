package com.mixia.vault;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mixia.vault.data.PhotoStore;
import com.mixia.vault.model.Entry;
import com.mixia.vault.util.AppDialog;
import com.mixia.vault.util.ClipboardHelper;
import com.mixia.vault.util.PasswordTools;
import com.mixia.vault.util.PhotoViewer;
import com.mixia.vault.util.PressFeedback;
import com.mixia.vault.util.Ui;

/** Read-only detail sheet, drawn with the app's own dark styling. */
public final class EntryDetailDialog {

    private EntryDetailDialog() {
    }

    public static void show(final BaseActivity activity, final Entry entry, final Runnable onChanged) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundResource(R.drawable.dialog_bg);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(activity, 20);
        root.setPadding(p, p, p, Ui.dp(activity, 16));
        scroll.addView(root);

        TextView title = new TextView(activity);
        title.setText(entry.title);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFFE9EEF8);
        root.addView(title);

        TextView meta = new TextView(activity);
        meta.setText(entry.category + (entry.favorite ? " · 已收藏" : "")
                + (entry.photos.isEmpty() ? "" : " · " + entry.photos.size() + " 张照片"));
        meta.setTextSize(12);
        meta.setTextColor(0xFF8C97AC);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = Ui.dp(activity, 4);
        root.addView(meta, mlp);

        addField(activity, root, "账号", entry.username, true, false);
        addField(activity, root, "密码", entry.password, true, true);
        addField(activity, root, "网址", entry.url, true, false);
        addField(activity, root, "标签", entry.tags, false, false);
        addField(activity, root, "备注", entry.notes, true, false);

        if (!entry.password.isEmpty()) {
            TextView strength = new TextView(activity);
            strength.setText("强度：" + PasswordTools.strengthLabel(entry.password) + " · "
                    + PasswordTools.entropyText(entry.password));
            strength.setTextSize(12);
            strength.setTextColor(PasswordTools.strengthColor(entry.password));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = Ui.dp(activity, 12);
            root.addView(strength, slp);
        }

        if (!entry.photos.isEmpty()) {
            addPhotos(activity, root, entry);
        }

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(activity, 22);
        root.addView(buttons, blp);

        TextView edit = new TextView(activity);
        edit.setBackgroundResource(R.drawable.btn_ghost);
        edit.setText("编辑");
        edit.setGravity(Gravity.CENTER);
        edit.setTextColor(0xFFE9EEF8);
        edit.setTextSize(15);
        edit.setClickable(true);
        edit.setOnClickListener(v -> {
            EntryEditActivity.open(activity, entry.id);
        });
        buttons.addView(edit, new LinearLayout.LayoutParams(0, Ui.dp(activity, 48), 1f));

        TextView delete = new TextView(activity);
        delete.setBackgroundResource(R.drawable.btn_danger_solid);
        delete.setText("删除");
        delete.setGravity(Gravity.CENTER);
        delete.setTextColor(0xFFFFFFFF);
        delete.setTextSize(15);
        delete.setClickable(true);
        PressFeedback.attach(delete);
        PressFeedback.attach(edit);
        delete.setOnClickListener(v -> AppDialog.confirm(activity, "删除记录",
                "确定删除「" + entry.title + "」？照片也会一起删除。", "删除", true, () -> {
                    for (String path : entry.photos) PhotoStore.delete(path);
                    VaultApp app = VaultApp.get(activity);
                    app.vault().removeEntry(entry.id);
                    app.saveAsync(() -> {
                        if (onChanged != null) onChanged.run();
                    });
                    activity.toast("已删除");
                }));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, Ui.dp(activity, 48), 1f);
        dlp.setMarginStart(Ui.dp(activity, 10));
        buttons.addView(delete, dlp);

        AppDialog.Builder builder = new AppDialog.Builder(activity)
                .title(null).message(null)
                .content(scroll)
                .hideNegative()
                .positive("关闭", null);
        builder.show();
    }

    private static void addPhotos(final BaseActivity activity, LinearLayout root, final Entry entry) {
        TextView label = new TextView(activity);
        label.setText("照片");
        label.setTextSize(12);
        label.setTextColor(0xFF8C97AC);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(activity, 16);
        root.addView(label, lp);

        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(row);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(activity, 6);
        root.addView(scroll, slp);

        int size = Ui.dp(activity, 88);
        for (int i = 0; i < entry.photos.size(); i++) {
            final int index = i;
            final String path = entry.photos.get(i);
            FrameLayout wrap = new FrameLayout(activity);
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(size, size);
            wlp.setMarginEnd(Ui.dp(activity, 10));
            wrap.setLayoutParams(wlp);
            final ImageView thumb = new ImageView(activity);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundResource(R.drawable.photo_thumb_bg);
            wrap.addView(thumb, new FrameLayout.LayoutParams(size, size));
            wrap.setClickable(true);
            wrap.setOnClickListener(v -> PhotoViewer.show(activity, path, index, entry.photos.size()));
            row.addView(wrap);

            final byte[] key = VaultApp.get(activity).vaultFile() == null
                    ? null : VaultApp.get(activity).vaultFile().key();
            if (key != null) {
                new Thread(() -> {
                    final Bitmap bitmap = PhotoStore.thumbnail(path, key, 320);
                    activity.runOnUiThread(() -> {
                        if (bitmap != null) thumb.setImageBitmap(bitmap);
                    });
                }, "thumb").start();
            }
        }
    }

    private static void addField(final android.content.Context ctx, LinearLayout root, String label,
                                 String value, boolean copyable, boolean secret) {
        final String finalValue = value == null ? "" : value;
        if (finalValue.isEmpty() && !label.equals("密码")) return;

        TextView labelView = new TextView(ctx);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(0xFF8C97AC);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(ctx, 16);
        root.addView(labelView, lp);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.input_bg);
        row.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 4), Ui.dp(ctx, 4), Ui.dp(ctx, 4));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(ctx, 6);
        root.addView(row, rlp);

        final TextView valueView = new TextView(ctx);
        valueView.setTextSize(14);
        valueView.setTextColor(0xFFE9EEF8);
        valueView.setTextIsSelectable(true);
        valueView.setPadding(Ui.dp(ctx, 2), Ui.dp(ctx, 8), Ui.dp(ctx, 2), Ui.dp(ctx, 8));
        valueView.setText(secret ? mask(finalValue) : (finalValue.isEmpty() ? "—" : finalValue));
        row.addView(valueView, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (secret && !finalValue.isEmpty()) {
            final ImageButton eye = new ImageButton(ctx);
            eye.setBackgroundResource(android.R.color.transparent);
            eye.setImageResource(R.drawable.ic_eye);
            eye.setColorFilter(0xFF8C97AC);
            eye.setContentDescription("显示密码");
            final boolean[] visible = {false};
            eye.setOnClickListener(v -> {
                visible[0] = !visible[0];
                valueView.setText(visible[0] ? finalValue : mask(finalValue));
                eye.setImageResource(visible[0] ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            });
            row.addView(eye, new LinearLayout.LayoutParams(Ui.dp(ctx, 40), Ui.dp(ctx, 40)));
        }

        if (copyable && !finalValue.isEmpty()) {
            final ImageButton copy = new ImageButton(ctx);
            copy.setBackgroundResource(android.R.color.transparent);
            copy.setImageResource(R.drawable.ic_copy);
            copy.setColorFilter(0xFF4EC9B0);
            copy.setContentDescription("复制");
            copy.setOnClickListener(v -> ClipboardHelper.copy(ctx, label, finalValue));
            row.addView(copy, new LinearLayout.LayoutParams(Ui.dp(ctx, 40), Ui.dp(ctx, 40)));
        }
    }

    private static String mask(String value) {
        if (value.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(value.length(), 24); i++) sb.append('•');
        return sb.toString();
    }
}
