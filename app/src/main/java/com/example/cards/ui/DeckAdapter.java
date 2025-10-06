package com.example.cards.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.R;
import com.example.cards.data.model.Deck;

import java.util.List;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.VH> {
    public interface OnDeckClick {
        void onClick(Deck deck);
    }
    private final List<Deck> data;
    private final OnDeckClick onClick;
    public DeckAdapter(List<Deck> data, OnDeckClick onClick) {
        this.data = data;
        this.onClick = onClick;
    }
    static class VH extends RecyclerView.ViewHolder {
        TextView title;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
        }
    }
    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck, parent, false);
        return new VH(v);
        }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Deck d = data.get(pos);
        h.title.setText(d.title);
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(d);
        });
    }
    @Override public int getItemCount() {return data.size();}
}