import com.mixia.vault.core.*;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Plain-JDK verification harness (not shipped in the APK). */
public class TestVectors {
    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        Bip39.load(new FileInputStream(args[0]));

        eq("ripemd160('')", "9c1185a5c5e9fc54612808977ee8f548b2258d31", Hex.encode(Ripemd160.hash(new byte[0])));
        eq("ripemd160('abc')", "8eb208f7e05d987a9b044a8e98c6b087f15a0bfc", Hex.encode(Ripemd160.hash("abc".getBytes(StandardCharsets.US_ASCII))));
        eq("ripemd160('message digest')", "5d0689ef49d2fae572b881b123a85ffa21595f36", Hex.encode(Ripemd160.hash("message digest".getBytes(StandardCharsets.US_ASCII))));
        eq("keccak256('')", "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", Hex.encode(Keccak256.hash(new byte[0])));
        eq("keccak256('abc')", "4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45", Hex.encode(Keccak256.hash("abc".getBytes(StandardCharsets.US_ASCII))));
        eq("pbkdf2-sha512('password','salt',1)", "867f70cf1ade02cff3752599a3a53dc4af34c7a669815ae5d513554e1c8cf252c02d470a285a0501bad999bfe943c08f050235d7d68b1da55e63f73b60a57fce",
                Hex.encode(Hashing.pbkdf2("HmacSHA512", "password".getBytes(StandardCharsets.US_ASCII), "salt".getBytes(StandardCharsets.US_ASCII), 1, 64)));

        String words12 = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        eq("bip39 from zero entropy", words12, Bip39.fromEntropy(new byte[16]));
        eq("bip39 valid", "true", String.valueOf(Bip39.validate(words12)));
        eq("bip39 invalid checksum", "false", String.valueOf(Bip39.validate(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon")));
        eq("bip39 seed(TREZOR)", "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04",
                Hex.encode(Bip39.toSeed(words12, "TREZOR")));
        eq("bip39 24 words", "24", String.valueOf(Bip39.generate(24).split(" ").length));

        WalletDeriver.Result eth = WalletDeriver.derive(words12, "", "m/44'/60'/0'/0/0", WalletDeriver.Coin.ETH);
        eq("eth address", "0x9858EfFD232B4033E47d90003D41EC34EcaEda94", eth.address);

        String hardhat = "test test test test test test test test test test test junk";
        WalletDeriver.Result eth2 = WalletDeriver.derive(hardhat, "", "m/44'/60'/0'/0/0", WalletDeriver.Coin.ETH);
        eq("eth hardhat address", "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266", eth2.address);

        WalletDeriver.Result btc = WalletDeriver.derive(words12, "", "m/44'/0'/0'/0/0", WalletDeriver.Coin.BTC);
        eq("btc p2pkh address", "1LqBGSKuX5yYUonjxT5qGfpUsXKYYWeabA", btc.address);

        WalletDeriver.Result seg = WalletDeriver.derive(words12, "", "m/84'/0'/0'/0/0", WalletDeriver.Coin.BTC_SEGWIT);
        eq("btc bech32 address", "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", seg.address);

        eq("base58 'Hello World!'", "2NEpo7TZRRrLZSi2U", Base58.encode("Hello World!".getBytes(StandardCharsets.US_ASCII)));
        eq("base58check roundtrip", btc.address, Base58.encodeCheck(Base58.decodeCheck(btc.address)));

        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) i;
        byte[] plain = "secret 密码".getBytes(StandardCharsets.UTF_8);
        byte[] ct = AesGcm.encrypt(key, plain, "aad".getBytes(StandardCharsets.US_ASCII));
        eq("aes-gcm roundtrip", new String(plain, StandardCharsets.UTF_8),
                new String(AesGcm.decrypt(key, ct, "aad".getBytes(StandardCharsets.US_ASCII)), StandardCharsets.UTF_8));
        ct[ct.length - 1] ^= 1;
        boolean threw = false;
        try {
            AesGcm.decrypt(key, ct, "aad".getBytes(StandardCharsets.US_ASCII));
        } catch (AesGcm.BadPasswordException e) {
            threw = true;
        }
        eq("aes-gcm tamper rejected", "true", String.valueOf(threw));

        char[] pwd = "correct horse".toCharArray();
        VaultFile vf = VaultFile.create(pwd);
        byte[] sealed = vf.seal("{\"v\":1}".getBytes(StandardCharsets.UTF_8));
        VaultFile reopened = VaultFile.open(pwd, sealed);
        eq("vault roundtrip", "{\"v\":1}", new String(reopened.openPayload(sealed), StandardCharsets.UTF_8));
        boolean wrong = false;
        try {
            VaultFile.open("wrong".toCharArray(), sealed);
        } catch (AesGcm.BadPasswordException e) {
            wrong = true;
        }
        eq("vault wrong password rejected", "true", String.valueOf(wrong));

        String generated = Bip39.generate(12);
        eq("generated mnemonic valid", "true", String.valueOf(Bip39.validate(generated)));

        System.out.println("passed=" + pass + " failed=" + fail);
        if (fail > 0) System.exit(1);
    }

    private static void eq(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            pass++;
            System.out.println("PASS  " + name);
        } else {
            fail++;
            System.out.println("FAIL  " + name + "\n   expected: " + expected + "\n   actual  : " + actual);
        }
    }
}
