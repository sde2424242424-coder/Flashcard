// WordListActivity.java (исправлено под твой CardDao)
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Objects;

public class WordListActivity extends AppCompatActivity {

    private long deckId;
    private AppDatabase db;
    private ListView listView;
    private WordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        long deckIdFromCamel = getIntent().getLongExtra("deckId", -1L);
        long deckIdFromSnake = getIntent().getLongExtra("deck_id", -1L);
        deckId = (deckIdFromCamel != -1L) ? deckIdFromCamel
                : (deckIdFromSnake != -1L ? deckIdFromSnake : 1L);

        db = AppDatabase.DbFactory.forDeck(this, deckId);
        android.util.Log.d("DB", "WordList uses deckId=" + deckId
                + " file=" + getDatabasePath("cards_deck_" + deckId + ".db"));

        listView = findViewById(R.id.listWords);
        if (listView == null) throw new IllegalStateException("ListView R.id.listWords not found");

        EditText searchInput = findViewById(R.id.searchInput);

        // Первичная загрузка — БЕЗ аргументов
        AppDatabase.databaseExecutor.execute(() -> {
            List<WordWithStats> rows = db.cardDao().getWordsWithStatsAll();
            runOnUiThread(() -> {
                adapter = new WordAdapter(WordListActivity.this, rows, db,
                        () -> setResult(RESULT_OK));
                listView.setAdapter(adapter);
            });
        });

        // Поиск — С deckId и строкой
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String q = (s == null) ? "" : s.toString().trim();
                AppDatabase.databaseExecutor.execute(() -> {
                    List<WordWithStats> data;
                    if (q.isEmpty()) {
                        data = db.cardDao().getWordsWithStatsAll(); // пустой запрос — показываем всё
                    } else {
                        data = db.cardDao().searchWords(deckId, q);  // твоя сигнатура
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
        clearTooltips(getWindow().getDecorView());
        super.onPause();
    }

    @Override
    protected void onStop() {
        clearTooltips(getWindow().getDecorView());
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (listView != null) listView.setAdapter(null);
        super.onDestroy();
    }

    private void clearTooltips(View v) {
        try { TooltipCompat.setTooltipText(v, null); } catch (Throwable ignored) {}
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) clearTooltips(vg.getChildAt(i));
        }
        v.setOnHoverListener(null);
        v.setOnLongClickListener(null);
    }
}
