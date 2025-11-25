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

/**
 * WordAdapter
 *
 * Custom ArrayAdapter for displaying a list of {@link WordWithStats} items
 * in a ListView. Each row shows:
 * - the front side of the card (word),
 * - the back side (translation or explanation),
 * - a "learned" checkbox that can be toggled by the user.
 *
 * Responsibilities:
 * - Bind word data (front/back text) to item views.
 * - Reflect and persist the "learned" state in the database when the
 *   checkbox is toggled.
 * - Notify an optional callback when the learned state changes, so the
 *   parent screen can update statistics or UI.
 */
public class WordAdapter extends ArrayAdapter<WordWithStats> {

    // Reference to Room database, used to update "learned" state.
    private final AppDatabase db;

    // Handler bound to the main thread, used to update UI after background work.
    private final Handler main = new Handler(Looper.getMainLooper());

    /**
     * Callback interface notified when any word's "learned" flag changes.
     */
    public interface OnLearnedChanged {
        void onChanged();
    }

    @Nullable
    private final OnLearnedChanged onLearnedChanged;

    /**
     * Creates an adapter without a learned-state callback.
     */
    public WordAdapter(@NonNull Context context,
                       @NonNull List<WordWithStats> words,
                       @NonNull AppDatabase db) {
        this(context, words, db, null);
    }

    /**
     * Creates an adapter with an optional callback invoked whenever
     * the learned flag changes for any item.
     */
    public WordAdapter(@NonNull Context context,
                       @NonNull List<WordWithStats> words,
                       @NonNull AppDatabase db,
                       @Nullable OnLearnedChanged callback) {
        super(context, 0, words);
        this.db = db;
        this.onLearnedChanged = callback;
    }

    /**
     * ViewHolder pattern to avoid repeated findViewById calls.
     */
    static class VH {
        TextView tvFront, tvBack;
        CheckBox cbLearned;
        View cbContainer;   // container view used to handle ripple + click area for checkbox
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        VH h;
        if (convertView == null) {
            // Inflate row layout and create a new ViewHolder.
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_word, parent, false);
            h = new VH();
            h.tvFront      = convertView.findViewById(R.id.tvFront);
            h.tvBack       = convertView.findViewById(R.id.tvBack);
            h.cbLearned    = convertView.findViewById(R.id.checkbox_learned);
            h.cbContainer  = convertView.findViewById(R.id.cb_container);
            convertView.setTag(h);
        } else {
            // Reuse existing ViewHolder.
            h = (VH) convertView.getTag();
        }

        // Get current item.
        WordWithStats w = getItem(position);
        if (w == null) return convertView;

        // Bind front/back text with null safety.
        h.tvFront.setText(w.front == null ? "" : w.front);
        h.tvBack.setText(w.back == null ? "" : w.back);

        // Remove previous listener before updating checked state to avoid
        // triggering it when we call setChecked().
        h.cbLearned.setOnCheckedChangeListener(null);
        h.cbLearned.setChecked(w.learned);

        // Listener that updates "learned" flag in the database when the
        // checkbox is toggled.
        h.cbLearned.setOnCheckedChangeListener((btn, checked) -> {
            final long cardId = w.cardId;
            AppDatabase.databaseExecutor.execute(() -> {
                try {
                    // Update both the card and its review state in the DB.
                    db.cardDao().setLearnedBoth(cardId, checked);

                    // On success: update in-memory model and refresh UI.
                    main.post(() -> {
                        w.learned = checked;
                        notifyDataSetChanged();
                        if (onLearnedChanged != null) onLearnedChanged.onChanged();
                    });
                } catch (Exception e) {
                    // On failure: revert checkbox and show a message.
                    main.post(() -> {
                        // Temporarily remove listener to avoid recursion.
                        btn.setOnCheckedChangeListener(null);
                        btn.setChecked(!checked);
                        // Re-attach dummy listener (or could re-attach the real one if needed).
                        btn.setOnCheckedChangeListener((b, c) -> {});
                        Toast.makeText(getContext(),
                                "Failed to save flag",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }
            });
        });

        // Click on container triggers checkbox click (for better ripple feedback
        // and larger tap area).
        h.cbContainer.setOnClickListener(v -> h.cbLearned.performClick());

        return convertView;
    }

    /**
     * Replaces the adapter's data set with a new list of WordWithStats.
     * Uses setNotifyOnChange(false) to control when the update is propagated,
     * then calls notifyDataSetChanged() once at the end.
     *
     * @param newData new list of words (can be null or empty).
     */
    public void updateData(@Nullable List<WordWithStats> newData) {
        setNotifyOnChange(false);
        clear();
        if (newData != null && !newData.isEmpty()) addAll(newData);
        notifyDataSetChanged();
    }
}
