package com.mixia.vault.util;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Small press feedback (scale + fade) so every tappable element reacts immediately. */
public final class PressFeedback {

    private PressFeedback() {
    }

    public static void attach(final View view) {
        if (view == null) return;
        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().cancel();
                        v.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.85f)
                                .setDuration(70).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().cancel();
                        v.animate().scaleX(1f).scaleY(1f).alpha(1f)
                                .setDuration(140).setInterpolator(new DecelerateInterpolator()).start();
                        break;
                    default:
                        break;
                }
                return false; // do not consume: click still fires
            }
        });
    }

    /** Attaches feedback to every Button / ImageButton under root. */
    public static void attachAll(View root) {
        if (root == null) return;
        if (root instanceof android.widget.Button || root instanceof android.widget.ImageButton) {
            attach(root);
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                attachAll(group.getChildAt(i));
            }
        }
    }
}
