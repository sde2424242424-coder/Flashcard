package com.example.cards;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.WordWithStats;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.Objects;

public class WordListActivity extends AppCompatActivity {

    private long deckId;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_list);

        // 1) Toolbar (опционально как action bar)
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        // 2) Получаем deckId из Intent
        deckId = getIntent().getLongExtra("deck_id", 1L);

        // 3) Открываем БД этой колоды
        db = AppDatabase.DbFactory.forDeck(this, deckId);

        // 4) Находим ListView
        ListView listView = findViewById(R.id.listWords);
        if (listView == null) throw new IllegalStateException("ListView R.id.listWords not found");

        // 5) Грузим слова (read-only)
        AppDatabase.databaseExecutor.execute(() -> {
            List<WordWithStats> rows = db.cardDao().getWordsWithStats(); // только SELECT

            runOnUiThread(() -> {
                // Твой кастомный адаптер
                WordAdapter wordAdapter = new WordAdapter(this, rows);
                listView.setAdapter(wordAdapter);
            });
        });
    }
}
