package com.mixia.vault.util;

import java.security.SecureRandom;
import java.util.Locale;

/** Password generation and strength estimation. */
public final class PasswordTools {
    public static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DIGITS = "0123456789";
    public static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/";
    private static final String AMBIGUOUS = "Il1O0o";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordTools() {
    }

    public static String generate(int length, boolean upper, boolean lower, boolean digits, boolean symbols,
                                  boolean avoidAmbiguous) {
        StringBuilder pool = new StringBuilder();
        if (lower) pool.append(LOWER);
        if (upper) pool.append(UPPER);
        if (digits) pool.append(DIGITS);
        if (symbols) pool.append(SYMBOLS);
        if (pool.length() == 0) pool.append(LOWER);
        String chars = pool.toString();
        if (avoidAmbiguous) {
            StringBuilder filtered = new StringBuilder();
            for (int i = 0; i < chars.length(); i++) {
                if (AMBIGUOUS.indexOf(chars.charAt(i)) < 0) filtered.append(chars.charAt(i));
            }
            if (filtered.length() > 0) chars = filtered.toString();
        }
        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) out.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return out.toString();
    }

    /** Estimated entropy in bits (rough but useful). */
    public static double entropyBits(String password) {
        if (password == null || password.isEmpty()) return 0;
        int pool = 0;
        boolean lower = false, upper = false, digit = false, symbol = false, other = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c >= 'a' && c <= 'z') lower = true;
            else if (c >= 'A' && c <= 'Z') upper = true;
            else if (c >= '0' && c <= '9') digit = true;
            else if (c < 128) symbol = true;
            else other = true;
        }
        if (lower) pool += 26;
        if (upper) pool += 26;
        if (digit) pool += 10;
        if (symbol) pool += 32;
        if (other) pool += 100;
        if (pool == 0) return 0;
        double bits = password.length() * (Math.log(pool) / Math.log(2));
        // penalise repetition a little
        int unique = (int) password.chars().distinct().count();
        if (unique < password.length()) bits *= 0.85;
        return bits;
    }

    public static int strengthLevel(String password) {
        double bits = entropyBits(password);
        if (bits < 40) return 0;
        if (bits < 60) return 1;
        if (bits < 80) return 2;
        return 3;
    }

    public static String strengthLabel(String password) {
        if (password == null || password.isEmpty()) return "未填写";
        switch (strengthLevel(password)) {
            case 0:
                return "弱";
            case 1:
                return "一般";
            case 2:
                return "强";
            default:
                return "很强";
        }
    }

    public static int strengthColor(String password) {
        switch (strengthLevel(password)) {
            case 0:
                return 0xFFEF5B6B;
            case 1:
                return 0xFFF0B429;
            case 2:
                return 0xFF57C785;
            default:
                return 0xFF4EC9B0;
        }
    }

    public static String entropyText(String password) {
        return String.format(Locale.ROOT, "约 %.0f bit", entropyBits(password));
    }
}
