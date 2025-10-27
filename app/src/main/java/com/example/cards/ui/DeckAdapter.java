package com.example.cards.ui;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.R;
import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.Deck;
import com.example.cards.data.db.DbProvider;


import java.util.List;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.VH> {

    public interface OnDeckClick { void onClick(Deck deck); }

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FIRST  = 1;

    private final List<Deck> data;
    private final OnDeckClick onClick;

    public DeckAdapter(List<Deck> data, OnDeckClick onClick) {
        this.data = data;
        this.onClick = onClick;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        ImageView fox;
        TextView tvPercent;    // может быть null, если в этом layout не существует
        ProgressBar progress;  // может быть null

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            fox   = itemView.findViewById(R.id.imgFox);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progress  = itemView.findViewById(R.id.progress);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_FIRST : TYPE_NORMAL;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_FIRST)
                ? R.layout.item_deck_first
                : R.layout.item_deck;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        // 1) Текущая модель
        Deck d = data.get(pos);
        h.title.setText(d.title); // у тебя поля, не геттеры, значит оставляем d.title

        // 2) Плейсхолдеры под процент
        if (h.tvPercent != null) h.tvPercent.setText("…%");
        if (h.progress  != null) {
            h.progress.setMax(100);
            h.progress.setProgress(0);
        }

        // 3) Фоновый расчёт процента
        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase db = DbProvider.forDeck(h.itemView.getContext(), d.id); // ← ВАЖНО
            int percent = db.cardDao().learnedPercent(d.id);

            h.itemView.post(() -> {
                int cur = h.getBindingAdapterPosition();
                if (cur == RecyclerView.NO_POSITION) return;
                // защита от рассинхронизации холдера
                if (data.get(cur).id != d.id) return;

                if (h.tvPercent != null) h.tvPercent.setText(percent + "%");
                if (h.progress  != null) h.progress.setProgress(percent);
            });
        });


        // 4) Клик по карточке колоды
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(d);
        });

        // 5) Лиса только для первого типа
        if (getItemViewType(pos) == TYPE_FIRST) {
            if (h.fox == null) {
                android.util.Log.e("DeckAdapter", "imgFox == null: проверь @id/imgFox в item_deck_first.xml");
                return;
            }
            h.fox.setClickable(true);
            h.fox.setFocusable(true);
            h.fox.setFocusableInTouchMode(true);

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

            h.fox.setOnClickListener(v ->
                    v.animate()
                            .translationYBy(-10f).setDuration(100)
                            .withEndAction(() -> v.animate().translationYBy(10f).setDuration(100).start())
                            .start()
            );
        } else {
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
