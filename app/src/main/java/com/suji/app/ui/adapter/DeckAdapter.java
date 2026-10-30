package com.suji.app.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suji.app.R;
import com.suji.app.data.Deck;
import com.suji.app.data.DeckStat;

import java.util.ArrayList;
import java.util.List;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.VH> {

    public interface Listener {
        void onClick(Deck deck);

        void onLongClick(Deck deck);
    }

    private static final int[] PALETTE = {
            0xFF5B5BD6, 0xFFF59E0B, 0xFF10B981, 0xFFEC4899, 0xFF06B6D4, 0xFF8B5CF6
    };

    private final List<DeckStat> items = new ArrayList<>();
    private final Listener listener;

    public DeckAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<DeckStat> stats) {
        items.clear();
        if (stats != null) {
            items.addAll(stats);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DeckStat stat = items.get(position);
        final Deck deck = stat.deck;

        holder.name.setText(deck.name);

        StringBuilder meta = new StringBuilder();
        meta.append(stat.total).append(" 张");
        if (stat.dueCount > 0) {
            meta.append(" · 待复习 ").append(stat.dueCount);
        } else if (stat.newCount > 0) {
            meta.append(" · 新卡 ").append(stat.newCount);
        }
        holder.meta.setText(meta.toString());

        int total = stat.dueCount + stat.newCount;
        if (total > 0) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText("今日 " + total);
        } else {
            holder.badge.setVisibility(View.GONE);
        }

        int color = deck.color != 0 ? deck.color : PALETTE[position % PALETTE.length];
        holder.dot.setBackgroundTintList(ColorStateList.valueOf(color));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(deck);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    listener.onLongClick(deck);
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final View dot;
        final TextView name;
        final TextView meta;
        final TextView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            dot = itemView.findViewById(R.id.dot);
            name = itemView.findViewById(R.id.name);
            meta = itemView.findViewById(R.id.meta);
            badge = itemView.findViewById(R.id.badge);
        }
    }
}
