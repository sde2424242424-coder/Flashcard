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

/**
 * CardAdapter
 *
 * RecyclerView adapter for displaying a list of {@link Card} items.
 * Uses {@link ListAdapter} with {@link DiffUtil} to efficiently handle
 * list updates and animations when the data set changes.
 *
 * Responsibilities:
 * - Provide item views for card front/back text.
 * - Compare items and contents via DiffUtil to perform smart updates.
 */
public class CardAdapter extends ListAdapter<Card, CardAdapter.VH> {

    /**
     * Creates a CardAdapter with a predefined DiffUtil callback.
     */
    public CardAdapter() {
        super(DIFF);
    }

    /**
     * DiffUtil callback used by ListAdapter to determine item and content changes.
     */
    static final DiffUtil.ItemCallback<Card> DIFF = new DiffUtil.ItemCallback<Card>() {
        @Override
        public boolean areItemsTheSame(@NonNull Card a, @NonNull Card b) {
            // Items are the same if they have the same unique ID.
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Card a, @NonNull Card b) {
            // Contents are the same if all relevant fields are equal.
            return a.deckId == b.deckId &&
                    Objects.equals(a.front, b.front) &&
                    Objects.equals(a.back, b.back) &&
                    a.createdAt == b.createdAt;
        }
    };

    /**
     * ViewHolder that holds references to the TextViews used to display
     * the front and back text of a Card.
     */
    static class VH extends RecyclerView.ViewHolder {
        final TextView front, back;

        VH(View v) {
            super(v);
            front = v.findViewById(R.id.tvFront);
            back  = v.findViewById(R.id.tvBack);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout for a single card item.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        // Bind card data to ViewHolder.
        Card c = getItem(position);
        holder.front.setText(c.front);
        holder.back.setText(c.back);
    }
}
