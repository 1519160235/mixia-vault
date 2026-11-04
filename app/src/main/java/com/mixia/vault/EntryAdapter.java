package com.mixia.vault;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mixia.vault.model.Entry;
import com.mixia.vault.util.Ui;

import java.util.ArrayList;
import java.util.List;

/** List adapter for credentials. */
public class EntryAdapter extends BaseAdapter {
    private final List<Entry> data = new ArrayList<>();
    private final LayoutInflater inflater;
    private final Context context;

    public EntryAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setData(List<Entry> entries) {
        data.clear();
        data.addAll(entries);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Entry getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_entry, parent, false);
            holder = new ViewHolder();
            holder.avatar = convertView.findViewById(R.id.avatarText);
            holder.title = convertView.findViewById(R.id.titleText);
            holder.subtitle = convertView.findViewById(R.id.subtitleText);
            holder.category = convertView.findViewById(R.id.categoryText);
            holder.star = convertView.findViewById(R.id.starIcon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Entry entry = data.get(position);
        holder.avatar.setText(Ui.initial(entry.title));
        holder.avatar.setBackgroundTintList(ColorStateList.valueOf(Ui.avatarColor(entry.title)));
        holder.title.setText(entry.title);
        holder.subtitle.setText(entry.subtitle());
        holder.category.setText(entry.category);
        holder.star.setVisibility(entry.favorite ? View.VISIBLE : View.GONE);
        return convertView;
    }

    private static class ViewHolder {
        TextView avatar;
        TextView title;
        TextView subtitle;
        TextView category;
        ImageView star;
    }
}
