package com.mixia.vault.core;

/** RIPEMD-160 (needed for Bitcoin hash160; the JDK/Android do not ship it). */
public final class Ripemd160 {

    private static final int[] R_LEFT = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
            3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
            1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
            4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13};
    private static final int[] R_RIGHT = {
            5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
            6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
            15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
            8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
            12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11};
    private static final int[] S_LEFT = {
            11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
            7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
            11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
            11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
            9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6};
    private static final int[] S_RIGHT = {
            8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
            9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
            9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
            15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
            8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11};

    private Ripemd160() {
    }

    public static byte[] hash(byte[] message) {
        int[] h = {0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0};
        long bitLen = (long) message.length * 8;
        int padded = ((message.length + 8) / 64 + 1) * 64;
        byte[] msg = new byte[padded];
        System.arraycopy(message, 0, msg, 0, message.length);
        msg[message.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            msg[padded - 8 + i] = (byte) (bitLen >>> (8 * i));
        }

        int[] x = new int[16];
        for (int off = 0; off < padded; off += 64) {
            for (int i = 0; i < 16; i++) {
                x[i] = (msg[off + i * 4] & 0xFF)
                        | ((msg[off + i * 4 + 1] & 0xFF) << 8)
                        | ((msg[off + i * 4 + 2] & 0xFF) << 16)
                        | ((msg[off + i * 4 + 3] & 0xFF) << 24);
            }
            int al = h[0], bl = h[1], cl = h[2], dl = h[3], el = h[4];
            int ar = h[0], br = h[1], cr = h[2], dr = h[3], er = h[4];

            for (int j = 0; j < 80; j++) {
                int round = j / 16;
                int t = fLeft(round, bl, cl, dl);
                t = Integer.rotateLeft(t + al + x[R_LEFT[j]] + K_LEFT[round], S_LEFT[j]) + el;
                al = el;
                el = dl;
                dl = Integer.rotateLeft(cl, 10);
                cl = bl;
                bl = t;

                t = fRight(round, br, cr, dr);
                t = Integer.rotateLeft(t + ar + x[R_RIGHT[j]] + K_RIGHT[round], S_RIGHT[j]) + er;
                ar = er;
                er = dr;
                dr = Integer.rotateLeft(cr, 10);
                cr = br;
                br = t;
            }

            int tmp = h[1] + cl + dr;
            h[1] = h[2] + dl + er;
            h[2] = h[3] + el + ar;
            h[3] = h[4] + al + br;
            h[4] = h[0] + bl + cr;
            h[0] = tmp;
        }

        byte[] out = new byte[20];
        for (int i = 0; i < 5; i++) {
            out[i * 4] = (byte) h[i];
            out[i * 4 + 1] = (byte) (h[i] >>> 8);
            out[i * 4 + 2] = (byte) (h[i] >>> 16);
            out[i * 4 + 3] = (byte) (h[i] >>> 24);
        }
        return out;
    }

    private static final int[] K_LEFT = {0x00000000, 0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC, 0xA953FD4E};
    private static final int[] K_RIGHT = {0x50A28BE6, 0x5C4DD124, 0x6D703EF3, 0x7A6D76E9, 0x00000000};

    private static int fLeft(int round, int x, int y, int z) {
        switch (round) {
            case 0:
                return x ^ y ^ z;
            case 1:
                return (x & y) | (~x & z);
            case 2:
                return (x | ~y) ^ z;
            case 3:
                return (x & z) | (y & ~z);
            default:
                return x ^ (y | ~z);
        }
    }

    private static int fRight(int round, int x, int y, int z) {
        switch (round) {
            case 0:
                return x ^ (y | ~z);
            case 1:
                return (x & z) | (y & ~z);
            case 2:
                return (x | ~y) ^ z;
            case 3:
                return (x & y) | (~x & z);
            default:
                return x ^ y ^ z;
        }
    }
}
