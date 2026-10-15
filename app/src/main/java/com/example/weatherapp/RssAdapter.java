package com.example.weatherapp;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RssAdapter extends RecyclerView.Adapter<RssAdapter.VH> {
    public interface OnItemClick { void onClick(RssItem item); }

    private final List<RssItem> items = new ArrayList<>();
    private final OnItemClick listener;

    public RssAdapter(OnItemClick l) { listener = l; }

    public void set(List<RssItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rss, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        RssItem it = items.get(pos);
        h.title.setText(it.title == null ? "(no title)" : it.title);
        String d = it.description == null ? ""
                : HtmlCompat.fromHtml(it.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim();
        h.desc.setText(d);
        h.desc.setVisibility(d.isEmpty() ? View.GONE : View.VISIBLE);
        h.date.setText(it.pubDate == null ? "" : it.pubDate);
        h.itemView.setOnClickListener(v -> listener.onClick(it));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, desc, date;
        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvRssTitle);
            desc = v.findViewById(R.id.tvRssDesc);
            date = v.findViewById(R.id.tvRssDate);
        }
    }
}
