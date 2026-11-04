package com.mixia.vault.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Hash / MAC / PBKDF2 helpers built only from the platform JCE. */
public final class Hashing {
    private Hashing() {
    }

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] sha256d(byte[] data) {
        return sha256(sha256(data));
    }

    public static byte[] sha512(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] hmacSha512(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key, "HmacSHA512"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Portable PBKDF2 (RFC 8018) over the given HMAC algorithm.
     * Implemented by hand so behaviour is identical on Android and on the JVM
     * and so that passwords are always encoded as UTF-8.
     */
    public static byte[] pbkdf2(String hmacAlgo, byte[] password, byte[] salt, int iterations, int keyLenBytes) {
        try {
            Mac mac = Mac.getInstance(hmacAlgo);
            mac.init(new SecretKeySpec(password, hmacAlgo));
            int hLen = mac.getMacLength();
            int blocks = (keyLenBytes + hLen - 1) / hLen;
            byte[] out = new byte[blocks * hLen];
            byte[] block = new byte[hLen];
            for (int i = 1; i <= blocks; i++) {
                mac.update(salt);
                mac.update(new byte[]{(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i});
                byte[] u = mac.doFinal();
                System.arraycopy(u, 0, block, 0, hLen);
                for (int j = 1; j < iterations; j++) {
                    u = mac.doFinal(u);
                    for (int k = 0; k < hLen; k++) block[k] ^= u[k];
                }
                System.arraycopy(block, 0, out, (i - 1) * hLen, hLen);
            }
            return out.length == keyLenBytes ? out : Arrays.copyOf(out, keyLenBytes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
