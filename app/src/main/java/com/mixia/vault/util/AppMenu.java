package com.mixia.vault.util;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mixia.vault.R;

/** Bottom sheet style menu that replaces the default PopupMenu. */
public final class AppMenu {

    public static final class Item {
        final int icon;
        final String title;
        final String subtitle;
        final boolean danger;
        final Runnable action;

        public Item(int icon, String title, String subtitle, Runnable action) {
            this(icon, title, subtitle, false, action);
        }

        public Item(int icon, String title, String subtitle, boolean danger, Runnable action) {
            this.icon = icon;
            this.title = title;
            this.subtitle = subtitle;
            this.danger = danger;
            this.action = action;
        }
    }

    private AppMenu() {
    }

    public static void show(final Activity activity, final Item... items) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.sheet_menu);

        LinearLayout container = dialog.findViewById(R.id.menuContainer);
        int gap = Ui.dp(activity, 10);
        for (final Item item : items) {
            View row = activity.getLayoutInflater().inflate(R.layout.item_menu, container, false);
            ImageView icon = row.findViewById(R.id.menuIcon);
            TextView title = row.findViewById(R.id.menuTitle);
            TextView subtitle = row.findViewById(R.id.menuSubtitle);
            icon.setImageResource(item.icon);
            icon.setColorFilter(item.danger ? 0xFFEF5B6B : 0xFF4EC9B0);
            title.setText(item.title);
            title.setTextColor(item.danger ? 0xFFEF5B6B : 0xFFE9EEF8);
            if (item.subtitle == null || item.subtitle.isEmpty()) {
                subtitle.setVisibility(View.GONE);
            } else {
                subtitle.setText(item.subtitle);
            }
            row.setClickable(true);
            if (item.danger) {
                row.setBackgroundResource(R.drawable.menu_row_danger_bg);
            }
            PressFeedback.attach(row);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                if (item.action != null) item.action.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = gap;
            container.addView(row, lp);
        }

        View cancel = dialog.findViewById(R.id.menuCancel);
        PressFeedback.attach(cancel);
        cancel.setOnClickListener(v -> dialog.dismiss());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.55f;
            params.gravity = Gravity.BOTTOM;
            window.setAttributes(params);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();

        // keep the sheet clear of the gesture bar
        View sheet = dialog.findViewById(R.id.sheetRoot);
        if (sheet != null && Build.VERSION.SDK_INT >= 30) {
            sheet.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.navigationBars()
                                | android.view.WindowInsets.Type.displayCutout());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                        v.getPaddingRight(), Ui.dp(activity, 14) + bars.bottom);
                return insets;
            });
            sheet.requestApplyInsets();
        }
    }
}
