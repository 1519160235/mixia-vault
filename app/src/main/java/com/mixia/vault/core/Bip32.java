package com.mixia.vault.core;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** BIP-0032 hierarchical deterministic keys. */
public final class Bip32 {

    public static final class Node {
        public final byte[] key;   // 32 bytes private key
        public final byte[] chain; // 32 bytes chain code

        Node(byte[] key, byte[] chain) {
            this.key = key;
            this.chain = chain;
        }
    }

    private static final BigInteger N = Secp256k1.N;

    private Bip32() {
    }

    public static Node master(byte[] seed) {
        byte[] i = Hashing.hmacSha512("Bitcoin seed".getBytes(StandardCharsets.US_ASCII), seed);
        byte[] il = slice(i, 0, 32);
        byte[] ir = slice(i, 32, 64);
        BigInteger k = new BigInteger(1, il);
        if (k.signum() == 0 || k.compareTo(N) >= 0) throw new IllegalStateException("invalid master key");
        return new Node(il, ir);
    }

    public static Node derive(Node parent, int index, boolean hardened) {
        int idx = hardened ? index | 0x80000000 : index;
        byte[] data = new byte[37];
        if (hardened) {
            data[0] = 0;
            System.arraycopy(parent.key, 0, data, 1, 32);
        } else {
            byte[] pub = Secp256k1.publicKeyCompressed(parent.key);
            System.arraycopy(pub, 0, data, 0, 33);
        }
        data[33] = (byte) (idx >>> 24);
        data[34] = (byte) (idx >>> 16);
        data[35] = (byte) (idx >>> 8);
        data[36] = (byte) idx;
        byte[] i = Hashing.hmacSha512(parent.chain, data);
        BigInteger il = new BigInteger(1, slice(i, 0, 32));
        if (il.compareTo(N) >= 0) throw new IllegalStateException("invalid child");
        BigInteger child = il.add(new BigInteger(1, parent.key)).mod(N);
        if (child.signum() == 0) throw new IllegalStateException("invalid child");
        return new Node(Secp256k1.to32(child), slice(i, 32, 64));
    }

    public static Node derivePath(Node master, String path) {
        List<PathElement> elements = parsePath(path);
        Node node = master;
        for (PathElement e : elements) {
            node = derive(node, e.index, e.hardened);
        }
        return node;
    }

    public static List<PathElement> parsePath(String path) {
        String p = path.trim();
        if (p.equals("m") || p.equals("M") || p.isEmpty()) return new ArrayList<>();
        String[] parts = p.split("/");
        if (!parts[0].equalsIgnoreCase("m")) throw new IllegalArgumentException("path must start with m/");
        List<PathElement> out = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.isEmpty()) continue;
            boolean hardened = s.endsWith("'") || s.endsWith("h") || s.endsWith("H");
            if (hardened) s = s.substring(0, s.length() - 1);
            int idx = Integer.parseInt(s);
            if (idx < 0 || idx > 0x7FFFFFFF) throw new IllegalArgumentException("bad index");
            out.add(new PathElement(idx, hardened));
        }
        return out;
    }

    public static final class PathElement {
        public final int index;
        public final boolean hardened;

        PathElement(int index, boolean hardened) {
            this.index = index;
            this.hardened = hardened;
        }
    }

    public static String toXprv(Node master, int depth, int childNumber) {
        // 78-byte serialization: version(4) depth(1) fingerprint(4) child(4) chain(32) key(33)
        byte[] out = new byte[78 + 4];
        out[0] = 0x04; // xprv
        out[1] = (byte) 0x88;
        out[2] = (byte) 0xAD;
        out[3] = (byte) 0xE4;
        out[4] = (byte) depth;
        // fingerprint + child number zeroed for the master key
        System.arraycopy(master.chain, 0, out, 13, 32);
        out[45] = 0;
        System.arraycopy(master.key, 0, out, 46, 32);
        byte[] raw = new byte[78];
        System.arraycopy(out, 0, raw, 0, 78);
        return Base58.encodeCheck(raw);
    }

    private static byte[] slice(byte[] in, int from, int to) {
        byte[] out = new byte[to - from];
        System.arraycopy(in, from, out, 0, out.length);
        return out;
    }
}
