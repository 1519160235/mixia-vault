package com.mixia.vault.util;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mixia.vault.R;
import com.mixia.vault.VaultApp;
import com.mixia.vault.data.PhotoStore;

/** Full screen photo viewer for encrypted attachments. */
public final class PhotoViewer {

    private PhotoViewer() {
    }

    public static void show(final Activity activity, final String path, final int index, final int total) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_photo);

        final ImageView image = dialog.findViewById(R.id.photoImage);
        TextView counter = dialog.findViewById(R.id.photoCounter);
        final View progress = dialog.findViewById(R.id.photoProgress);

        counter.setText((index + 1) + " / " + total);
        image.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            byte[] key = VaultApp.get(activity).vaultFile() == null
                    ? null : VaultApp.get(activity).vaultFile().key();
            final Bitmap bitmap = key == null ? null : PhotoStore.fullImage(path, key);
            activity.runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (bitmap == null) {
                    image.setVisibility(View.GONE);
                    counter.setText("图片无法解码");
                } else {
                    image.setImageBitmap(bitmap);
                    image.setVisibility(View.VISIBLE);
                }
            });
        }, "photo-load").start();

        dialog.findViewById(R.id.photoClose).setOnClickListener(v -> dialog.dismiss());
        image.setOnClickListener(v -> dialog.dismiss());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#EE000000")));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 1f;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    /** Small square thumbnail view used by the editor and the detail sheet. */
    public static ImageView thumb(Activity activity, int size) {
        ImageView view = new ImageView(activity);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setBackgroundResource(R.drawable.photo_thumb_bg);
        view.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return view;
    }
}
