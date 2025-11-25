package com.example.cards.ui;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.R;
import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.Deck;
import com.example.cards.data.db.DbProvider;

import java.util.List;

/**
 * DeckAdapter
 *
 * RecyclerView adapter for displaying a list of {@link Deck} items.
 * Supports two view types:
 * - The first deck item with a special layout and interactive fox image.
 * - Normal deck items for all other positions.
 *
 * Responsibilities:
 * - Bind deck title and progress (learned percent) to card views.
 * - Load learned percentage in the background for each deck.
 * - Provide click handling for deck selection.
 * - Animate the fox icon in the first item and handle its touch area.
 */
public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.VH> {

    /**
     * Callback for deck item clicks.
     */
    public interface OnDeckClick {
        void onClick(Deck deck);
    }

    // View types: special first item and normal items.
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FIRST  = 1;

    private final List<Deck> data;
    private final OnDeckClick onClick;

    public DeckAdapter(List<Deck> data, OnDeckClick onClick) {
        this.data = data;
        this.onClick = onClick;
    }

    /**
     * ViewHolder for a deck card.
     * Contains:
     * - title: deck name
     * - fox: optional fox image (only present in the first item layout)
     * - tvPercent: optional text view for progress percent
     * - progress: optional progress bar for learned percent
     */
    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        ImageView fox;
        TextView tvPercent;    // may be null if the layout does not define this view
        ProgressBar progress;  // may be null

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            fox = itemView.findViewById(R.id.imgFoxHead);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progress  = itemView.findViewById(R.id.progress);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // First item uses a special layout.
        return position == 0 ? TYPE_FIRST : TYPE_NORMAL;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_FIRST)
                ? R.layout.item_deck_first
                : R.layout.item_deck;

        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);

        // Adjust Z translation for layering; we do not modify actual Z order.
        if (viewType == TYPE_FIRST) {
            ViewCompat.setTranslationZ(v, 0f);
        } else {
            ViewCompat.setTranslationZ(v, 1f);
        }

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        // 1) Current deck model.
        Deck d = data.get(pos);
        h.title.setText(d.title);

        // 2) Initial placeholders for percent and progress.
        if (h.tvPercent != null) {
            h.tvPercent.setText("…%");
        }
        if (h.progress != null) {
            h.progress.setMax(100);
            h.progress.setProgress(0);
        }

        // 3) Background calculation of learned percent for this deck.
        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase db = DbProvider.forDeck(h.itemView.getContext(), d.id);
            int percent = db.cardDao().learnedPercent(d.id);

            h.itemView.post(() -> {
                int cur = h.getBindingAdapterPosition();
                if (cur == RecyclerView.NO_POSITION) return;
                // Ensure ViewHolder is still bound to the same deck.
                if (data.get(cur).id != d.id) return;

                if (h.tvPercent != null) h.tvPercent.setText(percent + "%");
                if (h.progress  != null) h.progress.setProgress(percent);
            });
        });

        // 4) Card click → propagate deck selection via callback.
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(d);
        });

        // 5) Fox behavior only for the first item type.
        if (getItemViewType(pos) == TYPE_FIRST) {
            if (h.fox == null) {
                android.util.Log.e(
                        "DeckAdapter",
                        "imgFox == null: check @id/imgFox in item_deck_first.xml"
                );
                return;
            }

            h.fox.setClickable(true);
            h.fox.setFocusable(true);
            h.fox.setFocusableInTouchMode(true);

            // Touch listener limits interception so that only the center area
            // of the fox is treated as a click region, improving gesture behavior.
            h.fox.setOnTouchListener((v, event) -> {
                float x = event.getX(), y = event.getY();
                float w = v.getWidth(), hgt = v.getHeight();

                float left = w * 0.30f, right = w * 0.70f;
                float top  = hgt * 0.30f, bottom = hgt * 0.70f;

                boolean inside = (x >= left && x <= right && y >= top && y <= bottom);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.getParent().requestDisallowInterceptTouchEvent(inside);
                        return !inside;
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        return !inside;
                }
                return false;
            });

            // Simple "bounce" animation when fox is clicked.
            h.fox.setOnClickListener(v ->
                    v.animate()
                            .translationYBy(-10f).setDuration(100)
                            .withEndAction(() ->
                                    v.animate()
                                            .translationYBy(10f)
                                            .setDuration(100)
                                            .start()
                            )
                            .start()
            );
        } else {
            // For normal items, ensure fox has no special handlers if view is reused.
            if (h.fox != null) {
                h.fox.setOnClickListener(null);
                h.fox.setOnTouchListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
