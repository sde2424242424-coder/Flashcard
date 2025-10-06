package com.example.cards;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.Card;
import com.example.cards.domain.ReviewRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayDeque;
import java.util.List;

public class StudyActivity extends AppCompatActivity {

    private ReviewRepository repo;
    private final ArrayDeque<Card> queue = new ArrayDeque<>();

    private Button btnShowTranslation, btnEasy, btnMedium, btnHard, btnWordList;
    private TextView tvWord, tvTranslation;
    private LinearLayout btnDifficultyLayout;

    private AppDatabase db;
    private long deckId = 1L; // при необходимости возьмите из Intent
    private boolean showingTranslation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study);

        // ----- View bindings -----
        tvWord             = findViewById(R.id.tvWord);
        tvTranslation      = findViewById(R.id.tvTranslation);
        btnShowTranslation = findViewById(R.id.btnShowTranslation);
        btnEasy            = findViewById(R.id.btnEasy);
        btnMedium          = findViewById(R.id.btnMedium);
        btnHard            = findViewById(R.id.btnHard);
        btnWordList        = findViewById(R.id.btnWordList);
        btnDifficultyLayout= findViewById(R.id.btnDifficultyLayout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        // Стартовое состояние

        tvTranslation.setVisibility(View.GONE);
        btnDifficultyLayout.setVisibility(View.GONE);
        setButtonsEnabled(false);

        toolbar.setNavigationOnClickListener(v -> finish());

        // ----- DB/Repo -----
        deckId = getIntent().getLongExtra("deck_id", 1L);

// ВАЖНО: открываем ту же пер-колодную БД, что и в DeckActivity
        this.db = AppDatabase.DbFactory.forDeck(this, deckId);

// Репозиторий берём из этой же БД
        this.repo = new ReviewRepository(db.reviewDao());
        // deckId = getIntent().getLongExtra("deck_id", 1L);

        // Загрузим «должные» карточки
        loadDueCards();

        // Обработчики оценок
        btnHard.setOnClickListener(v -> gradeAndNext(3));   // Трудно
        btnMedium.setOnClickListener(v -> gradeAndNext(4)); // Средне
        btnEasy.setOnClickListener(v -> gradeAndNext(5));   // Легко

        // Показ/скрытие перевода
        btnShowTranslation.setOnClickListener(v -> {
            Card c = queue.peekFirst();
            if (c == null) return;
            if (!showingTranslation) {
                tvTranslation.setText(c.getBack());
                tvTranslation.setVisibility(View.VISIBLE);
                btnDifficultyLayout.setVisibility(View.VISIBLE);
                btnShowTranslation.setText("Дальше");
                showingTranslation = true;
            } else {
                showingTranslation = false;
                tvTranslation.setVisibility(View.GONE);
                btnDifficultyLayout.setVisibility(View.GONE);
                btnShowTranslation.setText("Узнать перевод");
                queue.pollFirst();
                showNext();
            }
        });

        // Переход к списку слов
        btnWordList.setOnClickListener(v -> {
            Intent i = new Intent(this, WordListActivity.class);
            i.putExtra("deck_id", deckId);   // <— добавить эту строку!
            startActivity(i);
        });

    }

    private void loadDueCards() {
        AppDatabase.databaseExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            List<Card> due = repo.getDueCards(now, 50);
            runOnUiThread(() -> {
                queue.clear();
                queue.addAll(due);
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
            btnShowTranslation.setText("Узнать перевод");
            showingTranslation = false;
            setButtonsEnabled(false);
            return;
        }
        tvWord.setText(c.getFront());
        tvTranslation.setText("");
        tvTranslation.setVisibility(View.GONE);
        btnDifficultyLayout.setVisibility(View.GONE);
        btnShowTranslation.setText("Узнать перевод");
        showingTranslation = false;
        setButtonsEnabled(true);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnHard.setEnabled(enabled);
        btnMedium.setEnabled(enabled);
        btnEasy.setEnabled(enabled);
    }

    private void gradeAndNext(int grade) {
        Card current = queue.pollFirst();
        if (current == null) return;

        AppDatabase.databaseExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            repo.reviewAndSchedule(current.getId(), grade, now);
            runOnUiThread(this::showNext);
        });
    }
}
