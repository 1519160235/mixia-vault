import com.mixia.vault.model.Entry;
import com.mixia.vault.model.Vault;
import com.mixia.vault.util.PasswordTools;

import java.util.List;

/** Model checks for categories, sorting and photo fields. */
public class ModelTest2 {
    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        Vault v = new Vault();
        eq("default categories", "7", String.valueOf(v.categoriesInOrder().size()));

        Entry a = new Entry();
        a.id = 1; a.title = "GitHub"; a.category = "网站"; a.favorite = true;
        a.updatedAt = 100; a.createdAt = 1000;
        a.photos.add("/data/x/p1.mxp");
        Entry b = new Entry();
        b.id = 2; b.title = "家里 Wi-Fi"; b.category = "Wi-Fi"; b.updatedAt = 300; b.createdAt = 3000;
        Entry c = new Entry();
        c.id = 3; c.title = "招商银行"; c.category = "银行卡"; c.updatedAt = 200; c.createdAt = 2000;
        v.entries.add(a); v.entries.add(b); v.entries.add(c);

        eq("updated sort", "家里 Wi-Fi", v.filtered("", "全部").get(0).title);
        v.sortMode = Vault.SORT_CREATED;
        eq("created sort", "家里 Wi-Fi", v.filtered("", "全部").get(0).title);
        v.sortMode = Vault.SORT_TITLE;
        eq("title sort (collator)", "GitHub", v.filtered("", "全部").get(0).title);
        v.sortMode = Vault.SORT_CATEGORY;

        // category order: 网站, 应用, Wi-Fi, 邮箱, 银行卡, 服务器, 其他
        eq("category sort first is 网站", "GitHub", v.filtered("", "全部").get(0).title);

        // custom category
        v.categories.add("工作");
        eq("custom category count", "8", String.valueOf(v.categoriesInOrder().size()));
        eq("rename ok", "true", String.valueOf(v.renameCategory("工作", "公司")));
        eq("rename applied", "8", String.valueOf(v.categoriesInOrder().size()));
        eq("rename duplicate blocked", "false", String.valueOf(v.renameCategory("公司", "网站")));
        eq("rename empty blocked", "false", String.valueOf(v.renameCategory("公司", "")));

        v.moveCategory(0, 6);
        eq("move category", "应用", v.categoriesInOrder().get(0));
        eq("moved item", "网站", v.categoriesInOrder().get(6));

        v.removeCategory("Wi-Fi");
        eq("remove moves entries", v.categoriesInOrder().get(0), v.findEntry(2).category);
        eq("no Wi-Fi left", "false", String.valueOf(v.categories.contains("Wi-Fi")));

        String json = v.toJsonString();
        Vault back = Vault.fromJsonString(json);
        eq("categories persisted", String.valueOf(v.categories.size()), String.valueOf(back.categories.size()));
        eq("sortMode persisted", Vault.SORT_CATEGORY, back.sortMode);
        eq("photo path persisted", "/data/x/p1.mxp", back.findEntry(1).photos.get(0));
        eq("subtitle fallback", "网站", back.findEntry(1).subtitle());

        // legacy JSON without categories/photos must still load
        String legacy = "{\"version\":1,\"entries\":[{\"id\":7,\"title\":\"老记录\",\"category\":\"自定义\"}],\"wallets\":[]}";
        Vault old = Vault.fromJsonString(legacy);
        eq("legacy entry loaded", "老记录", old.findEntry(7).title);
        eq("legacy category added", "true", String.valueOf(old.categories.contains("自定义")));
        eq("legacy photos empty", "0", String.valueOf(old.findEntry(7).photos.size()));

        String pw = PasswordTools.generate(20, true, true, true, true, true);
        eq("password length", "20", String.valueOf(pw.length()));

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
