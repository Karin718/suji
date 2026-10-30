package com.suji.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suji.app.R;
import com.suji.app.data.Card;

import java.util.ArrayList;
import java.util.List;

/** 导入预览：勾选 + 就地编辑。 */
public class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.VH> {

    public interface Listener {
        void onEdit(int position);
    }

    private final List<Card> items = new ArrayList<>();
    private final List<Boolean> checked = new ArrayList<>();
    private final Listener listener;

    public PreviewAdapter(List<Card> cards, Listener listener) {
        this.listener = listener;
        if (cards != null) {
            for (Card c : cards) {
                items.add(c);
                checked.add(Boolean.TRUE);
            }
        }
    }

    public int selectedCount() {
        int n = 0;
        for (Boolean b : checked) {
            if (b != null && b) {
                n++;
            }
        }
        return n;
    }

    public List<Card> selected() {
        List<Card> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (checked.get(i)) {
                out.add(items.get(i));
            }
        }
        return out;
    }

    public Card get(int position) {
        return items.get(position);
    }

    public void selectAll(boolean value) {
        for (int i = 0; i < checked.size(); i++) {
            checked.set(i, value);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preview, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final VH holder, final int position) {
        Card card = items.get(position);
        holder.front.setText(card.front);
        holder.back.setText(card.back);
        holder.back.setVisibility(card.back.trim().isEmpty() ? View.GONE : View.VISIBLE);

        holder.check.setOnCheckedChangeListener(null);
        holder.check.setChecked(checked.get(position));
        holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int pos = holder.getAdapterPosition();
                if (pos >= 0 && pos < checked.size()) {
                    checked.set(pos, isChecked);
                }
            }
        });

        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos >= 0 && listener != null) {
                    listener.onEdit(pos);
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.check.toggle();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final CheckBox check;
        final TextView front;
        final TextView back;
        final ImageView edit;

        VH(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.check);
            front = itemView.findViewById(R.id.front);
            back = itemView.findViewById(R.id.back);
            edit = itemView.findViewById(R.id.edit);
        }
    }
}
