/*package com.example.cards;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.data.model.Card;

import java.util.List;

class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private List<Card> cards;

    public CardAdapter(@NonNull List<Card> cardList) {
        if (cardList == null) {
            throw new IllegalArgumentException("Card list cannot be null");
        }
        this.cards = cardList;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.frontText.setText(card.front);
        holder.backText.setText(card.back);
    }

    @Override
    public int getItemCount() {
        return cards != null ? cards.size() : 0;  // Проверка на null
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView frontText, backText;

        public CardViewHolder(View itemView) {
            super(itemView);
            frontText = itemView.findViewById(R.id.frontText);
            backText = itemView.findViewById(R.id.backText);
        }
    }
}*/
