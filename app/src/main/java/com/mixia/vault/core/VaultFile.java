package com.mixia.vault.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * On-disk vault container.
 * Layout: "MXV1" | version(1) | iterations(4) | salt(16) | aes-gcm(key=PBKDF2(pw,salt), aad=header)
 */
public final class VaultFile {
    private static final byte[] MAGIC = {'M', 'X', 'V', '1'};
    private static final byte VERSION = 1;
    public static final int DEFAULT_ITERATIONS = 250_000;
    private static final int SALT_LEN = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] salt;
    private final int iterations;
    private final byte[] key;

    private VaultFile(byte[] salt, int iterations, byte[] key) {
        this.salt = salt;
        this.iterations = iterations;
        this.key = key;
    }

    public static VaultFile create(char[] password) {
        byte[] salt = new byte[SALT_LEN];
        RANDOM.nextBytes(salt);
        byte[] key = deriveKey(password, salt, DEFAULT_ITERATIONS);
        return new VaultFile(salt, DEFAULT_ITERATIONS, key);
    }

    public static VaultFile open(char[] password, byte[] data) throws AesGcm.BadPasswordException {
        if (data == null || data.length < MAGIC.length + 1 + 4 + SALT_LEN + 12) {
            throw new AesGcm.BadPasswordException();
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) throw new AesGcm.BadPasswordException();
        }
        if (data[MAGIC.length] != VERSION) throw new AesGcm.BadPasswordException();
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.position(MAGIC.length + 1);
        int iterations = buf.getInt();
        if (iterations < 10_000 || iterations > 10_000_000) throw new AesGcm.BadPasswordException();
        byte[] salt = new byte[SALT_LEN];
        buf.get(salt);
        byte[] key = deriveKey(password, salt, iterations);
        // verify the key by decrypting the payload
        AesGcm.decrypt(key, Arrays.copyOfRange(data, buf.position(), data.length), header(iterations, salt));
        return new VaultFile(salt, iterations, key);
    }

    public static VaultFile fromKey(byte[] key32, byte[] salt, int iterations) {
        return new VaultFile(salt, iterations, key32.clone());
    }

    /** Rebuild the container handle from a raw key plus the header of an existing file. */
    public static VaultFile fromKey(byte[] key32, byte[] data) throws AesGcm.BadPasswordException {
        if (data == null || data.length < MAGIC.length + 1 + 4 + SALT_LEN + 12) {
            throw new AesGcm.BadPasswordException();
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) throw new AesGcm.BadPasswordException();
        }
        if (data[MAGIC.length] != VERSION) throw new AesGcm.BadPasswordException();
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.position(MAGIC.length + 1);
        int iterations = buf.getInt();
        byte[] salt = new byte[SALT_LEN];
        buf.get(salt);
        VaultFile vf = new VaultFile(salt, iterations, key32.clone());
        vf.openPayload(data);
        return vf;
    }

    public byte[] seal(byte[] plaintext) {
        byte[] payload = AesGcm.encrypt(key, plaintext, header(iterations, salt));
        byte[] out = new byte[MAGIC.length + 1 + 4 + SALT_LEN + payload.length];
        ByteBuffer buf = ByteBuffer.wrap(out);
        buf.put(MAGIC);
        buf.put(VERSION);
        buf.putInt(iterations);
        buf.put(salt);
        buf.put(payload);
        return out;
    }

    public byte[] openPayload(byte[] data) throws AesGcm.BadPasswordException {
        return AesGcm.decrypt(key, Arrays.copyOfRange(data, MAGIC.length + 1 + 4 + SALT_LEN, data.length),
                header(iterations, salt));
    }

    public byte[] key() {
        return key.clone();
    }

    public byte[] salt() {
        return salt.clone();
    }

    public int iterations() {
        return iterations;
    }

    private static byte[] header(int iterations, byte[] salt) {
        return ByteBuffer.allocate(4 + SALT_LEN).putInt(iterations).put(salt).array();
    }

    public static byte[] deriveKey(char[] password, byte[] salt, int iterations) {
        byte[] pw = new String(password).getBytes(StandardCharsets.UTF_8);
        return Hashing.pbkdf2("HmacSHA256", pw, salt, iterations, 32);
    }
}
