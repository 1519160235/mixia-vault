package com.mixia.vault.util;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mixia.vault.R;

/**
 * The app's own dialog. Replaces android.app.AlertDialog so every sheet matches the dark theme
 * instead of falling back to the platform's Holo/Material light styling.
 */
public final class AppDialog {

    public interface OnText {
        void onText(String value);
    }

    public interface OnIndex {
        void onIndex(int index);
    }

    private AppDialog() {
    }

    public static void alert(Activity activity, String title, String message) {
        new Builder(activity).title(title).message(message).positive("知道了", null).show();
    }

    public static void confirm(Activity activity, String title, String message, String okText,
                               boolean danger, Runnable onOk) {
        new Builder(activity).title(title).message(message).positive(okText, onOk).danger(danger).show();
    }

    public static void input(Activity activity, String title, String hint, String initial,
                             boolean password, final OnText onOk) {
        EditText input = new EditText(activity);
        input.setBackgroundResource(R.drawable.input_bg);
        int p = Ui.dp(activity, 14);
        input.setPadding(p, p, p, p);
        input.setTextColor(0xFFE9EEF8);
        input.setHintTextColor(0xFF5E6A7E);
        input.setTextSize(16);
        input.setHint(hint);
        input.setSingleLine(true);
        if (initial != null) input.setText(initial);
        if (password) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 52));
        lp.topMargin = Ui.dp(activity, 4);
        input.setLayoutParams(lp);
        new Builder(activity).title(title).content(input).positive("保存", () ->
                onOk.onText(input.getText().toString())).show();
    }

    public static void singleChoice(Activity activity, String title, String[] items, int checked,
                                    final OnIndex onPick) {
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        final Dialog[] holder = new Dialog[1];
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.choice_row_bg);
            row.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 14),
                    Ui.dp(activity, 14), Ui.dp(activity, 14));
            row.setClickable(true);
            row.setSelected(i == checked);

            TextView label = new TextView(activity);
            label.setText(items[i]);
            label.setTextSize(15);
            label.setTextColor(0xFFE9EEF8);
            row.addView(label, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            ImageView check = new ImageView(activity);
            check.setImageResource(R.drawable.ic_check);
            check.setColorFilter(0xFF4EC9B0);
            check.setVisibility(i == checked ? View.VISIBLE : View.INVISIBLE);
            row.addView(check, new LinearLayout.LayoutParams(Ui.dp(activity, 20), Ui.dp(activity, 20)));

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = Ui.dp(activity, 6);
            list.addView(row, rlp);

            row.setOnClickListener(v -> {
                onPick.onIndex(index);
                if (holder[0] != null) holder[0].dismiss();
            });
        }
        Dialog dialog = new Builder(activity).title(title).content(list).hideNegative().show();
        holder[0] = dialog;
    }

    public static final class Builder {
        private final Activity activity;
        private String title;
        private String message;
        private String positiveText = "确定";
        private String negativeText = "取消";
        private boolean showNegative = true;
        private boolean danger;
        private boolean cancelable = true;
        private View content;
        private Runnable onPositive;
        private Runnable onNegative;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder title(String text) {
            this.title = text;
            return this;
        }

        public Builder message(String text) {
            this.message = text;
            return this;
        }

        public Builder content(View view) {
            this.content = view;
            return this;
        }

        public Builder positive(String text, Runnable action) {
            this.positiveText = text;
            this.onPositive = action;
            return this;
        }

        public Builder negative(String text, Runnable action) {
            this.negativeText = text;
            this.onNegative = action;
            return this;
        }

        public Builder hideNegative() {
            this.showNegative = false;
            return this;
        }

        public Builder danger(boolean value) {
            this.danger = value;
            return this;
        }

        public Builder cancelable(boolean value) {
            this.cancelable = value;
            return this;
        }

        public Dialog show() {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return null;
            }
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_app);
            dialog.setCanceledOnTouchOutside(cancelable);
            dialog.setCancelable(cancelable);

            TextView titleView = dialog.findViewById(R.id.dialogTitle);
            TextView messageView = dialog.findViewById(R.id.dialogMessage);
            LinearLayout contentBox = dialog.findViewById(R.id.dialogContent);
            TextView primary = dialog.findViewById(R.id.btnDialogPrimary);
            TextView negative = dialog.findViewById(R.id.btnDialogNegative);
            View buttons = dialog.findViewById(R.id.dialogButtons);

            if (title == null || title.isEmpty()) {
                titleView.setVisibility(View.GONE);
            } else {
                titleView.setText(title);
            }
            if (message == null || message.isEmpty()) {
                messageView.setVisibility(View.GONE);
            } else {
                messageView.setText(message);
            }
            if (content != null) {
                contentBox.addView(content);
                contentBox.setVisibility(View.VISIBLE);
            } else {
                contentBox.setVisibility(View.GONE);
            }

            primary.setText(positiveText);
            if (danger) {
                primary.setBackgroundResource(R.drawable.btn_danger_solid);
                primary.setTextColor(0xFFFFFFFF);
            }
            primary.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });

            if (showNegative) {
                negative.setText(negativeText);
                negative.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (onNegative != null) onNegative.run();
                });
                LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,
                        Ui.dp(activity, 48), 1f);
                nlp.setMarginEnd(Ui.dp(activity, 10));
                negative.setLayoutParams(nlp);
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0,
                        Ui.dp(activity, 48), 1f);
                primary.setLayoutParams(plp);
            } else {
                negative.setVisibility(View.GONE);
                primary.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 48)));
            }
            if (buttons != null) buttons.setVisibility(View.VISIBLE);
            PressFeedback.attach(primary);
            PressFeedback.attach(negative);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowManager.LayoutParams params = window.getAttributes();
                params.dimAmount = 0.62f;
                params.gravity = Gravity.CENTER;
                window.setAttributes(params);
                int width = Math.min((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f),
                        Ui.dp(activity, 420));
                window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.show();
            return dialog;
        }
    }
}
