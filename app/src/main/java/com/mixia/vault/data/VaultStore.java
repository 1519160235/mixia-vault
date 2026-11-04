package com.mixia.vault.data;

import android.content.Context;

import com.mixia.vault.core.AesGcm;
import com.mixia.vault.core.VaultFile;
import com.mixia.vault.model.Vault;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/** File based persistence. Everything on disk is AES-256-GCM encrypted. */
public class VaultStore {
    private final File file;

    public VaultStore(Context context) {
        file = new File(context.getFilesDir(), "vault.dat");
    }

    public boolean exists() {
        return file.exists() && file.length() > 0;
    }

    public byte[] readBytes() throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] data = new byte[(int) file.length()];
            int off = 0;
            while (off < data.length) {
                int n = in.read(data, off, data.length - off);
                if (n < 0) break;
                off += n;
            }
            return data;
        } finally {
            in.close();
        }
    }

    public Vault load(VaultFile vf, byte[] data) throws AesGcm.BadPasswordException {
        try {
            return Vault.fromJsonString(new String(vf.openPayload(data), "UTF-8"));
        } catch (AesGcm.BadPasswordException e) {
            throw e;
        } catch (Exception e) {
            throw new AesGcm.BadPasswordException();
        }
    }

    public void save(VaultFile vf, Vault vault) throws Exception {
        byte[] plain = vault.toJsonString().getBytes("UTF-8");
        byte[] sealed = vf.seal(plain);
        File tmp = new File(file.getParentFile(), "vault.tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(sealed);
            out.getFD().sync();
        } finally {
            out.close();
        }
        if (!tmp.renameTo(file)) {
            // fallback: direct write
            FileOutputStream direct = new FileOutputStream(file);
            try {
                direct.write(sealed);
            } finally {
                direct.close();
            }
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    public void wipe() {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
        File tmp = new File(file.getParentFile(), "vault.tmp");
        //noinspection ResultOfMethodCallIgnored
        tmp.delete();
        byte[] junk = new byte[4096];
        new SecureRandom().nextBytes(junk);
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(junk);
            out.close();
        } catch (IOException ignored) {
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public File file() {
        return file;
    }
}
