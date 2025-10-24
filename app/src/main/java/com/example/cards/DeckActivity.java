package com.example.cards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.data.db.AppDatabase;
// ИЛИ используй ваш DbProvider, если он у тебя есть:
// import com.example.cards.data.db.DbProvider;
import com.example.cards.data.db.CardDao;
import com.example.cards.data.model.Card;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class DeckActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID    = "deckId";
    public static final String EXTRA_DECK_TITLE = "deckTitle";
    public static final String EXTRA_DECK_DESC  = "deckDescription";

    private long deckId;
    private CardDao cardDao;
    private AppDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck);

        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, 1L);
        String deckTitle = getIntent().getStringExtra(EXTRA_DECK_TITLE);
        String deckDesc  = getIntent().getStringExtra(EXTRA_DECK_DESC);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(deckTitle != null ? deckTitle : ("Колода " + deckId));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TextView tvTitle = findViewById(R.id.tvDeckTitle);
        TextView tvDesc  = findViewById(R.id.tvDeckDescription);
        if (deckTitle != null && !deckTitle.isEmpty()) tvTitle.setText(deckTitle);
        if (deckDesc  != null && !deckDesc.isEmpty())  tvDesc.setText(deckDesc);

        MaterialButton btnWordList = findViewById(R.id.btnWordList);
        MaterialButton btnStudy    = findViewById(R.id.btnStudy);

        // ОТКРЫВАЕМ ИМЕННО ФАЙЛ ЭТОЙ КОЛОДЫ
        db = AppDatabase.DbFactory.forDeck(this, deckId); // или DbProvider.forDeck(...)
        cardDao = db.cardDao();

        Log.d("DB", "Opened path = " + db.getOpenHelper().getWritableDatabase().getPath());

        // ВСЕ ЗАПРОСЫ — НЕ НА UI ПОТОКЕ
        AppDatabase.databaseExecutor.execute(() -> {
            try {
                // Если у тебя отдельный .db на каждую колоду — начинаем с варианта БЕЗ фильтра
                int total = cardDao.countAll(); // SELECT COUNT(*) FROM cards
                Log.d("DB", "[no-filter] total=" + total + " (deckId=" + deckId + ")");

                if (total == 0) {
                    Log.w("DB", "Файл колоды пуст. Либо пересоздали БД, либо не подложили данные. " +
                            "Если используешь createFromAsset, удали существующий файл и дай Room заново скопировать из assets.");
                } else {
                    // Проба чтения 5 карточек без фильтра
                    List<Card> sample = cardDao.getPage(5, 0);
                    for (Card c : sample) {
                        Log.d("DB", "[no-filter] " + c.id + " " + c.front + " / " + c.back + " deckId=" + c.deckId);
                    }
                }

                // Если ты В ДАО используешь запросы С ФИЛЬТРОМ по deckId — проверим, что они что-то возвращают
                int byDeck = cardDao.countByDeck(deckId); // SELECT COUNT(*) FROM cards WHERE deckId=:deckId
                Log.d("DB", "[with-filter] byDeck=" + byDeck + " (deckId=" + deckId + ")");

                if (byDeck == 0 && total > 0) {
                    Log.w("DB", "В файле есть строки, но по deckId=" + deckId + " ничего не нашлось. " +
                            "Скорее всего, внутри файла у всех записей deckId=1. " +
                            "Либо не фильтруй по deckId для per-deck файла, " +
                            "либо один раз выполни UPDATE cards SET deckId=" + deckId + " при создании файла.");
                }

            } catch (Exception e) {
                Log.e("DB", "query error", e);
            }
        });

        btnWordList.setOnClickListener(v -> {
            Intent i = new Intent(this, WordListActivity.class);
            i.putExtra(EXTRA_DECK_ID, deckId);
            startActivity(i);
        });

        btnStudy.setOnClickListener(v -> {
            Intent i = new Intent(this, StudyActivity.class);
            i.putExtra(EXTRA_DECK_ID, deckId);
            i.putExtra(EXTRA_DECK_TITLE, tvTitle.getText().toString());
            startActivity(i);
        });
    }
}
