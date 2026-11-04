package com.mixia.vault.core;

import java.math.BigInteger;

/** secp256k1 public-key derivation (only what BIP32/BIP44 needs). */
public final class Secp256k1 {
    public static final BigInteger P =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    public static final BigInteger N =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    private static final BigInteger GX =
            new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    private static final BigInteger GY =
            new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
    private static final BigInteger SEVEN = BigInteger.valueOf(7);

    private Secp256k1() {
    }

    private static final class Point {
        final BigInteger x;
        final BigInteger y;

        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    private static Point add(Point a, Point b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.x.equals(b.x)) {
            if (!a.y.equals(b.y) || a.y.signum() == 0) return null;
            return twice(a);
        }
        BigInteger dx = b.x.subtract(a.x).mod(P);
        BigInteger lambda = b.y.subtract(a.y).mod(P).multiply(dx.modInverse(P)).mod(P);
        BigInteger x3 = lambda.multiply(lambda).subtract(a.x).subtract(b.x).mod(P);
        BigInteger y3 = lambda.multiply(a.x.subtract(x3)).subtract(a.y).mod(P);
        return new Point(x3, y3);
    }

    private static Point twice(Point a) {
        BigInteger lambda = a.x.multiply(a.x).multiply(BigInteger.valueOf(3))
                .multiply(a.y.shiftLeft(1).modInverse(P)).mod(P);
        BigInteger x3 = lambda.multiply(lambda).subtract(a.x.shiftLeft(1)).mod(P);
        BigInteger y3 = lambda.multiply(a.x.subtract(x3)).subtract(a.y).mod(P);
        return new Point(x3, y3);
    }

    /** Compressed 33-byte SEC1 public key for a 32-byte private key. */
    public static byte[] publicKeyCompressed(byte[] privKey32) {
        BigInteger d = new BigInteger(1, privKey32);
        if (d.signum() == 0 || d.compareTo(N) >= 0) throw new IllegalArgumentException("bad private key");
        Point p = null;
        Point q = new Point(GX, GY);
        for (int i = 0; i < 256; i++) {
            if (d.testBit(i)) p = add(p, q);
            q = twice(q);
        }
        if (p == null) throw new IllegalStateException("null point");
        byte[] out = new byte[33];
        out[0] = (byte) (p.y.testBit(0) ? 0x03 : 0x02);
        byte[] x = to32(p.x);
        System.arraycopy(x, 0, out, 1, 32);
        return out;
    }

    /** Uncompressed public key without the 0x04 prefix (64 bytes). */
    public static byte[] publicKeyUncompressed(byte[] privKey32) {
        BigInteger d = new BigInteger(1, privKey32);
        if (d.signum() == 0 || d.compareTo(N) >= 0) throw new IllegalArgumentException("bad private key");
        Point p = null;
        Point q = new Point(GX, GY);
        for (int i = 0; i < 256; i++) {
            if (d.testBit(i)) p = add(p, q);
            q = twice(q);
        }
        byte[] out = new byte[64];
        System.arraycopy(to32(p.x), 0, out, 0, 32);
        System.arraycopy(to32(p.y), 0, out, 32, 32);
        return out;
    }

    public static byte[] to32(BigInteger v) {
        byte[] raw = v.toByteArray();
        byte[] out = new byte[32];
        if (raw.length > 32) {
            System.arraycopy(raw, raw.length - 32, out, 0, 32);
        } else {
            System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        }
        return out;
    }

    /** Sanity check used by tests: y^2 == x^3 + 7 (mod p). */
    public static boolean onCurve(BigInteger x, BigInteger y) {
        return y.multiply(y).mod(P).equals(x.multiply(x).multiply(x).add(SEVEN).mod(P));
    }
}
