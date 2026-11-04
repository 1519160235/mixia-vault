import com.mixia.vault.model.Entry;
import com.mixia.vault.model.Vault;
import com.mixia.vault.model.Wallet;
import com.mixia.vault.util.PasswordTools;

import java.util.List;

/** Model-level checks that do not require an Android device. */
public class ModelTest {
    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        Vault v = new Vault();
        Entry a = new Entry();
        a.id = 1;
        a.title = "GitHub";
        a.username = "me@example.com";
        a.password = "S3cret!pass";
        a.url = "https://github.com";
        a.tags = "工作 dev";
        a.category = "网站";
        a.favorite = true;
        a.updatedAt = 100;
        a.createdAt = 1000;
        Entry b = new Entry();
        b.id = 2;
        b.title = "家里 Wi-Fi";
        b.username = "TP-LINK";
        b.password = "wifi-pass";
        b.category = "Wi-Fi";
        b.updatedAt = 200;
        b.createdAt = 3000;
        Entry c = new Entry();
        c.id = 3;
        c.title = "招商银行";
        c.category = "银行卡";
        c.updatedAt = 300;
        c.createdAt = 2000;
        v.entries.add(a);
        v.entries.add(b);
        v.entries.add(c);

        Wallet w = new Wallet();
        w.id = 9;
        w.title = "主钱包";
        w.mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        w.path = "m/44'/60'/0'/0/0";
        w.coin = "ETH";
        v.wallets.add(w);

        String json = v.toJsonString();
        Vault back = Vault.fromJsonString(json);
        eq("entries roundtrip", "3", String.valueOf(back.entries.size()));
        eq("wallet roundtrip", "主钱包", back.wallets.get(0).title);
        eq("password roundtrip", "S3cret!pass", back.entries.get(0).password);
        eq("category roundtrip", "Wi-Fi", back.entries.get(1).category);

        List<Entry> all = back.filtered("", "全部");
        eq("default sort newest first", "招商银行", all.get(0).title);
        eq("then 家里 Wi-Fi", "家里 Wi-Fi", all.get(1).title);
        back.sortMode = Vault.SORT_CREATED;
        eq("created sort", "家里 Wi-Fi", back.filtered("", "全部").get(0).title);
        eq("filters by category", "1", String.valueOf(back.filtered("", "Wi-Fi").size()));
        eq("searches tags", "GitHub", back.filtered("dev", "全部").get(0).title);
        eq("searches username", "1", String.valueOf(back.filtered("me@example", "全部").size()));
        eq("unknown query empty", "0", String.valueOf(back.filtered("zzzz", "全部").size()));
        eq("countIn 收藏", "1", String.valueOf(back.countIn("收藏")));
        eq("countIn 全部", "3", String.valueOf(back.countIn("全部")));

        String pw = PasswordTools.generate(24, true, true, true, false, true);
        eq("generated length", "24", String.valueOf(pw.length()));
        eq("no ambiguous chars", "true", String.valueOf(pw.indexOf('I') < 0 && pw.indexOf('l') < 0 && pw.indexOf('0') < 0));
        eq("strength very strong", "很强", PasswordTools.strengthLabel(pw));
        eq("empty strength", "未填写", PasswordTools.strengthLabel(""));

        System.out.println("passed=" + pass + " failed=" + fail);
        if (fail > 0) System.exit(1);
    }

    private static void eq(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            pass++;
            System.out.println("PASS  " + name);
        } else {
            fail++;
            System.out.println("FAIL  " + name + " expected=" + expected + " actual=" + actual);
        }
    }
}
