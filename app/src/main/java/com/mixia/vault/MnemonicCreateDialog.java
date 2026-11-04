package com.mixia.vault;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mixia.vault.core.Bip39;
import com.mixia.vault.model.Wallet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Create a new mnemonic (generate or import). */
public final class MnemonicCreateDialog {

    private MnemonicCreateDialog() {
    }

    public static void show(final BaseActivity activity, final Runnable onDone) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mnemonic_create);

        final EditText title = dialog.findViewById(R.id.wTitleInput);
        final EditText importInput = dialog.findViewById(R.id.importInput);
        final TextView error = dialog.findViewById(R.id.importError);

        dialog.findViewById(R.id.btnGen12).setOnClickListener(v -> {
            if (!Bip39.isLoaded()) {
                error.setText("词库仍在加载，请稍后再试");
                error.setVisibility(TextView.VISIBLE);
                return;
            }
            save(activity, dialog, title.getText().toString(), Bip39.generate(12), onDone);
        });
        dialog.findViewById(R.id.btnGen24).setOnClickListener(v -> {
            if (!Bip39.isLoaded()) {
                error.setText("词库仍在加载，请稍后再试");
                error.setVisibility(TextView.VISIBLE);
                return;
            }
            save(activity, dialog, title.getText().toString(), Bip39.generate(24), onDone);
        });
        dialog.findViewById(R.id.btnImport).setOnClickListener(v -> {
            String text = importInput.getText().toString();
            if (!Bip39.isLoaded()) {
                error.setText("词库仍在加载，请稍后再试");
                error.setVisibility(TextView.VISIBLE);
                return;
            }
            if (!Bip39.validate(text)) {
                error.setText("助记词无效：请检查单词拼写、数量和校验和");
                error.setVisibility(TextView.VISIBLE);
                return;
            }
            save(activity, dialog, title.getText().toString(), Bip39.normalize(text), onDone);
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.62f);
            window.setLayout(Math.min((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f), (int) (com.mixia.vault.util.Ui.dp(activity, 420))),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.CENTER);
        }
        dialog.show();
    }

    private static void save(BaseActivity activity, Dialog dialog, String title, String mnemonic, Runnable onDone) {
        VaultApp app = VaultApp.get(activity);
        Wallet wallet = new Wallet();
        wallet.id = System.currentTimeMillis() * 1000 + (long) (Math.random() * 1000);
        wallet.title = title.trim().isEmpty()
                ? "钱包 " + new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date())
                : title.trim();
        wallet.mnemonic = mnemonic;
        wallet.coin = "ETH";
        wallet.path = "m/44'/60'/0'/0/0";
        wallet.createdAt = System.currentTimeMillis();
        wallet.updatedAt = wallet.createdAt;
        app.vault().wallets.add(wallet);
        app.saveAsync(() -> {
            dialog.dismiss();
            if (onDone != null) onDone.run();
        });
        activity.toast("已加密保存");
    }
}


