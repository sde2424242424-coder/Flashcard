package com.example.cards;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.Card;
import com.example.cards.domain.ReviewRepository;
import com.example.cards.util.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

public class StudyActivity extends AppCompatActivity {

    private ReviewRepository repo;
    private final ArrayDeque<Card> queue = new ArrayDeque<>();

    private Button btnShowTranslation, btnEasy, btnMedium, btnHard;
    private TextView tvWord, tvTranslation;

    // ---- New fields (fox + bubble) ----
    private ImageView foxImage;
    private TextView bubbleText;
    private int hardClicks = 0;
    private final Random rnd = new Random();

    private final String[] normalPhrases = new String[] {
            "Great! Just a little more 🦊",
            "You’re doing great, keep it up 💪",
            "Every word makes you stronger ✨",
            "You’re doing well! The fox is proud of you 🧡"
    };
    private final String hard3Phrase =
            "It’s okay! Even the fox doesn’t understand everything the first time 🦊💤";

    private LinearLayout btnDifficultyLayout;

    private AppDatabase db;
    private long deckId = 1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyThemeFromPrefs(this);
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
        foxImage            = findViewById(R.id.foxImage);
        bubbleText          = findViewById(R.id.bubbleText);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        // ----- Extras -----
        deckId = getIntent().getLongExtra(DeckActivity.EXTRA_DECK_ID, 1L);

        // ----- DB/Repo -----
        db = AppDatabase.DbFactory.forDeck(this, deckId);
        repo = new ReviewRepository(db.reviewDao());

        // Initial UI state
        showQuestionState();
        setButtonsEnabled(false);
        showRandomPhrase();         // initial phrase
        switchFoxToNormal();        // initial fox state

        // Load due cards
        loadDueCards();

        // Answers (with fox + phrase logic)
        btnHard.setOnClickListener(v -> {
            hardClicks++;
            if (hardClicks >= 3) {
                hardClicks = 0;
                switchFoxToSupport();
                setBubbleText(hard3Phrase);
            } else {
                switchFoxToNormal();
                showRandomPhrase();
            }
            gradeAndNext(3); // Hard
        });

        View.OnClickListener okListener = v -> {
            hardClicks = 0;
            switchFoxToNormal();
            showRandomPhrase();
            if (v.getId() == R.id.btnMedium) gradeAndNext(4);
            else gradeAndNext(5);
        };
        btnMedium.setOnClickListener(okListener);
        btnEasy.setOnClickListener(okListener);

        // Show translation
        btnShowTranslation.setOnClickListener(v -> {
            Card c = queue.peekFirst();
            if (c == null) return;
            tvTranslation.setText(c.getBack());
            showAnswerState(); // show translation and ONLY difficulty buttons
            showRandomPhrase(); // new support phrase when showing answer
        });
    }

    // ---------------------------
    // Screen states
    // ---------------------------

    private void showQuestionState() {
        // Question: translation hidden, difficulty buttons hidden, "Show translation" visible
        tvTranslation.setVisibility(View.GONE);
        btnDifficultyLayout.setVisibility(View.GONE);
        btnShowTranslation.setVisibility(View.VISIBLE);
    }

    private void showAnswerState() {
        // Answer: translation visible, difficulty buttons visible, "Show translation" hidden
        tvTranslation.setVisibility(View.VISIBLE);
        btnDifficultyLayout.setVisibility(View.VISIBLE);
        btnShowTranslation.setVisibility(View.GONE);
    }

    // ---------------------------
    // Business logic
    // ---------------------------

    private void loadDueCards() {
        AppDatabase.databaseExecutor.execute(() -> {
            long now = System.currentTimeMillis();

            int cardsBefore   = db.reviewDao().countStates(deckId);
            db.reviewDao().seedReviewState(deckId, now);

            int excl          = db.reviewDao().countExcluded(deckId);
            int learnedCards  = db.reviewDao().countLearnedCards(deckId);
            int states        = db.reviewDao().countStates(deckId);
            int dueN          = db.reviewDao().countDue(deckId, now);

            List<Card> due = repo.getDueCards(deckId, now, 800);

            runOnUiThread(() -> {
                /*Toast.makeText(
                        this,
                        "states(before)=" + cardsBefore +
                                "  excl=" + excl +
                                "  learned(cards)=" + learnedCards +
                                "  states=" + states +
                                "  due=" + dueN +
                                "  queued=" + (due != null ? due.size() : 0),
                        Toast.LENGTH_LONG
                ).show();*/

                queue.clear();
                if (due != null) queue.addAll(due);
                showNext();
            });
        });
    }

    private void showNext() {
        Card c = queue.peekFirst();
        if (c == null) {
            tvWord.setText("No cards to review");
            tvTranslation.setVisibility(View.GONE);
            btnDifficultyLayout.setVisibility(View.GONE);
            btnShowTranslation.setVisibility(View.GONE);
            setButtonsEnabled(false);
            return;
        }

        tvWord.setText(c.getFront());
        tvTranslation.setText("");
        showQuestionState();         // each new card starts in "question" mode
        setButtonsEnabled(true);
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
            runOnUiThread(this::showNext);
        });
    }

    // ---------------------------
    // Speech bubble + fox
    // ---------------------------

    private void showRandomPhrase() {
        if (bubbleText == null) return;
        String phrase = normalPhrases[rnd.nextInt(normalPhrases.length)];
        setBubbleText(phrase);
    }

    private void setBubbleText(String text) {
        if (bubbleText == null) return;
        bubbleText.animate().alpha(0f).setDuration(120).withEndAction(() -> {
            bubbleText.setText(text);
            bubbleText.animate().alpha(1f).setDuration(120).start();
        }).start();
    }

    private void switchFoxToSupport() {
        crossfadeFox(R.drawable.fox_support);
    }

    private void switchFoxToNormal() {
        crossfadeFox(R.drawable.fox_study);
    }

    private void crossfadeFox(int drawableRes) {
        if (foxImage == null) return;
        foxImage.animate().alpha(0f).setDuration(120).withEndAction(() -> {
            foxImage.setImageResource(drawableRes);
            foxImage.animate().alpha(1f).setDuration(120).start();
        }).start();
    }
}
