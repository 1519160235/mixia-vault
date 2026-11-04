package com.mixia.vault.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** The decrypted vault content. */
public class Vault {
    public static final String SORT_UPDATED = "updated";
    public static final String SORT_CREATED = "created";
    public static final String SORT_TITLE = "title";
    public static final String SORT_CATEGORY = "category";

    public final List<Entry> entries = new ArrayList<>();
    public final List<Wallet> wallets = new ArrayList<>();
    /** User-defined, ordered category list. */
    public final List<String> categories = new ArrayList<>();
    public String sortMode = SORT_UPDATED;

    public Vault() {
        normalizeCategories();
    }

    /** Ensures at least the built-in categories exist and every entry has a valid one. */
    public void normalizeCategories() {
        if (categories.isEmpty()) {
            Collections.addAll(categories, Entry.DEFAULT_CATEGORIES);
        }
        for (Entry e : entries) {
            if (e.category == null || e.category.trim().isEmpty()) {
                e.category = categories.get(0);
            } else if (!categories.contains(e.category)) {
                categories.add(e.category);
            }
        }
    }

    public List<String> categoriesInOrder() {
        normalizeCategories();
        return categories;
    }

    public String toJsonString() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", 2);
        JSONArray es = new JSONArray();
        for (Entry e : entries) es.put(e.toJson());
        root.put("entries", es);
        JSONArray ws = new JSONArray();
        for (Wallet w : wallets) ws.put(w.toJson());
        root.put("wallets", ws);
        JSONArray cs = new JSONArray();
        for (String c : categoriesInOrder()) cs.put(c);
        root.put("categories", cs);
        root.put("sortMode", sortMode == null ? SORT_UPDATED : sortMode);
        return root.toString();
    }

    public static Vault fromJsonString(String json) throws JSONException {
        Vault v = new Vault();
        JSONObject root = new JSONObject(json);
        JSONArray cs = root.optJSONArray("categories");
        if (cs != null) {
            v.categories.clear();
            for (int i = 0; i < cs.length(); i++) {
                String c = cs.optString(i, "").trim();
                if (!c.isEmpty() && !v.categories.contains(c)) v.categories.add(c);
            }
            if (v.categories.isEmpty()) Collections.addAll(v.categories, Entry.DEFAULT_CATEGORIES);
        }
        v.sortMode = root.optString("sortMode", SORT_UPDATED);
        JSONArray es = root.optJSONArray("entries");
        if (es != null) {
            for (int i = 0; i < es.length(); i++) v.entries.add(Entry.fromJson(es.getJSONObject(i)));
        }
        JSONArray ws = root.optJSONArray("wallets");
        if (ws != null) {
            for (int i = 0; i < ws.length(); i++) v.wallets.add(Wallet.fromJson(ws.getJSONObject(i)));
        }
        v.normalizeCategories();
        return v;
    }

    public Entry findEntry(long id) {
        for (Entry e : entries) if (e.id == id) return e;
        return null;
    }

    public Wallet findWallet(long id) {
        for (Wallet w : wallets) if (w.id == id) return w;
        return null;
    }

    public boolean removeEntry(long id) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id == id) {
                entries.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean removeWallet(long id) {
        for (int i = 0; i < wallets.size(); i++) {
            if (wallets.get(i).id == id) {
                wallets.remove(i);
                return true;
            }
        }
        return false;
    }

    public List<Entry> filtered(String query, String category) {
        List<Entry> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Entry e : entries) {
            if (category != null && !category.isEmpty() && !category.equals("全部")) {
                if (category.equals("收藏")) {
                    if (!e.favorite) continue;
                } else if (!category.equals(e.category)) {
                    continue;
                }
            }
            if (!q.isEmpty()) {
                String hay = (e.title + " " + e.username + " " + e.url + " " + e.tags + " " + e.notes + " " + e.category)
                        .toLowerCase(Locale.ROOT);
                if (!hay.contains(q)) continue;
            }
            out.add(e);
        }
        Collections.sort(out, comparator());
        return out;
    }

    private Comparator<Entry> comparator() {
        final Collator collator = Collator.getInstance(Locale.CHINA);
        String mode = sortMode == null ? SORT_UPDATED : sortMode;
        if (SORT_TITLE.equals(mode)) {
            return (a, b) -> collator.compare(a.title == null ? "" : a.title, b.title == null ? "" : b.title);
        }
        if (SORT_CREATED.equals(mode)) {
            return (a, b) -> Long.compare(b.createdAt, a.createdAt);
        }
        if (SORT_CATEGORY.equals(mode)) {
            return (a, b) -> {
                int ia = categories.indexOf(a.category);
                int ib = categories.indexOf(b.category);
                if (ia != ib) return Integer.compare(ia < 0 ? 999 : ia, ib < 0 ? 999 : ib);
                return collator.compare(a.title == null ? "" : a.title, b.title == null ? "" : b.title);
            };
        }
        return (a, b) -> Long.compare(b.updatedAt, a.updatedAt);
    }

    public String sortLabel() {
        String mode = sortMode == null ? SORT_UPDATED : sortMode;
        switch (mode) {
            case SORT_CREATED:
                return "最近创建";
            case SORT_TITLE:
                return "标题 A→Z";
            case SORT_CATEGORY:
                return "按分类";
            default:
                return "最近更新";
        }
    }

    public int countIn(String category) {
        if (category.equals("全部")) return entries.size();
        int n = 0;
        for (Entry e : entries) {
            if (category.equals("收藏") ? e.favorite : category.equals(e.category)) n++;
        }
        return n;
    }

    /** Renames a category everywhere; returns false when the name is empty or duplicate. */
    public boolean renameCategory(String from, String to) {
        String target = to == null ? "" : to.trim();
        if (target.isEmpty()) return false;
        if (target.equals(from)) return true;
        if (categories.contains(target)) return false;
        int index = categories.indexOf(from);
        if (index < 0) return false;
        categories.set(index, target);
        for (Entry e : entries) {
            if (from.equals(e.category)) e.category = target;
        }
        return true;
    }

    public void removeCategory(String name) {
        if (categories.size() <= 1) return;
        if (!categories.remove(name)) return;
        String fallback = categories.get(0);
        for (Entry e : entries) {
            if (name.equals(e.category)) e.category = fallback;
        }
    }

    public void moveCategory(int from, int to) {
        if (from < 0 || to < 0 || from >= categories.size() || to >= categories.size()) return;
        String value = categories.remove(from);
        categories.add(to, value);
    }
}
