package com.mixia.vault;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Toast;

/** Common activity behaviour: screenshot protection, edge-to-edge insets and session guard. */
public class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        window.setBackgroundDrawableResource(R.drawable.bg_app);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            window.setNavigationBarContrastEnforced(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attrs);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        android.view.View root = ((android.view.ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        applyInsets(root);
        com.mixia.vault.util.PressFeedback.attachAll(root);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyInsets(view);
        com.mixia.vault.util.PressFeedback.attachAll(view);
    }

    /** Adds status-bar / cutout / navigation-bar padding to the page root. */
    private void applyInsets(final View root) {
        if (root == null || root.getTag(R.id.inset_tag) != null) return;
        root.setTag(R.id.inset_tag, Boolean.TRUE);
        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = 0, top = 0, right = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(baseLeft + left, baseTop + top, baseRight + right, baseBottom + bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    @Override
    protected void onStart() {
        super.onStart();
        VaultApp app = VaultApp.get(this);
        if (!(this instanceof LockActivity)) {
            // 回到前台：按「离开时长」判断是否需要重新解锁
            if (!app.isUnlocked() || app.shouldLockOnReturn()) {
                if (app.isUnlocked()) app.lock();
                goToLockScreen();
                return;
            }
            app.markForeground();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VaultApp app = VaultApp.get(this);
        if (!(this instanceof LockActivity) && !app.isUnlocked()) {
            goToLockScreen();
        }
    }

    private void goToLockScreen() {
        Intent intent = new Intent(this, LockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
