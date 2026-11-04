package com.mixia.vault;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.mixia.vault.data.PhotoStore;
import com.mixia.vault.model.Entry;
import com.mixia.vault.util.AppDialog;
import com.mixia.vault.util.GeneratorLauncher;
import com.mixia.vault.util.PasswordTools;
import com.mixia.vault.util.PhotoViewer;
import com.mixia.vault.util.Ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Create / edit a credential. */
public class EntryEditActivity extends BaseActivity {
    private static final String EXTRA_ID = "entry_id";
    private static final int REQ_PHOTO = 2001;
    private VaultApp app;
    private Entry entry;
    private long entryId;
    private EditText inputTitle;
    private EditText inputUsername;
    private EditText inputPassword;
    private EditText inputUrl;
    private EditText inputTags;
    private EditText inputNotes;
    private Switch switchFavorite;
    private TextView strengthText;
    private LinearLayout photoRow;
    private boolean passwordVisible;
    private String category = "网站";
    private final List<TextView> chips = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final List<String> photos = new ArrayList<>();
    private final List<String> pendingPhotos = new ArrayList<>();
    private boolean saved;

    public static void open(android.content.Context context, long id) {
        Intent intent = new Intent(context, EntryEditActivity.class);
        intent.putExtra(EXTRA_ID, id);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        app = VaultApp.get(this);
        entryId = getIntent().getLongExtra(EXTRA_ID, 0);
        entry = entryId > 0 && app.vault() != null ? app.vault().findEntry(entryId) : null;

        inputTitle = findViewById(R.id.inputTitle);
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        inputUrl = findViewById(R.id.inputUrl);
        inputTags = findViewById(R.id.inputTags);
        inputNotes = findViewById(R.id.inputNotes);
        switchFavorite = findViewById(R.id.switchFavorite);
        strengthText = findViewById(R.id.strengthText);
        photoRow = findViewById(R.id.photoRow);

        TextView header = findViewById(R.id.headerTitle);
        header.setText(entry == null ? "新建记录" : "编辑记录");

        buildCategoryChips();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnGenerate).setOnClickListener(v ->
                GeneratorLauncher.show(this, inputPassword.getText().toString(), value -> {
                    inputPassword.setText(value);
                    updateStrength();
                }));
        ImageButton toggle = findViewById(R.id.btnTogglePassword);
        toggle.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            inputPassword.setInputType(passwordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            inputPassword.setSelection(inputPassword.getText().length());
            toggle.setImageResource(passwordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
        });

        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateStrength();
            }
        });

        if (entry != null) {
            inputTitle.setText(entry.title);
            inputUsername.setText(entry.username);
            inputPassword.setText(entry.password);
            inputUrl.setText(entry.url);
            inputTags.setText(entry.tags);
            inputNotes.setText(entry.notes);
            switchFavorite.setChecked(entry.favorite);
            category = entry.category;
            photos.addAll(entry.photos);
            TextView delete = findViewById(R.id.btnDelete);
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(v -> confirmDelete());
            com.mixia.vault.util.PressFeedback.attach(delete);
        }
        syncChips();
        updateStrength();
        renderPhotos();
    }

    // ---------- category chips ----------

    private void buildCategoryChips() {
        LinearLayout row = findViewById(R.id.categoryRow);
        row.removeAllViews();
        chips.clear();
        names.clear();
        List<String> list = new ArrayList<>(app.vault().categoriesInOrder());
        if (!list.contains(category)) list.add(category);

        for (final String name : list) {
            TextView chip = new TextView(this);
            chip.setText(name);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text, getTheme()));
            chip.setBackgroundResource(R.drawable.chip_bg);
            chip.setPadding(Ui.dp(this, 15), Ui.dp(this, 8), Ui.dp(this, 15), Ui.dp(this, 8));
            chip.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(this, 8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                category = name;
                syncChips();
            });
            chips.add(chip);
            names.add(name);
            row.addView(chip);
        }

        TextView add = new TextView(this);
        add.setText("＋ 新分类");
        add.setTextSize(13);
        add.setGravity(Gravity.CENTER);
        add.setTextColor(0xFF4EC9B0);
        add.setBackgroundResource(R.drawable.chip_bg);
        add.setPadding(Ui.dp(this, 15), Ui.dp(this, 8), Ui.dp(this, 15), Ui.dp(this, 8));
        add.setClickable(true);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        add.setLayoutParams(alp);
        add.setOnClickListener(v -> AppDialog.input(this, "新建分类", "分类名称", "", false, value -> {
            String name = value.trim();
            if (name.isEmpty()) return;
            if (!app.vault().categories.contains(name)) {
                app.vault().categories.add(name);
            }
            category = name;
            app.saveAsync(null);
            buildCategoryChips();
            syncChips();
        }));
        row.addView(add);
    }

    private void syncChips() {
        for (int i = 0; i < chips.size(); i++) {
            chips.get(i).setSelected(names.get(i).equals(category));
        }
    }

    // ---------- photos ----------

    private void renderPhotos() {
        photoRow.removeAllViews();
        int size = Ui.dp(this, 84);
        for (int i = 0; i < photos.size(); i++) {
            final int index = i;
            final String path = photos.get(i);
            FrameLayout wrap = new FrameLayout(this);
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(size, size);
            wlp.setMarginEnd(Ui.dp(this, 10));
            wrap.setLayoutParams(wlp);

            ImageView thumb = new ImageView(this);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundResource(R.drawable.photo_thumb_bg);
            wrap.addView(thumb, new FrameLayout.LayoutParams(size, size));

            new Thread(() -> {
                byte[] key = app.vaultFile() == null ? null : app.vaultFile().key();
                final Bitmap bitmap = key == null ? null : PhotoStore.thumbnail(path, key, 320);
                runOnUiThread(() -> {
                    if (bitmap != null) thumb.setImageBitmap(bitmap);
                });
            }, "thumb").start();

            thumb.setOnClickListener(v -> PhotoViewer.show(this, path, index, photos.size()));

            ImageView remove = new ImageView(this);
            remove.setImageResource(R.drawable.ic_close);
            remove.setColorFilter(0xFFFFFFFF);
            remove.setBackgroundResource(R.drawable.remove_badge_bg);
            int rs = Ui.dp(this, 22);
            FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(rs, rs);
            rlp.gravity = Gravity.TOP | Gravity.END;
            rlp.topMargin = Ui.dp(this, 4);
            rlp.rightMargin = Ui.dp(this, 4);
            remove.setPadding(Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5));
            wrap.addView(remove, rlp);
            remove.setOnClickListener(v -> {
                photos.remove(index);
                if (pendingPhotos.remove(path)) PhotoStore.delete(path);
                renderPhotos();
            });

            photoRow.addView(wrap);
        }

        FrameLayout add = new FrameLayout(this);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(size, size);
        alp.setMarginEnd(Ui.dp(this, 10));
        add.setLayoutParams(alp);
        add.setBackgroundResource(R.drawable.photo_add_bg);
        add.setClickable(true);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_photo);
        icon.setColorFilter(0xFF6E7C91);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(Ui.dp(this, 26), Ui.dp(this, 26));
        ilp.gravity = Gravity.CENTER;
        add.addView(icon, ilp);
        add.setOnClickListener(v -> pickPhoto());
        photoRow.addView(add);
    }

    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择照片"), REQ_PHOTO);
        } catch (Exception e) {
            toast("没有可用的图片选择器");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PHOTO || resultCode != RESULT_OK || data == null) return;
        final List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;
        toast("正在加密保存…");
        new Thread(() -> {
            final List<String> added = new ArrayList<>();
            String error = null;
            byte[] key = app.vaultFile() == null ? null : app.vaultFile().key();
            if (key == null) error = "保险库未解锁";
            if (error == null) {
                for (Uri uri : uris) {
                    try {
                        InputStream in = getContentResolver().openInputStream(uri);
                        if (in == null) continue;
                        added.add(PhotoStore.save(this, key, in));
                    } catch (Exception e) {
                        error = e.getMessage();
                    }
                }
            }
            final String finalError = error;
            runOnUiThread(() -> {
                photos.addAll(added);
                pendingPhotos.addAll(added);
                renderPhotos();
                if (finalError != null) toast("部分照片保存失败：" + finalError);
            });
        }, "photo-save").start();
    }

    // ---------- misc ----------

    private void updateStrength() {
        String pw = inputPassword.getText().toString();
        if (pw.isEmpty()) {
            strengthText.setText("建议使用 16 位以上随机密码");
            strengthText.setTextColor(0xFF5E6A7E);
            return;
        }
        strengthText.setText("强度：" + PasswordTools.strengthLabel(pw) + " · " + PasswordTools.entropyText(pw));
        strengthText.setTextColor(PasswordTools.strengthColor(pw));
    }

    private void save() {
        String title = inputTitle.getText().toString().trim();
        if (title.isEmpty()) {
            toast("请填写标题");
            return;
        }
        if (entry == null) {
            entry = new Entry();
            entry.id = System.currentTimeMillis() * 1000 + (long) (Math.random() * 1000);
            entry.createdAt = System.currentTimeMillis();
            app.vault().entries.add(entry);
        }
        entry.title = title;
        entry.username = inputUsername.getText().toString().trim();
        entry.password = inputPassword.getText().toString();
        entry.url = inputUrl.getText().toString().trim();
        entry.tags = inputTags.getText().toString().trim();
        entry.notes = inputNotes.getText().toString().trim();
        entry.category = category;
        entry.favorite = switchFavorite.isChecked();
        entry.updatedAt = System.currentTimeMillis();
        entry.photos.clear();
        entry.photos.addAll(photos);
        saved = true;
        app.saveAsync(this::finish);
    }

    /** Drop encrypted copies that were added but never saved. */
    private void discardPending() {
        if (saved) return;
        for (String path : pendingPhotos) PhotoStore.delete(path);
        pendingPhotos.clear();
    }

    @Override
    protected void onDestroy() {
        discardPending();
        super.onDestroy();
    }

    private void confirmDelete() {
        AppDialog.confirm(this, "删除记录",
                "「" + entry.title + "」及其照片将被永久删除，无法恢复。", "删除", true, () -> {
                    for (String path : entry.photos) PhotoStore.delete(path);
                    app.vault().removeEntry(entry.id);
                    saved = true;
                    app.saveAsync(this::finish);
                });
    }
}
