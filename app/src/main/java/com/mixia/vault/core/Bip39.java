package com.mixia.vault.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/** BIP-0039 mnemonic generation, validation and seed derivation. */
public final class Bip39 {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static List<String> words;
    private static java.util.Map<String, Integer> index;

    private Bip39() {
    }

    public static synchronized void load(InputStream in) throws IOException {
        if (words != null) return;
        List<String> list = new ArrayList<>(2048);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) list.add(line);
            }
        }
        if (list.size() != 2048) throw new IOException("BIP39 wordlist must have 2048 words, got " + list.size());
        java.util.HashMap<String, Integer> map = new java.util.HashMap<>(4096);
        for (int i = 0; i < list.size(); i++) map.put(list.get(i), i);
        index = map;
        words = list;
    }

    public static synchronized boolean isLoaded() {
        return words != null;
    }

    public static synchronized List<String> wordlist() {
        return words;
    }

    public static String generate(int wordCount) {
        if (wordCount != 12 && wordCount != 24) throw new IllegalArgumentException("wordCount 12 or 24");
        int entropyBytes = wordCount == 12 ? 16 : 32;
        byte[] entropy = new byte[entropyBytes];
        RANDOM.nextBytes(entropy);
        return fromEntropy(entropy);
    }

    public static String fromEntropy(byte[] entropy) {
        requireLoaded();
        int entBits = entropy.length * 8;
        int csBits = entBits / 32;
        byte[] hash = Hashing.sha256(entropy);
        int totalBits = entBits + csBits;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalBits / 11; i++) {
            int idx = 0;
            for (int j = 0; j < 11; j++) {
                int bitPos = i * 11 + j;
                int bit = bitAt(entropy, hash, bitPos, entBits);
                idx = (idx << 1) | bit;
            }
            if (sb.length() > 0) sb.append(' ');
            sb.append(words.get(idx));
        }
        return sb.toString();
    }

    private static int bitAt(byte[] entropy, byte[] hash, int bitPos, int entBits) {
        if (bitPos < entBits) {
            int b = entropy[bitPos / 8] & 0xFF;
            return (b >>> (7 - (bitPos % 8))) & 1;
        }
        int p = bitPos - entBits;
        int b = hash[p / 8] & 0xFF;
        return (b >>> (7 - (p % 8))) & 1;
    }

    public static String normalize(String mnemonic) {
        if (mnemonic == null) return "";
        return mnemonic.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static boolean validate(String mnemonic) {
        requireLoaded();
        String norm = normalize(mnemonic);
        if (norm.isEmpty()) return false;
        String[] parts = norm.split(" ");
        int count = parts.length;
        if (count != 12 && count != 15 && count != 18 && count != 21 && count != 24) return false;
        int totalBits = count * 11;
        int csBits = totalBits / 33;
        int entBits = totalBits - csBits;
        byte[] entropy = new byte[entBits / 8];
        int cs = 0;
        for (int i = 0; i < totalBits; i++) {
            Integer idx = index.get(parts[i / 11]);
            if (idx == null) return false;
            int bit = (idx >>> (10 - (i % 11))) & 1;
            int pos = i;
            if (pos < entBits) {
                entropy[pos / 8] |= (byte) (bit << (7 - (pos % 8)));
            } else {
                cs = (cs << 1) | bit;
            }
        }
        byte[] hash = Hashing.sha256(entropy);
        int expected = 0;
        for (int i = 0; i < csBits; i++) {
            int bit = (hash[i / 8] >>> (7 - (i % 8))) & 1;
            expected = (expected << 1) | bit;
        }
        return expected == cs;
    }

    public static byte[] toSeed(String mnemonic, String passphrase) {
        String norm = normalize(mnemonic);
        String salt = "mnemonic" + (passphrase == null ? "" : passphrase);
        return Hashing.pbkdf2("HmacSHA512", Hashing.utf8(norm), Hashing.utf8(salt), 2048, 64);
    }

    public static byte[] entropyOf(String mnemonic) {
        requireLoaded();
        String norm = normalize(mnemonic);
        String[] parts = norm.split(" ");
        int totalBits = parts.length * 11;
        int entBits = totalBits - totalBits / 33;
        byte[] entropy = new byte[entBits / 8];
        for (int i = 0; i < entBits; i++) {
            int idx = index.get(parts[i / 11]);
            int bit = (idx >>> (10 - (i % 11))) & 1;
            entropy[i / 8] |= (byte) (bit << (7 - (i % 8)));
        }
        return entropy;
    }

    private static void requireLoaded() {
        if (words == null) throw new IllegalStateException("BIP39 wordlist not loaded");
    }
}
