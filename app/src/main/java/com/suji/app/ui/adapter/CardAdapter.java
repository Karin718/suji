package com.suji.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suji.app.R;
import com.suji.app.core.Scheduler;
import com.suji.app.data.Card;

import java.util.ArrayList;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.VH> {

    public interface Listener {
        void onClick(Card card);

        void onLongClick(Card card);
    }

    private final List<Card> all = new ArrayList<>();
    private final List<Card> shown = new ArrayList<>();
    private final Listener listener;
    private long[] intervals = new long[]{5, 30};
    private String query = "";

    public CardAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Card> cards, long[] intervals) {
        all.clear();
        if (cards != null) {
            all.addAll(cards);
        }
        if (intervals != null && intervals.length > 0) {
            this.intervals = intervals;
        }
        applyFilter();
    }

    public void filter(String q) {
        query = q == null ? "" : q.trim().toLowerCase();
        applyFilter();
    }

    private void applyFilter() {
        shown.clear();
        if (query.isEmpty()) {
            shown.addAll(all);
        } else {
            for (Card c : all) {
                String text = (c.front + " " + c.back + " " + c.note + " " + c.tags).toLowerCase();
                if (text.contains(query)) {
                    shown.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final Card card = shown.get(position);
        holder.front.setText(card.front);
        holder.back.setText(card.back);
        if (card.back == null || card.back.trim().isEmpty()) {
            holder.back.setVisibility(View.GONE);
        } else {
            holder.back.setVisibility(View.VISIBLE);
        }
        long now = System.currentTimeMillis();
        String status = Scheduler.statusText(card.stage, card.reviews, card.dueAt, intervals, now);
        holder.status.setText(status);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(card);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    listener.onLongClick(card);
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView front;
        final TextView back;
        final TextView status;

        VH(@NonNull View itemView) {
            super(itemView);
            front = itemView.findViewById(R.id.front);
            back = itemView.findViewById(R.id.back);
            status = itemView.findViewById(R.id.status);
        }
    }
}
