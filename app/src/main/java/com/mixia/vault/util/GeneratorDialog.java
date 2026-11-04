package com.mixia.vault.util;

import android.app.Activity;
import android.app.Dialog;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mixia.vault.R;

/** The shared password generator dialog. */
public final class GeneratorDialog {

    public interface Listener {
        void onUse(String password);
    }

    private GeneratorDialog() {
    }

    public static void show(final Activity activity, final String initial, final Listener listener) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_generator);

        final TextView output = dialog.findViewById(R.id.genPassword);
        final TextView strength = dialog.findViewById(R.id.genStrength);
        final TextView lengthLabel = dialog.findViewById(R.id.genLengthLabel);
        final SeekBar length = dialog.findViewById(R.id.genLength);
        final CheckBox upper = dialog.findViewById(R.id.cbUpper);
        final CheckBox lower = dialog.findViewById(R.id.cbLower);
        final CheckBox digits = dialog.findViewById(R.id.cbDigits);
        final CheckBox symbols = dialog.findViewById(R.id.cbSymbols);
        final CheckBox ambiguous = dialog.findViewById(R.id.cbAmbiguous);

        length.setMax(40);
        length.setProgress(12); // 8 + 12 = 20

        final Runnable generate = () -> {
            int len = 8 + length.getProgress();
            lengthLabel.setText("长度 " + len);
            String pw = PasswordTools.generate(len, upper.isChecked(), lower.isChecked(),
                    digits.isChecked(), symbols.isChecked(), ambiguous.isChecked());
            output.setText(pw);
            strength.setText(PasswordTools.strengthLabel(pw) + " · " + PasswordTools.entropyText(pw));
            strength.setTextColor(PasswordTools.strengthColor(pw));
        };

        length.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                generate.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        CheckBox.OnCheckedChangeListener reload = (buttonView, isChecked) -> generate.run();
        upper.setOnCheckedChangeListener(reload);
        lower.setOnCheckedChangeListener(reload);
        digits.setOnCheckedChangeListener(reload);
        symbols.setOnCheckedChangeListener(reload);
        ambiguous.setOnCheckedChangeListener(reload);

        if (initial != null && !initial.isEmpty()) {
            output.setText(initial);
            strength.setText(PasswordTools.strengthLabel(initial) + " · " + PasswordTools.entropyText(initial));
            strength.setTextColor(PasswordTools.strengthColor(initial));
        } else {
            generate.run();
        }

        dialog.findViewById(R.id.btnRegen).setOnClickListener(v -> generate.run());

        // 主按钮：编辑页调用时填入并复制；首页直接打开时只复制
        Button use = dialog.findViewById(R.id.btnCopyGen);
        use.setText(listener != null ? "使用这个密码" : "复制密码");
        use.setOnClickListener(v -> {
            String value = output.getText().toString();
            if (value.isEmpty()) return;
            ClipboardHelper.copy(activity, "密码", value);
            if (listener != null) listener.onUse(value);
            dialog.dismiss();
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
}

