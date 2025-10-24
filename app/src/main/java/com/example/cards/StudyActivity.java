package com.example.cards;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.db.DbProvider;
import com.example.cards.data.model.Card;
import com.example.cards.domain.ReviewRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayDeque;
import java.util.List;

public class StudyActivity extends AppCompatActivity {

    private ReviewRepository repo;
    private final ArrayDeque<Card> queue = new ArrayDeque<>();

    private Button btnShowTranslation, btnEasy, btnMedium, btnHard;
    private TextView tvWord, tvTranslation;
    private LinearLayout btnDifficultyLayout;

    private AppDatabase db;
    private long deckId = 1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study);

        // ----- View bindings -----
        tvWord              = findViewById(R.id.tvWord);
        tvTranslation       = findViewById(R.id.tvTranslation);
        btnShowTranslation  = findViewById(R.id.btnShowTranslation);
        btnEasy             = findViewById(R.id.btnEasy);
        btnMedium           = findViewById(R.id.btnMedium);
        btnHard             = findViewById(R.id.btnHard);
        btnDifficultyLayout = findViewById(R.id.btnDifficultyLayout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        // ----- Extras -----
        deckId = getIntent().getLongExtra(DeckActivity.EXTRA_DECK_ID, 1L);

        // ----- DB/Repo -----
        db = AppDatabase.DbFactory.forDeck(this, deckId);
        repo = new ReviewRepository(db.reviewDao());

        // Начальное состояние UI
        showQuestionState();
        setButtonsEnabled(false);

        // Загрузим «должные» карточки
        loadDueCards();

        // Оценки
        btnHard.setOnClickListener(v -> gradeAndNext(3));   // Трудно
        btnMedium.setOnClickListener(v -> gradeAndNext(4)); // Средне
        btnEasy.setOnClickListener(v -> gradeAndNext(5));   // Легко

        // Показ перевода
        btnShowTranslation.setOnClickListener(v -> {
            Card c = queue.peekFirst();
            if (c == null) return;
            tvTranslation.setText(c.getBack());
            showAnswerState(); // показываем перевод и ТОЛЬКО три кнопки сложности
        });
    }

    // ---------------------------
    // Состояния экрана
    // ---------------------------

    private void showQuestionState() {
        // Вопрос: скрыт перевод, скрыты кнопки сложности, видна "Узнать перевод"
        tvTranslation.setVisibility(View.GONE);
        btnDifficultyLayout.setVisibility(View.GONE);
        btnShowTranslation.setVisibility(View.VISIBLE);
    }

    private void showAnswerState() {
        // Ответ: показан перевод, показаны сложность-кнопки, "Узнать перевод" скрыта
        tvTranslation.setVisibility(View.VISIBLE);
        btnDifficultyLayout.setVisibility(View.VISIBLE);
        btnShowTranslation.setVisibility(View.GONE); // ← ключевой момент: НЕТ кнопки "Дальше"
    }

    // ---------------------------
    // Бизнес-логика
    // ---------------------------

    private void loadDueCards() {
        AppDatabase.databaseExecutor.execute(() -> {
            long now = System.currentTimeMillis();

            int cardsBefore = db.reviewDao().countStates(deckId);
            db.reviewDao().seedReviewState(deckId, now);

            int excl = db.reviewDao().countExcluded(deckId);
            int learnedCards = db.reviewDao().countLearnedCards(deckId);
            int states = db.reviewDao().countStates(deckId);
            int dueN   = db.reviewDao().countDue(deckId, now);

            List<Card> due = repo.getDueCards(deckId, now, 800);

            runOnUiThread(() -> {
                Toast.makeText(
                        this,
                        "states(before)=" + cardsBefore +
                                "  excl=" + excl +
                                "  learned(cards)=" + learnedCards +
                                "  states=" + states +
                                "  due=" + dueN +
                                "  queued=" + (due != null ? due.size() : 0),
                        Toast.LENGTH_LONG
                ).show();

                queue.clear();
                if (due != null) queue.addAll(due);
                showNext();
            });
        });
    }

    private void showNext() {
        Card c = queue.peekFirst();
        if (c == null) {
            tvWord.setText("Повторений нет");
            tvTranslation.setVisibility(View.GONE);
            btnDifficultyLayout.setVisibility(View.GONE);
            btnShowTranslation.setVisibility(View.GONE); // нечего показывать
            setButtonsEnabled(false);
            return;
        }

        tvWord.setText(c.getFront());
        tvTranslation.setText("");
        showQuestionState();         // каждая новая карточка начинается с режима "вопрос"
        setButtonsEnabled(true);     // активируем кнопки оценок (хотя они скрыты до показа ответа)
    }

    private void setButtonsEnabled(boolean enabled) {
        btnHard.setEnabled(enabled);
        btnMedium.setEnabled(enabled);
        btnEasy.setEnabled(enabled);
        btnShowTranslation.setEnabled(enabled);
    }

    private void gradeAndNext(int grade) {
        Card current = queue.pollFirst();
        if (current == null) return;

        AppDatabase.databaseExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            repo.reviewAndSchedule(current.getId(), grade, now);
            runOnUiThread(this::showNext); // сразу следующая карточка -> снова "Узнать перевод"
        });
    }
}
