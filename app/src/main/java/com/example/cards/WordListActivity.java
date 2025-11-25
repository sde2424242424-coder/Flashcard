// WordListActivity.java
package com.example.cards;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.WordWithStats;
import com.example.cards.util.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.Objects;

/**
 * WordListActivity
 *
 * Screen that displays all words for a specific deck in a ListView,
 * with support for:
 * - Viewing front/back text of each word.
 * - Toggling "learned" state via {@link WordAdapter}.
 * - Real-time search/filtering by text input.
 *
 * Responsibilities:
 * - Resolve deckId from Intent extras and open the corresponding deck database.
 * - Load the initial list of words with statistics.
 * - Filter words on the fly when the user types into the search field.
 * - Clean up adapter and tooltips on lifecycle changes.
 */
public class WordListActivity extends AppCompatActivity {

    private long deckId;
    private AppDatabase db;
    private ListView listView;
    private WordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply current theme before inflating the layout.
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_list);

        // ----- Toolbar setup -----
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(
                    v -> getOnBackPressedDispatcher().onBackPressed()
            );
        }

        // ----- Resolve deckId from Intent -----
        // Support both "deckId" (camelCase) and "deck_id" (snake_case) keys.
        long deckIdFromCamel = getIntent().getLongExtra("deckId", -1L);
        long deckIdFromSnake = getIntent().getLongExtra("deck_id", -1L);
        deckId = (deckIdFromCamel != -1L)
                ? deckIdFromCamel
                : (deckIdFromSnake != -1L ? deckIdFromSnake : 1L);

        // Create deck-specific database instance for this deckId.
        db = AppDatabase.DbFactory.forDeck(this, deckId);
        android.util.Log.d(
                "DB",
                "WordList uses deckId=" + deckId
                        + " file=" + getDatabasePath("cards_deck_" + deckId + ".db")
        );

        // ----- ListView binding -----
        listView = findViewById(R.id.listWords);
        if (listView == null) {
            throw new IllegalStateException("ListView R.id.listWords not found");
        }

        EditText searchInput = findViewById(R.id.searchInput);

        // ----- Initial data load (no search query, show all words) -----
        AppDatabase.databaseExecutor.execute(() -> {
            // Load all words with stats for this deck.
            List<WordWithStats> rows = db.cardDao().getWordsWithStatsAll();
            runOnUiThread(() -> {
                // Create adapter and set a callback to mark result OK when learned state changes.
                adapter = new WordAdapter(
                        WordListActivity.this,
                        rows,
                        db,
                        () -> setResult(RESULT_OK)
                );
                listView.setAdapter(adapter);
            });
        });

        // ----- Search behavior -----
        // Filters the list as the user types. Empty query = show all words.
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String q = (s == null) ? "" : s.toString().trim();
                AppDatabase.databaseExecutor.execute(() -> {
                    List<WordWithStats> data;
                    if (q.isEmpty()) {
                        // Empty query → show all words.
                        data = db.cardDao().getWordsWithStatsAll();
                    } else {
                        // Non-empty query → search within this deck by text.
                        // Uses cardDao().searchWords(deckId, queryString).
                        data = db.cardDao().searchWords(deckId, q);
                    }
                    runOnUiThread(() -> {
                        if (adapter != null) adapter.updateData(data);
                    });
                });
            }
        });
    }

    @Override
    protected void onPause() {
        // Clear all tooltips to avoid leaks when the activity is no longer visible.
        clearTooltips(getWindow().getDecorView());
        super.onPause();
    }

    @Override
    protected void onStop() {
        // Double safety: clear tooltips again when activity moves to stopped state.
        clearTooltips(getWindow().getDecorView());
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // Detach adapter to help GC and avoid potential leaks.
        if (listView != null) {
            listView.setAdapter(null);
        }
        super.onDestroy();
    }

    /**
     * Recursively clears tooltips and hover/long-click listeners on a view
     * hierarchy to avoid memory leaks related to TooltipCompat.
     *
     * @param v root view to start clearing from.
     */
    private void clearTooltips(View v) {
        try {
            TooltipCompat.setTooltipText(v, null);
        } catch (Throwable ignored) {
            // Ignore any compatibility issues.
        }

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                clearTooltips(vg.getChildAt(i));
            }
        }

        v.setOnHoverListener(null);
        v.setOnLongClickListener(null);
    }
}
