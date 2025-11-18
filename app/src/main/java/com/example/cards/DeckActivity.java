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
import com.example.cards.util.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.List;

public class DeckActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID    = "deckId";
    public static final String EXTRA_DECK_TITLE = "deckTitle";
    public static final String EXTRA_DECK_DESC  = "deckDescription"; // можно не использовать, но оставлю константу

    private long deckId;
    private CardDao cardDao;
    private AppDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck);

        // Получаем аргументы
        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, -1L);
        String deckTitle = getIntent().getStringExtra(EXTRA_DECK_TITLE);
        // String deckDesc  = getIntent().getStringExtra(EXTRA_DECK_DESC); // описание теперь задаём в коде

        Log.d("DeckActivity", "onCreate: deckId=" + deckId +
                " title=" + deckTitle);

        if (deckId == -1L) {
            Log.e("DeckActivity", "Missing EXTRA_DECK_ID! Finishing.");
            finish();
            return;
        }

        // UI
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView tvTitle       = findViewById(R.id.tvDeckTitle);
        TextView tvSubtitle    = findViewById(R.id.tvDeckSubtitle);
        TextView tvDesc        = findViewById(R.id.tvDeckDescription);
        MaterialButton btnWordList = findViewById(R.id.btnWordList);
        MaterialButton btnStudy    = findViewById(R.id.btnStudy);

        // Заголовок
        if (deckTitle != null && !deckTitle.isEmpty()) {
            tvTitle.setText(deckTitle);
            toolbar.setTitle(deckTitle);
        } else {
            String fallbackTitle = "Колода " + deckId;
            tvTitle.setText(fallbackTitle);
            toolbar.setTitle(fallbackTitle);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Индивидуальный subtitle + description для каждой колоды
        setupDeckTexts((int) deckId, tvSubtitle, tvDesc);

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

    /**
     * Задаём свои subtitle/description для каждой колоды по ее deckId.
     * deckId в БД long, но логика колод обычно 1,2,3,...
     */
    private void setupDeckTexts(int deckId,
                                TextView tvSubtitle,
                                TextView tvDescription) {

        switch (deckId) {
            case 1:
                tvSubtitle.setText("для начинающих");
                tvDescription.setText("Базовая лексика уровня 1급: самые частотные слова для ежедневной практики.");
                break;

            case 2:
                tvSubtitle.setText("продолжаем учиться");
                tvDescription.setText("Слова уровня 2급: расширяем словарный запас для повседневных ситуаций.");
                break;

            case 3:
                tvSubtitle.setText("уверенный уровень");
                tvDescription.setText("Лексика 3급: более сложные слова для общения и чтения новостей.");
                break;

            case 4:
                tvSubtitle.setText("уверенный уровень");
                tvDescription.setText("Лексика 4급: более сложные слова для общения и чтения новостей.");
                break;

            case 5:
                tvSubtitle.setText("уверенный уровень");
                tvDescription.setText("Лексика 5급: более сложные слова для общения и чтения новостей.");
                break;

            case 6:
                tvSubtitle.setText("уверенный уровень");
                tvDescription.setText("Лексика 6급: более сложные слова для общения и чтения новостей.");
                break;

            default:
                tvSubtitle.setText("колода");
                tvDescription.setText("Описание колоды скоро будет добавлено.");
                break;
        }
    }
}
