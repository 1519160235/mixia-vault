package com.mixia.vault.core;

import java.math.BigInteger;
import java.util.Locale;

/** Derives display-ready material from a BIP39 mnemonic. */
public final class WalletDeriver {

    public enum Coin {
        ETH("以太坊 ETH", "m/44'/60'/0'/0/0"),
        BTC("比特币 BTC", "m/44'/0'/0'/0/0"),
        BTC_SEGWIT("比特币 隔离见证", "m/84'/0'/0'/0/0");

        public final String label;
        public final String defaultPath;

        Coin(String label, String defaultPath) {
            this.label = label;
            this.defaultPath = defaultPath;
        }
    }

    public static final class Result {
        public final String address;
        public final String privateKeyHex;
        public final String publicKeyHex;
        public final String wif;
        public final String path;

        Result(String address, String privateKeyHex, String publicKeyHex, String wif, String path) {
            this.address = address;
            this.privateKeyHex = privateKeyHex;
            this.publicKeyHex = publicKeyHex;
            this.wif = wif;
            this.path = path;
        }
    }

    private WalletDeriver() {
    }

    public static Result derive(String mnemonic, String passphrase, String path, Coin coin) {
        byte[] seed = Bip39.toSeed(mnemonic, passphrase);
        Bip32.Node node = Bip32.derivePath(Bip32.master(seed), path);
        byte[] priv = node.key;
        byte[] pubCompressed = Secp256k1.publicKeyCompressed(priv);
        String address;
        String wif = "";
        if (coin == Coin.ETH) {
            byte[] pub = Secp256k1.publicKeyUncompressed(priv);
            byte[] hash = Keccak256.hash(pub);
            byte[] addr = new byte[20];
            System.arraycopy(hash, 12, addr, 0, 20);
            address = Eip55.checksum(Hex.encode(addr));
        } else {
            byte[] hash160 = Ripemd160.hash(Hashing.sha256(pubCompressed));
            if (coin == Coin.BTC) {
                byte[] payload = new byte[21];
                payload[0] = 0;
                System.arraycopy(hash160, 0, payload, 1, 20);
                address = Base58.encodeCheck(payload);
            } else {
                address = Bech32.encodeSegwitV0("bc", hash160);
            }
            byte[] wifPayload = new byte[34];
            wifPayload[0] = (byte) 0x80;
            System.arraycopy(priv, 0, wifPayload, 1, 32);
            wifPayload[33] = 0x01;
            wif = Base58.encodeCheck(wifPayload);
        }
        return new Result(address, Hex.encode(priv), Hex.encode(pubCompressed), wif, path);
    }

    /** EIP-55 mixed-case checksum encoding. */
    public static final class Eip55 {
        private Eip55() {
        }

        public static String checksum(String lowerHex) {
            String addr = lowerHex.toLowerCase(Locale.ROOT).replace("0x", "");
            byte[] hash = Keccak256.hash(Hashing.utf8(addr));
            StringBuilder sb = new StringBuilder("0x");
            for (int i = 0; i < addr.length(); i++) {
                char c = addr.charAt(i);
                if (c >= '0' && c <= '9') {
                    sb.append(c);
                } else {
                    int nibble = (hash[i / 2] >>> (i % 2 == 0 ? 4 : 0)) & 0x0F;
                    sb.append(nibble >= 8 ? Character.toUpperCase(c) : c);
                }
            }
            return sb.toString();
        }
    }

    public static String compressedToP2pkh(byte[] pubCompressed) {
        byte[] hash160 = Ripemd160.hash(Hashing.sha256(pubCompressed));
        byte[] payload = new byte[21];
        payload[0] = 0;
        System.arraycopy(hash160, 0, payload, 1, 20);
        return Base58.encodeCheck(payload);
    }

    static BigInteger unused() {
        return BigInteger.ONE;
    }
}
