package com.mixia.vault.model;

import org.json.JSONException;
import org.json.JSONObject;

/** Stored BIP39 mnemonic. */
public class Wallet {
    public long id;
    public String title = "";
    public String mnemonic = "";
    public String passphrase = "";
    public String coin = "ETH";
    public String path = "m/44'/60'/0'/0/0";
    public String notes = "";
    public long createdAt;
    public long updatedAt;

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("mnemonic", mnemonic);
        o.put("passphrase", passphrase);
        o.put("coin", coin);
        o.put("path", path);
        o.put("notes", notes);
        o.put("createdAt", createdAt);
        o.put("updatedAt", updatedAt);
        return o;
    }

    public static Wallet fromJson(JSONObject o) {
        Wallet w = new Wallet();
        w.id = o.optLong("id");
        w.title = o.optString("title", "");
        w.mnemonic = o.optString("mnemonic", "");
        w.passphrase = o.optString("passphrase", "");
        w.coin = o.optString("coin", "ETH");
        w.path = o.optString("path", "m/44'/60'/0'/0/0");
        w.notes = o.optString("notes", "");
        w.createdAt = o.optLong("createdAt");
        w.updatedAt = o.optLong("updatedAt");
        return w;
    }

    public int wordCount() {
        String m = mnemonic == null ? "" : mnemonic.trim();
        if (m.isEmpty()) return 0;
        return m.split("\\s+").length;
    }
}
