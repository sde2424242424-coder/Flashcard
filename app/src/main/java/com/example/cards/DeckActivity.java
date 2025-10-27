package com.example.cards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.db.CardDao;
import com.example.cards.data.model.Card;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
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

        // Получаем аргументы
        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, -1L);
        String deckTitle = getIntent().getStringExtra(EXTRA_DECK_TITLE);
        String deckDesc  = getIntent().getStringExtra(EXTRA_DECK_DESC);

        Log.d("DeckActivity", "onCreate: deckId=" + deckId +
                " title=" + deckTitle + " desc=" + deckDesc);

        if (deckId == -1L) {
            Log.e("DeckActivity", "Missing EXTRA_DECK_ID! Finishing.");
            finish();
            return;
        }

        // UI
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(deckTitle != null ? deckTitle : ("Колода " + deckId));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TextView tvTitle = findViewById(R.id.tvDeckTitle);
        TextView tvDesc  = findViewById(R.id.tvDeckDescription);
        if (deckTitle != null && !deckTitle.isEmpty()) tvTitle.setText(deckTitle);
        if (deckDesc  != null && !deckDesc.isEmpty())  tvDesc.setText(deckDesc);

        MaterialButton btnWordList = findViewById(R.id.btnWordList);
        MaterialButton btnStudy    = findViewById(R.id.btnStudy);

        // Проверяем ожидаемый файл БД этой колоды
        String dbFileName = "cards_deck_" + deckId + ".db";
        File expected = getDatabasePath(dbFileName);
        Log.d("DeckActivity", "expected DB file = " + expected.getAbsolutePath() +
                " exists=" + expected.exists() + " size=" + expected.length());

        // Открываем БД этой колоды (фабрика при необходимости скопирует asset)
        db = AppDatabase.DbFactory.forDeck(this, deckId);
        cardDao = db.cardDao();

        // После открытия проверим ещё раз (вдруг скопировалось только что)
        Log.d("DeckActivity", "after forDeck: exists=" + expected.exists() + " size=" + expected.length());

        // Пример фоновой проверки содержимого (не влияет на проценты/списки)
        AppDatabase.databaseExecutor.execute(() -> {
            try {
                String dbPath = db.getOpenHelper().getReadableDatabase().getPath();
                Log.d("DB", "Opened path = " + dbPath);

                int total = cardDao.countAll();
                Log.d("DB", "cards total=" + total + " (deckId=" + deckId + ")");

                if (total > 0) {
                    List<Card> sample = cardDao.getPage(5, 0);
                    for (Card c : sample) {
                        Log.d("DB", "sample: id=" + c.id + " " + c.front + " / " + c.back + " deckId=" + c.deckId);
                    }
                }
            } catch (Exception e) {
                Log.e("DB", "query error", e);
            }
        });

        // Кнопки
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
