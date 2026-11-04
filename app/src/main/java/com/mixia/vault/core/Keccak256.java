package com.mixia.vault.core;

/** Original Keccak-256 (Ethereum flavour, padding 0x01), not SHA3-256. */
public final class Keccak256 {
    private static final int RATE = 136; // 1088 bits
    private static final long[] RC = {
            0x0000000000000001L, 0x0000000000008082L, 0x800000000000808aL, 0x8000000080008000L,
            0x000000000000808bL, 0x0000000080000001L, 0x8000000080008081L, 0x8000000000008009L,
            0x000000000000008aL, 0x0000000000000088L, 0x0000000080008009L, 0x000000008000000aL,
            0x000000008000808bL, 0x800000000000008bL, 0x8000000000008089L, 0x8000000000008003L,
            0x8000000000008002L, 0x8000000000000080L, 0x000000000000800aL, 0x800000008000000aL,
            0x8000000080008081L, 0x8000000000008080L, 0x0000000080000001L, 0x8000000080008008L};

    private static final int[] ROT = {
            0, 1, 62, 28, 27,
            36, 44, 6, 55, 20,
            3, 10, 43, 25, 39,
            41, 45, 15, 21, 8,
            18, 2, 61, 56, 14};

    private Keccak256() {
    }

    public static byte[] hash(byte[] input) {
        long[] st = new long[25];
        int len = input.length;
        int full = len / RATE;
        for (int b = 0; b < full; b++) {
            absorbBlock(st, input, b * RATE);
            keccakF(st);
        }
        byte[] last = new byte[RATE];
        int rem = len - full * RATE;
        System.arraycopy(input, full * RATE, last, 0, rem);
        last[rem] ^= 0x01;
        last[RATE - 1] ^= (byte) 0x80;
        absorbBlock(st, last, 0);
        keccakF(st);

        byte[] out = new byte[32];
        for (int i = 0; i < 4; i++) {
            long lane = st[i];
            for (int j = 0; j < 8; j++) {
                out[i * 8 + j] = (byte) (lane >>> (8 * j));
            }
        }
        return out;
    }

    private static void absorbBlock(long[] st, byte[] data, int off) {
        for (int i = 0; i < RATE / 8; i++) {
            long lane = 0;
            for (int j = 0; j < 8; j++) {
                lane |= (long) (data[off + i * 8 + j] & 0xFF) << (8 * j);
            }
            st[i] ^= lane;
        }
    }

    private static void keccakF(long[] a) {
        long[] c = new long[5];
        long[] b = new long[25];
        for (int round = 0; round < 24; round++) {
            // theta
            for (int x = 0; x < 5; x++) {
                c[x] = a[x] ^ a[x + 5] ^ a[x + 10] ^ a[x + 15] ^ a[x + 20];
            }
            for (int x = 0; x < 5; x++) {
                long d = c[(x + 4) % 5] ^ Long.rotateLeft(c[(x + 1) % 5], 1);
                for (int y = 0; y < 5; y++) {
                    a[x + 5 * y] ^= d;
                }
            }
            // rho + pi
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    int idx = x + 5 * y;
                    int nx = y;
                    int ny = (2 * x + 3 * y) % 5;
                    b[nx + 5 * ny] = Long.rotateLeft(a[idx], ROT[idx]);
                }
            }
            // chi
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 5; x++) {
                    a[x + 5 * y] = b[x + 5 * y] ^ ((~b[(x + 1) % 5 + 5 * y]) & b[(x + 2) % 5 + 5 * y]);
                }
            }
            // iota
            a[0] ^= RC[round];
        }
    }
}
