package com.mixia.vault.core;

/** BIP-173 Bech32 encoder, enough for native-segwit (bc1...) addresses. */
public final class Bech32 {
    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final int[] GEN = {0x3B6A57B2, 0x26508E6D, 0x1EA119FA, 0x3D4233DD, 0x2A1462B3};

    private Bech32() {
    }

    public static String encodeSegwitV0(String hrp, byte[] program20) {
        int[] data = convertBits(program20, 8, 5, true);
        int[] values = new int[data.length + 1];
        values[0] = 0; // witness version
        System.arraycopy(data, 0, values, 1, data.length);
        int[] checksum = createChecksum(hrp, values);
        StringBuilder sb = new StringBuilder(hrp).append('1');
        for (int v : values) sb.append(CHARSET.charAt(v));
        for (int v : checksum) sb.append(CHARSET.charAt(v));
        return sb.toString();
    }

    private static int[] createChecksum(String hrp, int[] data) {
        int[] values = concat(hrpExpand(hrp), data, new int[]{0, 0, 0, 0, 0, 0});
        int mod = polymod(values) ^ 1;
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) {
            out[i] = (mod >>> (5 * (5 - i))) & 31;
        }
        return out;
    }

    private static int[] hrpExpand(String hrp) {
        int[] out = new int[hrp.length() * 2 + 1];
        for (int i = 0; i < hrp.length(); i++) {
            out[i] = hrp.charAt(i) >>> 5;
        }
        out[hrp.length()] = 0;
        for (int i = 0; i < hrp.length(); i++) {
            out[hrp.length() + 1 + i] = hrp.charAt(i) & 31;
        }
        return out;
    }

    private static int polymod(int[] values) {
        int chk = 1;
        for (int v : values) {
            int top = chk >>> 25;
            chk = ((chk & 0x1FFFFFF) << 5) ^ v;
            for (int i = 0; i < 5; i++) {
                if (((top >>> i) & 1) != 0) chk ^= GEN[i];
            }
        }
        return chk;
    }

    private static int[] convertBits(byte[] data, int from, int to, boolean pad) {
        int acc = 0;
        int bits = 0;
        int maxv = (1 << to) - 1;
        int[] out = new int[(data.length * from + to - 1) / to];
        int idx = 0;
        for (byte b : data) {
            acc = (acc << from) | (b & 0xFF);
            bits += from;
            while (bits >= to) {
                bits -= to;
                out[idx++] = (acc >>> bits) & maxv;
            }
        }
        if (pad && bits > 0) {
            out[idx++] = (acc << (to - bits)) & maxv;
        }
        int[] trimmed = new int[idx];
        System.arraycopy(out, 0, trimmed, 0, idx);
        return trimmed;
    }

    private static int[] concat(int[] a, int[] b, int[] c) {
        int[] out = new int[a.length + b.length + c.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        System.arraycopy(c, 0, out, a.length + b.length, c.length);
        return out;
    }
}
