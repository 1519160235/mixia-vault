package com.mixia.vault.core;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM with a random 96-bit IV prepended to the ciphertext. */
public final class AesGcm {
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AesGcm() {
    }

    public static byte[] encrypt(byte[] key32, byte[] plaintext, byte[] aad) {
        try {
            byte[] iv = new byte[IV_LEN];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key32, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            if (aad != null) cipher.updateAAD(aad);
            byte[] ct = cipher.doFinal(plaintext);
            byte[] out = new byte[IV_LEN + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_LEN);
            System.arraycopy(ct, 0, out, IV_LEN, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] decrypt(byte[] key32, byte[] blob, byte[] aad) throws BadPasswordException {
        if (blob == null || blob.length <= IV_LEN) throw new BadPasswordException();
        try {
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(blob, 0, iv, 0, IV_LEN);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key32, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            if (aad != null) cipher.updateAAD(aad);
            return cipher.doFinal(blob, IV_LEN, blob.length - IV_LEN);
        } catch (Exception e) {
            throw new BadPasswordException();
        }
    }

    public static class BadPasswordException extends Exception {
        public BadPasswordException() {
            super("bad password or corrupted data");
        }
    }
}
