package com.mixia.vault;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.mixia.vault.model.Entry;
import com.mixia.vault.model.Vault;
import com.mixia.vault.util.AppDialog;
import com.mixia.vault.util.AppMenu;
import com.mixia.vault.util.ClipboardHelper;
import com.mixia.vault.util.GeneratorDialog;
import com.mixia.vault.util.PressFeedback;
import com.mixia.vault.util.Ui;

import java.util.ArrayList;
import java.util.List;

/** Main credential list. */
public class VaultActivity extends BaseActivity {
    private VaultApp app;
    private ListView list;
    private View empty;
    private EditText search;
    private LinearLayout chips;
    private TextView subtitle;
    private EntryAdapter adapter;
    private String category = "全部";
    private final List<TextView> chipViews = new ArrayList<>();
    private final List<String> chipNames = new ArrayList<>();
    private android.app.Dialog currentSheet;

    private static final String[] SORT_LABELS = {"最近更新", "最近创建", "标题 A→Z", "按分类"};
    private static final String[] SORT_VALUES = {
            Vault.SORT_UPDATED, Vault.SORT_CREATED, Vault.SORT_TITLE, Vault.SORT_CATEGORY};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);
        app = VaultApp.get(this);

        list = findViewById(R.id.list);
        empty = findViewById(R.id.emptyView);
        search = findViewById(R.id.searchInput);
        chips = findViewById(R.id.chips);
        subtitle = findViewById(R.id.subtitleText);

        adapter = new EntryAdapter(this);
        list.setAdapter(adapter);
        // 单击：只显示密码
        list.setOnItemClickListener((parent, view, position, id) ->
                QuickPasswordDialog.show(this, adapter.getItem(position)));
        // 长按：才出现编辑 / 删除
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            showEntryMenu(adapter.getItem(position));
            return true;
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh();
            }
        });

        findViewById(R.id.fab).setOnClickListener(v ->
                startActivity(new Intent(this, EntryEditActivity.class)));
        findViewById(R.id.btnMenu).setOnClickListener(this::showMenu);
        findViewById(R.id.btnSort).setOnClickListener(v -> showSort());
        PressFeedback.attach(findViewById(R.id.fab));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (app.isUnlocked()) {
            buildChips();
            refresh();
        }
    }

    /** Long-press menu for one record. */
    private void showEntryMenu(final Entry entry) {
        AppMenu.show(this,
                new AppMenu.Item(R.drawable.ic_edit, "编辑这条记录", "修改标题、账号、密码、照片",
                        () -> EntryEditActivity.open(this, entry.id)),
                new AppMenu.Item(R.drawable.ic_copy, "复制账号", entry.username,
                        () -> ClipboardHelper.copy(this, "账号", entry.username)),
                new AppMenu.Item(R.drawable.ic_copy, "复制密码", "复制后 45 秒自动清空",
                        () -> ClipboardHelper.copy(this, "密码", entry.password)),
                new AppMenu.Item(R.drawable.ic_photo, "查看详情", "全部字段与照片",
                        () -> EntryDetailDialog.show(this, entry, this::refresh)),
                new AppMenu.Item(R.drawable.ic_delete, "删除这条记录", null, true,
                        () -> confirmDelete(entry)));
    }

    private void confirmDelete(final Entry entry) {
        AppDialog.confirm(this, "删除记录",
                "「" + entry.title + "」及其照片将被永久删除，无法恢复。", "删除", true, () -> {
                    app.vault().removeEntry(entry.id);
                    app.saveAsync(this::refresh);
                    toast("已删除");
                });
    }

    private void showMenu(View anchor) {
        AppMenu.show(this,
                new AppMenu.Item(R.drawable.ic_wallet, "助记词保险箱", "BIP39 助记词与本地派生",
                        () -> startActivity(new Intent(this, MnemonicActivity.class))),
                new AppMenu.Item(R.drawable.ic_key, "密码生成器", "随机生成高强度密码",
                        () -> GeneratorDialog.show(this, null, null)),
                new AppMenu.Item(R.drawable.ic_category, "分类管理", "重命名、删除、调整顺序",
                        this::manageCategories),
                new AppMenu.Item(R.drawable.ic_settings, "设置", "自动锁定、备份、主密码",
                        () -> startActivity(new Intent(this, SettingsActivity.class))),
                new AppMenu.Item(R.drawable.ic_lock, "立即锁定", "清空内存中的明文", true, () -> {
                    app.lock();
                    Intent intent = new Intent(this, LockActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }));
    }

    private void showSort() {
        Vault vault = app.vault();
        int checked = 0;
        for (int i = 0; i < SORT_VALUES.length; i++) {
            if (SORT_VALUES[i].equals(vault.sortMode)) checked = i;
        }
        AppDialog.singleChoice(this, "排序方式", SORT_LABELS, checked, index -> {
            vault.sortMode = SORT_VALUES[index];
            app.saveAsync(this::refresh);
        });
    }

    private void buildChips() {
        chips.removeAllViews();
        chipViews.clear();
        chipNames.clear();
        List<String> names = new ArrayList<>();
        names.add("全部");
        names.add("收藏");
        names.addAll(app.vault().categoriesInOrder());

        for (final String name : names) {
            TextView chip = new TextView(this);
            chip.setText(name);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text, getTheme()));
            chip.setBackgroundResource(R.drawable.chip_bg);
            chip.setPadding(Ui.dp(this, 15), Ui.dp(this, 8), Ui.dp(this, 15), Ui.dp(this, 8));
            chip.setClickable(true);
            chip.setSelected(name.equals(category));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(this, 8));
            chip.setLayoutParams(lp);
            PressFeedback.attach(chip);
            chip.setOnClickListener(v -> {
                category = name;
                for (int i = 0; i < chipViews.size(); i++) {
                    chipViews.get(i).setSelected(chipNames.get(i).equals(category));
                }
                refresh();
            });
            chipViews.add(chip);
            chipNames.add(name);
            chips.addView(chip);
        }
    }

    private void refresh() {
        Vault vault = app.vault();
        if (vault == null) return;
        List<Entry> data = vault.filtered(search.getText().toString(), category);
        adapter.setData(data);
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        subtitle.setText(vault.entries.size() + " 条记录 · " + vault.wallets.size() + " 份助记词 · "
                + vault.sortLabel());
        for (int i = 0; i < chipViews.size(); i++) {
            String name = chipNames.get(i);
            int count = vault.countIn(name);
            chipViews.get(i).setText(name + (count > 0 ? " " + count : ""));
        }
    }

    /** Rename / reorder / delete categories. */
    private void manageCategories() {
        final Vault vault = app.vault();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < vault.categories.size(); i++) {
            final int index = i;
            final String name = vault.categories.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.choice_row_bg);
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));

            TextView label = new TextView(this);
            label.setText(name);
            label.setTextSize(15);
            label.setTextColor(0xFFE9EEF8);
            row.addView(label, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            row.addView(iconButton(R.drawable.ic_updown, 0xFF8C97AC, v -> {
                if (index > 0) {
                    vault.moveCategory(index, index - 1);
                    app.saveAsync(null);
                    reopenSheet();
                }
            }));
            row.addView(iconButton(R.drawable.ic_edit_small, 0xFF4EC9B0, v ->
                    AppDialog.input(this, "重命名分类", "分类名称", name, false, value -> {
                        if (vault.renameCategory(name, value)) {
                            if (category.equals(name)) category = value.trim();
                            app.saveAsync(null);
                            reopenSheet();
                        } else {
                            toast("名称重复或为空");
                        }
                    })));
            row.addView(iconButton(R.drawable.ic_delete, 0xFFEF5B6B, v ->
                    AppDialog.confirm(this, "删除分类",
                            "「" + name + "」下的记录会移到「" + vault.categories.get(0) + "」。",
                            "删除", true, () -> {
                                vault.removeCategory(name);
                                if (category.equals(name)) category = "全部";
                                app.saveAsync(null);
                                reopenSheet();
                            })));

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = Ui.dp(this, 8);
            panel.addView(row, rlp);
        }

        TextView add = new TextView(this);
        add.setText("＋ 新建分类");
        add.setTextSize(15);
        add.setGravity(Gravity.CENTER);
        add.setTextColor(0xFF4EC9B0);
        add.setBackgroundResource(R.drawable.btn_ghost);
        add.setClickable(true);
        add.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13));
        PressFeedback.attach(add);
        add.setOnClickListener(v -> AppDialog.input(this, "新建分类", "分类名称", "", false, value -> {
            String name = value.trim();
            if (name.isEmpty()) return;
            if (vault.categories.contains(name)) {
                toast("分类已存在");
                return;
            }
            vault.categories.add(name);
            app.saveAsync(null);
            reopenSheet();
        }));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        alp.topMargin = Ui.dp(this, 6);
        panel.addView(add, alp);

        currentSheet = new AppDialog.Builder(this)
                .title("分类管理")
                .message("上下箭头调整顺序，顺序会影响首页标签与「按分类」排序。")
                .content(panel)
                .hideNegative()
                .positive("完成", null)
                .show();
    }

    /** Closes the current sheet and reopens it with fresh data. */
    private void reopenSheet() {
        if (currentSheet != null) {
            currentSheet.dismiss();
            currentSheet = null;
        }
        buildChips();
        refresh();
        manageCategories();
    }

    private View iconButton(int res, int color, View.OnClickListener listener) {
        IconButton button = new IconButton(this, res, color);
        PressFeedback.attach(button);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34));
        lp.setMarginStart(Ui.dp(this, 6));
        button.setLayoutParams(lp);
        return button;
    }

    /** Small helper so the small icon buttons look identical everywhere. */
    private static class IconButton extends android.widget.ImageButton {
        IconButton(android.content.Context context, int res, int color) {
            super(context);
            setBackgroundResource(android.R.color.transparent);
            setImageResource(res);
            setColorFilter(color);
            setPadding(Ui.dp(context, 7), Ui.dp(context, 7), Ui.dp(context, 7), Ui.dp(context, 7));
        }
    }
}
