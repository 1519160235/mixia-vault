package com.mixia.vault;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mixia.vault.util.PressFeedback;
import com.mixia.vault.util.Ui;

/** First-run disclaimer. Must be accepted before the vault can be created. */
public class DisclaimerActivity extends Activity {

    public static final String PREFS = "mixia";
    public static final String K_ACCEPTED = "disclaimer_accepted_v1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(K_ACCEPTED, false)) {
            goNext();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_app);
        int pad = Ui.dp(this, 22);
        root.setPadding(pad, Ui.dp(this, 28), pad, Ui.dp(this, 18));

        TextView title = new TextView(this);
        title.setText("使用前请阅读");
        title.setTextSize(22);
        title.setTextColor(0xFFE9EEF8);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("密匣是一个离线密码库，请先了解它的能力边界");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFF8C97AC);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 6);
        root.addView(subtitle, slp);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        TextView body = new TextView(this);
        body.setText(DISCLAIMER);
        body.setTextSize(13);
        body.setTextColor(0xFFC7D0DE);
        body.setLineSpacing(Ui.dp(this, 2), 1.25f);
        body.setMovementMethod(new ScrollingMovementMethod());
        int bp = Ui.dp(this, 15);
        body.setPadding(bp, bp, bp, bp);
        body.setBackgroundResource(R.drawable.card_bg);
        scroll.addView(body);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        clp.topMargin = Ui.dp(this, 16);
        root.addView(scroll, clp);

        TextView accept = new TextView(this);
        accept.setText("我已阅读并同意，承担保管风险");
        accept.setTextSize(16);
        accept.setTextColor(0xFF062B25);
        accept.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        accept.setGravity(Gravity.CENTER);
        accept.setBackgroundResource(R.drawable.btn_primary);
        accept.setClickable(true);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
        alp.topMargin = Ui.dp(this, 16);
        root.addView(accept, alp);
        PressFeedback.attach(accept);
        accept.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(K_ACCEPTED, true).apply();
            goNext();
        });

        TextView decline = new TextView(this);
        decline.setText("不同意并退出");
        decline.setTextSize(13);
        decline.setTextColor(0xFF6E7C91);
        decline.setGravity(Gravity.CENTER);
        decline.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 4));
        decline.setClickable(true);
        root.addView(decline);
        decline.setOnClickListener(v -> finishAffinity());

        setContentView(root);
    }

    private void goNext() {
        startActivity(new Intent(this, LockActivity.class));
        finish();
    }

    private static final String DISCLAIMER =
            "1. 主密码无法找回\n"
                    + "应用不保存、不上传主密码。忘记主密码 = 保险库数据永久无法解密，开发者无法恢复。\n\n"
                    + "2. 数据丢失风险\n"
                    + "卸载、清除数据、换机、设备损坏或丢失都可能导致数据丢失。请自行定期导出加密备份；备份不含照片，恢复时需要当时的主密码。\n\n"
                    + "3. 助记词由你全权负责\n"
                    + "助记词等同于钱包的全部所有权。泄露、抄错、丢失都可能造成资产永久损失。\n\n"
                    + "4. 剪贴板与输入法风险（重要）\n"
                    + "复制密码时，内容会进入系统剪贴板。剪贴板是系统级共享通道，当前获得焦点的输入法"
                    + "（如微信输入法、搜狗输入法、Gboard 等）可以合法读取该内容，并可能自动保存到它自己的"
                    + "「剪贴板历史」中。这些数据属于输入法的私有存储，本应用没有任何权限删除或访问。\n"
                    + "本应用能做的只有：把复制内容标记为敏感（Android 13+ 系统浮层不预览）、密码复制后"
                    + "最多 15 秒自动清空系统剪贴板、以及提供「清除系统剪贴板」按钮。\n"
                    + "如果你对此有顾虑，请：① 在输入法设置中关闭「剪贴板历史 / 云同步」；"
                    + "② 尽量使用「查看密码」后手动输入，而不是复制。\n\n"
                    + "5. 不联网 ≠ 绝对安全\n"
                    + "本应用未申请 INTERNET 权限，但无法防御已 root 的设备、恶意系统组件、屏幕录制、无障碍服务、键盘记录器或物理接触攻击。\n\n"
                    + "6. 使用范围\n"
                    + "本应用为个人自用工具，按「现状」提供，不附带任何担保；不适合企业密钥管理，也不适合存放无法承受丢失风险的资产。\n\n"
                    + "完整条款见仓库 DISCLAIMER.md。使用本应用即表示你已理解并接受上述风险。";
}
