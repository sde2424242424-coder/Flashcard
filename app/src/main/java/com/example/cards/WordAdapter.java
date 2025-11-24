package com.example.cards;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.WordWithStats;

import java.util.List;

public class WordAdapter extends ArrayAdapter<WordWithStats> {

    private final AppDatabase db;
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface OnLearnedChanged { void onChanged(); }
    @Nullable private final OnLearnedChanged onLearnedChanged;

    public WordAdapter(@NonNull Context context,
                       @NonNull List<WordWithStats> words,
                       @NonNull AppDatabase db) {
        this(context, words, db, null);
    }

    public WordAdapter(@NonNull Context context,
                       @NonNull List<WordWithStats> words,
                       @NonNull AppDatabase db,
                       @Nullable OnLearnedChanged callback) {
        super(context, 0, words);
        this.db = db;
        this.onLearnedChanged = callback;
    }

    static class VH {
        TextView tvFront, tvBack;
        CheckBox cbLearned;
        View cbContainer;   // container for checkbox click ripple
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        VH h;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_word, parent, false);
            h = new VH();
            h.tvFront      = convertView.findViewById(R.id.tvFront);
            h.tvBack       = convertView.findViewById(R.id.tvBack);
            h.cbLearned    = convertView.findViewById(R.id.checkbox_learned);
            h.cbContainer  = convertView.findViewById(R.id.cb_container);
            convertView.setTag(h);
        } else {
            h = (VH) convertView.getTag();
        }

        WordWithStats w = getItem(position);
        if (w == null) return convertView;

        h.tvFront.setText(w.front == null ? "" : w.front);
        h.tvBack.setText(w.back == null ? "" : w.back);

        // remove previous listener before setChecked to avoid unwanted triggers
        h.cbLearned.setOnCheckedChangeListener(null);
        h.cbLearned.setChecked(w.learned);

        // common DB update code
        h.cbLearned.setOnCheckedChangeListener((btn, checked) -> {
            final long cardId = w.cardId;
            AppDatabase.databaseExecutor.execute(() -> {
                try {
                    db.cardDao().setLearnedBoth(cardId, checked);
                    main.post(() -> {
                        w.learned = checked;
                        notifyDataSetChanged();
                        if (onLearnedChanged != null) onLearnedChanged.onChanged();
                    });
                } catch (Exception e) {
                    main.post(() -> {
                        btn.setOnCheckedChangeListener(null);
                        btn.setChecked(!checked);
                        btn.setOnCheckedChangeListener((b, c) -> {});
                        Toast.makeText(getContext(), "Failed to save flag", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // click on container → click on checkbox (for better ripple effect)
        h.cbContainer.setOnClickListener(v -> h.cbLearned.performClick());

        return convertView;
    }

    public void updateData(@Nullable List<WordWithStats> newData) {
        setNotifyOnChange(false);
        clear();
        if (newData != null && !newData.isEmpty()) addAll(newData);
        notifyDataSetChanged();
    }
}
