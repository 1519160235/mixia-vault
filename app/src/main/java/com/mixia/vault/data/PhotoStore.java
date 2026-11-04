package com.mixia.vault.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.mixia.vault.core.AesGcm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;

/**
 * Photos (screenshots of passwords, recovery codes, QR codes...) are encrypted with the same
 * vault key as the database and written to filesDir/photos. Nothing is ever stored in clear.
 */
public final class PhotoStore {
    private static final byte[] MAGIC = {'M', 'X', 'P', '1'};
    private static final SecureRandom RANDOM = new SecureRandom();

    private PhotoStore() {
    }

    public static File dir(Context context) {
        File d = new File(context.getFilesDir(), "photos");
        if (!d.exists()) {
            //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
        }
        return d;
    }

    /** Encrypts the stream and returns the new file path. */
    public static String save(Context context, byte[] key, InputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[16 * 1024];
        int n;
        while ((n = in.read(chunk)) > 0) buffer.write(chunk, 0, n);
        in.close();
        return saveBytes(context, key, buffer.toByteArray());
    }

    public static String saveBytes(Context context, byte[] key, byte[] original) throws Exception {
        byte[] sealed = AesGcm.encrypt(key, original, MAGIC);
        String name = "p" + System.currentTimeMillis() + "_" + Math.abs(RANDOM.nextInt(100000)) + ".mxp";
        File out = new File(dir(context), name);
        FileOutputStream stream = new FileOutputStream(out);
        try {
            stream.write(MAGIC);
            stream.write(sealed);
            stream.getFD().sync();
        } finally {
            stream.close();
        }
        return out.getAbsolutePath();
    }

    public static byte[] read(String path, byte[] key) throws Exception {
        File file = new File(path);
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[16 * 1024];
        int n;
        while ((n = in.read(chunk)) > 0) buffer.write(chunk, 0, n);
        in.close();
        byte[] all = buffer.toByteArray();
        if (all.length <= MAGIC.length) throw new IllegalStateException("照片已损坏");
        for (int i = 0; i < MAGIC.length; i++) {
            if (all[i] != MAGIC[i]) throw new IllegalStateException("照片格式不正确");
        }
        byte[] payload = new byte[all.length - MAGIC.length];
        System.arraycopy(all, MAGIC.length, payload, 0, payload.length);
        return AesGcm.decrypt(key, payload, MAGIC);
    }

    /** Decodes a thumbnail, downsampled so large screenshots do not blow up memory. */
    public static Bitmap thumbnail(String path, byte[] key, int maxSize) {
        try {
            byte[] data = read(path, key);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            int sample = 1;
            while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Bitmap fullImage(String path, byte[] key) {
        try {
            byte[] data = read(path, key);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            int sample = 1;
            while ((bounds.outWidth / sample) > 2600 || (bounds.outHeight / sample) > 2600) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void delete(String path) {
        if (path == null) return;
        //noinspection ResultOfMethodCallIgnored
        new File(path).delete();
    }

    public static void deleteAll(Context context) {
        File[] files = dir(context).listFiles();
        if (files == null) return;
        for (File f : files) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
