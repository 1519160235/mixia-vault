package com.mixia.vault;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mixia.vault.model.Wallet;
import com.mixia.vault.util.AppMenu;
import com.mixia.vault.util.Ui;

/** List of stored mnemonics. */
public class MnemonicActivity extends BaseActivity {
    private VaultApp app;
    private ListView list;
    private View empty;
    private WalletAdapter adapter;
    private TextView subtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mnemonic);
        app = VaultApp.get(this);
        list = findViewById(R.id.mnemonicList);
        empty = findViewById(R.id.mnemonicEmpty);
        subtitle = findViewById(R.id.mnemonicSubtitle);

        adapter = new WalletAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, MnemonicDetailActivity.class);
            intent.putExtra("wallet_id", adapter.getItem(position).id);
            startActivity(intent);
        });
        findViewById(R.id.btnBackM).setOnClickListener(v -> finish());
        findViewById(R.id.fabMnemonic).setOnClickListener(v ->
                MnemonicCreateDialog.show(this, this::refresh));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (app.isUnlocked()) refresh();
    }

    private void refresh() {
        if (app.vault() == null) return;
        adapter.setData(app.vault().wallets);
        empty.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        subtitle.setText(adapter.getCount() + " 份助记词 · 全部本地加密");
    }

    private class WalletAdapter extends BaseAdapter {
        private java.util.List<Wallet> data = new java.util.ArrayList<>();

        void setData(java.util.List<Wallet> wallets) {
            data = wallets;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Wallet getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return data.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_wallet, parent, false);
            }
            Wallet w = getItem(position);
            TextView avatar = convertView.findViewById(R.id.wAvatar);
            TextView title = convertView.findViewById(R.id.wTitle);
            TextView sub = convertView.findViewById(R.id.wSub);
            avatar.setText("₿");
            avatar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.avatarColor(w.title)));
            title.setText(w.title);
            sub.setText(w.wordCount() + " 个单词 · " + w.coin + " · " + w.path);
            return convertView;
        }
    }
}
