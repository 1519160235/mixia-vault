package com.mixia.vault;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.mixia.vault.core.WalletDeriver;
import com.mixia.vault.model.Wallet;
import com.mixia.vault.util.AppDialog;
import com.mixia.vault.util.ClipboardHelper;

/** Detail view for one mnemonic: words, path, derived address and keys. */
public class MnemonicDetailActivity extends BaseActivity {
    private static final String[] COIN_LABELS = {"以太坊 ETH", "比特币 BTC（Legacy）", "比特币（隔离见证）", "自定义路径"};
    private static final String[] COIN_PATHS = {"m/44'/60'/0'/0/0", "m/44'/0'/0'/0/0", "m/84'/0'/0'/0/0", ""};

    private VaultApp app;
    private Wallet wallet;
    private TextView wordsText;
    private TextView addrText;
    private TextView privText;
    private TextView wifText;
    private TextView wifLabel;
    private TextView status;
    private EditText pathInput;
    private EditText passInput;
    private Spinner coinSpinner;
    private Button btnReveal;
    private boolean revealed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean initialising = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mnemonic_detail);
        app = VaultApp.get(this);
        long id = getIntent().getLongExtra("wallet_id", 0);
        wallet = app.vault() == null ? null : app.vault().findWallet(id);
        if (wallet == null) {
            finish();
            return;
        }

        wordsText = findViewById(R.id.wordsText);
        addrText = findViewById(R.id.addrText);
        privText = findViewById(R.id.privText);
        wifText = findViewById(R.id.wifText);
        wifLabel = findViewById(R.id.wifLabel);
        status = findViewById(R.id.statusText);
        pathInput = findViewById(R.id.pathInput);
        passInput = findViewById(R.id.passInput);
        coinSpinner = findViewById(R.id.coinSpinner);
        btnReveal = findViewById(R.id.btnRevealWords);

        ((TextView) findViewById(R.id.detailTitle)).setText(wallet.title);
        wordsText.setText(maskedWords(wallet.wordCount()));
        pathInput.setText(wallet.path);
        passInput.setText(wallet.passphrase);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, COIN_LABELS);
        coinSpinner.setAdapter(spinnerAdapter);
        coinSpinner.setSelection(coinIndex(wallet.coin), false);
        coinSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (initialising) return;
                if (position < 3) {
                    pathInput.setText(COIN_PATHS[position]);
                }
                derive();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.btnBackD).setOnClickListener(v -> finish());
        findViewById(R.id.btnDerive).setOnClickListener(v -> derive());
        btnReveal.setOnClickListener(v -> toggleReveal());
        findViewById(R.id.btnCopyWords).setOnClickListener(v -> copyMnemonic());
        findViewById(R.id.btnCopyPriv).setOnClickListener(v ->
                ClipboardHelper.copy(this, "私钥", privText.getText().toString()));
        findViewById(R.id.btnDeleteWallet).setOnClickListener(v -> confirmDelete());

        initialising = false;
        derive();
    }

    private int coinIndex(String coin) {
        if ("BTC".equals(coin)) return 1;
        if ("BTC_SEGWIT".equals(coin)) return 2;
        if ("CUSTOM".equals(coin)) return 3;
        return 0;
    }

    private WalletDeriver.Coin coinFor(int position) {
        switch (position) {
            case 1:
                return WalletDeriver.Coin.BTC;
            case 2:
                return WalletDeriver.Coin.BTC_SEGWIT;
            default:
                return WalletDeriver.Coin.ETH;
        }
    }

    private void toggleReveal() {
        revealed = !revealed;
        if (revealed) {
            wordsText.setText(numberedWords(wallet.mnemonic));
            btnReveal.setText("隐藏助记词");
        } else {
            wordsText.setText(maskedWords(wallet.wordCount()));
            btnReveal.setText("显示助记词");
        }
    }

    private String maskedWords(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("••••");
            if (i < count - 1) sb.append("   ");
        }
        return sb.toString();
    }

    private String numberedWords(String mnemonic) {
        String[] parts = mnemonic.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i += 2) {
            sb.append(String.format(java.util.Locale.ROOT, "%2d  %-14s", i + 1, parts[i]));
            if (i + 1 < parts.length) {
                sb.append(String.format(java.util.Locale.ROOT, "%2d  %s", i + 2, parts[i + 1]));
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private void copyMnemonic() {
        AppDialog.confirm(this, "复制助记词",
                "剪贴板可能被输入法或其他应用读取。更安全的做法是抄写在纸上。",
                "仍要复制", false, () -> ClipboardHelper.copy(this, "助记词", wallet.mnemonic));
    }

    private void derive() {
        final String path = pathInput.getText().toString().trim();
        final String passphrase = passInput.getText().toString();
        final int position = coinSpinner.getSelectedItemPosition();
        if (path.isEmpty()) {
            status.setText("请输入派生路径");
            return;
        }
        status.setText("正在本机计算…");
        findViewById(R.id.btnDerive).setEnabled(false);
        addrText.setText("—");
        privText.setText("—");

        new Thread(() -> {
            try {
                final WalletDeriver.Result result = WalletDeriver.derive(wallet.mnemonic, passphrase, path,
                        coinFor(position));
                handler.post(() -> showResult(result, position));
            } catch (final Exception e) {
                handler.post(() -> {
                    status.setText("派生失败：" + e.getMessage());
                    findViewById(R.id.btnDerive).setEnabled(true);
                });
            }
        }, "derive-wallet").start();
    }

    private void showResult(WalletDeriver.Result result, int position) {
        addrText.setText(result.address);
        privText.setText(result.privateKeyHex);
        boolean btc = position == 1 || position == 2;
        wifLabel.setVisibility(btc ? View.VISIBLE : View.GONE);
        wifText.setVisibility(btc ? View.VISIBLE : View.GONE);
        wifText.setText(result.wif);
        status.setText("路径 " + result.path + " · 已在本地派生完成");
        findViewById(R.id.btnDerive).setEnabled(true);

        wallet.path = result.path;
        wallet.passphrase = passInput.getText().toString();
        wallet.coin = position == 1 ? "BTC" : position == 2 ? "BTC_SEGWIT" : position == 3 ? "CUSTOM" : "ETH";
        wallet.updatedAt = System.currentTimeMillis();
        app.saveAsync(null);
    }

    private void confirmDelete() {
        AppDialog.confirm(this, "删除助记词",
                "删除后只能靠你自己抄写的备份恢复。确定删除「" + wallet.title + "」？", "删除", true, () -> {
                    app.vault().removeWallet(wallet.id);
                    app.saveAsync(this::finish);
                });
    }
}

