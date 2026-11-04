package com.mixia.vault.util;

import android.content.Context;
import android.util.TypedValue;

/** Small UI helpers. */
public final class Ui {
    private static final int[] AVATAR_COLORS = {
            0xFF2C6E63, 0xFF3B5B8C, 0xFF7A4E8C, 0xFF8C5A32, 0xFF3F6B45, 0xFF7C4A4A, 0xFF4A5E73};

    private Ui() {
    }

    public static int dp(Context context, float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }

    public static int avatarColor(String seed) {
        int hash = seed == null ? 0 : seed.hashCode();
        return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
    }

    public static String initial(String text) {
        if (text == null || text.trim().isEmpty()) return "?";
        String t = text.trim();
        int cp = t.codePointAt(0);
        return new String(Character.toChars(cp)).toUpperCase();
    }
}
