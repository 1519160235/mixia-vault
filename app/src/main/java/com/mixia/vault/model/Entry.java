package com.mixia.vault.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One credential record. */
public class Entry {
    /** Categories used when the vault has none of its own yet. */
    public static final String[] DEFAULT_CATEGORIES = {"网站", "应用", "Wi-Fi", "邮箱", "银行卡", "服务器", "其他"};

    public long id;
    public String title = "";
    public String username = "";
    public String password = "";
    public String url = "";
    public String notes = "";
    public String tags = "";
    public String category = "网站";
    public boolean favorite;
    public long createdAt;
    public long updatedAt;
    /** Absolute paths of encrypted photo copies, in display order. */
    public final List<String> photos = new ArrayList<>();

    public Entry() {
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("username", username);
        o.put("password", password);
        o.put("url", url);
        o.put("notes", notes);
        o.put("tags", tags);
        o.put("category", category);
        o.put("favorite", favorite);
        o.put("createdAt", createdAt);
        o.put("updatedAt", updatedAt);
        JSONArray arr = new JSONArray();
        for (String p : photos) arr.put(p);
        o.put("photos", arr);
        return o;
    }

    public static Entry fromJson(JSONObject o) {
        Entry e = new Entry();
        e.id = o.optLong("id");
        e.title = o.optString("title", "");
        e.username = o.optString("username", "");
        e.password = o.optString("password", "");
        e.url = o.optString("url", "");
        e.notes = o.optString("notes", "");
        e.tags = o.optString("tags", "");
        e.category = o.optString("category", "网站");
        e.favorite = o.optBoolean("favorite", false);
        e.createdAt = o.optLong("createdAt");
        e.updatedAt = o.optLong("updatedAt");
        JSONArray arr = o.optJSONArray("photos");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String p = arr.optString(i, "");
                if (!p.isEmpty()) e.photos.add(p);
            }
        }
        return e;
    }

    /** Subtitle shown in the list. */
    public String subtitle() {
        if (username != null && !username.isEmpty()) return username;
        if (url != null && !url.isEmpty()) return url;
        return category;
    }

    public boolean hasPhotos() {
        return !photos.isEmpty();
    }
}
