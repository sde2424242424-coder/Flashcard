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

        // --- Явно логируем incoming extras и не маскируем отсутствие ключа ---
        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, -1L); // <- из -1 станет очевидно, если ключ не пришёл
        String deckTitle = getIntent().getStringExtra(EXTRA_DECK_TITLE);
        String deckDesc  = getIntent().getStringExtra(EXTRA_DECK_DESC);

        Log.d("DeckActivity", "onCreate: received extras deckId=" + deckId +
                " title=" + deckTitle + " desc=" + deckDesc);

        if (deckId == -1L) {
            Log.e("DeckActivity", "Missing EXTRA_DECK_ID! Aborting.");
            // можно показать toast или finish()
            // Toast.makeText(this, "Ошибка: не выбрана колода", Toast.LENGTH_SHORT).show();
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

        // До вызова фабрики — проверим ожидаемый путь файла
        String dbFileName = "cards_deck_" + deckId + ".db";
        File expected = getDatabasePath(dbFileName);
        Log.d("DeckActivity", "expected DB file path = " + expected.getAbsolutePath() +
                " exists=" + expected.exists() + " size=" + expected.length());

        // Открываем БД (фабрика может скопировать файл из assets при первом вызове).
        // Оборачиваем операции с Room/SQLite в background executor, чтобы не блокировать UI.
        db = AppDatabase.DbFactory.forDeck(this, deckId);
        cardDao = db.cardDao();

        // После создания Room — снова проверим файл (фабрика могла скопировать asset)
        Log.d("DeckActivity", "after forDeck: expected.exists=" + expected.exists() + " size=" + expected.length());

        AppDatabase.databaseExecutor.execute(() -> {
            try {
                // Получим путь БД в фоне (без открытия на UI потоке)
                String dbPath = db.getOpenHelper().getReadableDatabase().getPath();
                Log.d("DB", "Opened (background) path = " + dbPath);

                int total = cardDao.countAll();
                Log.d("DB", "[no-filter] total=" + total + " (deckId=" + deckId + ")");

                if (total == 0) {
                    Log.w("DB", "Файл колоды пуст или таблица cards пуста.");
                } else {
                    List<Card> sample = cardDao.getPage(5, 0);
                    for (Card c : sample) {
                        Log.d("DB", "[no-filter] " + c.id + " " + c.front + " / " + c.back + " deckId=" + c.deckId);
                    }
                }

                int byDeck = cardDao.countByDeck(deckId);
                Log.d("DB", "[with-filter] byDeck=" + byDeck + " (deckId=" + deckId + ")");
                if (byDeck == 0 && total > 0) {
                    Log.w("DB", "В БД есть записи, но filter by deckId ничего не вернул. " +
                            "Возможно в файле deckId других значений или DAO использует фильтр, " +
                            "а вы ожидаете per-file БД без фильтра.");
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