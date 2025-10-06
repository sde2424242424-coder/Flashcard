package com.example.cards;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.data.model.Card;

import java.util.Objects;

public class CardAdapter extends ListAdapter<Card, CardAdapter.VH> {
    public CardAdapter() { super(DIFF); }

    static final DiffUtil.ItemCallback<Card> DIFF = new DiffUtil.ItemCallback<Card>() {
        @Override public boolean areItemsTheSame(@NonNull Card a, @NonNull Card b) { return a.id == b.id; }
        @Override public boolean areContentsTheSame(@NonNull Card a, @NonNull Card b) {
            return a.deckId == b.deckId &&
                    Objects.equals(a.front, b.front) &&
                    Objects.equals(a.back, b.back) &&
                    a.createdAt == b.createdAt;
        }
    };

    static class VH extends RecyclerView.ViewHolder {
        final TextView front, back;
        VH(View v){
            super(v);
            front = v.findViewById(R.id.tvFront);
            back  = v.findViewById(R.id.tvBack);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_card, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Card c = getItem(pos);
        h.front.setText(c.front);
        h.back.setText(c.back);
    }
}
