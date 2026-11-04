package com.mixia.vault.core;

import java.math.BigInteger;
import java.util.Arrays;

/** Base58 and Base58Check (Bitcoin alphabet). */
public final class Base58 {
    private static final char[] ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final BigInteger FIFTY_EIGHT = BigInteger.valueOf(58);

    private Base58() {
    }

    public static String encode(byte[] input) {
        if (input.length == 0) return "";
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) zeros++;
        BigInteger value = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        while (value.signum() > 0) {
            BigInteger[] qr = value.divideAndRemainder(FIFTY_EIGHT);
            sb.append(ALPHABET[qr[1].intValue()]);
            value = qr[0];
        }
        for (int i = 0; i < zeros; i++) sb.append(ALPHABET[0]);
        return sb.reverse().toString();
    }

    public static byte[] decode(String input) {
        if (input.isEmpty()) return new byte[0];
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < input.length(); i++) {
            int digit = indexOf(input.charAt(i));
            if (digit < 0) throw new IllegalArgumentException("bad base58 char");
            value = value.multiply(FIFTY_EIGHT).add(BigInteger.valueOf(digit));
        }
        int zeros = 0;
        while (zeros < input.length() && input.charAt(zeros) == ALPHABET[0]) zeros++;
        if (value.signum() == 0) return new byte[zeros];
        byte[] raw = value.toByteArray();
        int start = (raw.length > 1 && raw[0] == 0) ? 1 : 0;
        byte[] out = new byte[zeros + raw.length - start];
        System.arraycopy(raw, start, out, zeros, raw.length - start);
        return out;
    }

    public static byte[] decodeCheck(String input) {
        byte[] raw = decode(input);
        if (raw.length < 5) throw new IllegalArgumentException("too short");
        byte[] payload = Arrays.copyOfRange(raw, 0, raw.length - 4);
        byte[] checksum = Arrays.copyOfRange(raw, raw.length - 4, raw.length);
        byte[] expected = Arrays.copyOf(Hashing.sha256d(payload), 4);
        if (!Arrays.equals(checksum, expected)) throw new IllegalArgumentException("bad checksum");
        return payload;
    }

    public static String encodeCheck(byte[] payload) {
        byte[] checksum = Hashing.sha256d(payload);
        byte[] data = Arrays.copyOf(payload, payload.length + 4);
        System.arraycopy(checksum, 0, data, payload.length, 4);
        return encode(data);
    }

    private static int indexOf(char c) {
        for (int i = 0; i < ALPHABET.length; i++) {
            if (ALPHABET[i] == c) return i;
        }
        return -1;
    }
}
