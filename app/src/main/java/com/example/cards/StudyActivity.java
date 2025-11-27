package com.example.cards;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.Card;
import com.example.cards.data.model.WordWithStats;
import com.example.cards.domain.ReviewRepository;
import com.example.cards.util.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * StudyActivity
 *
 * Runs a review session for a single deck:
 * - Loads only unlearned and not-excluded cards (CardDao.getSelection).
 * - Cycles through this selection in random order.
 * - When the queue ends but there are still unlearned cards, the selection is loaded again
 *   and shuffled (infinite cycle until all cards become learned).
 * - When all cards are learned, shows a final message.
 */
public class StudyActivity extends AppCompatActivity {

    private ReviewRepository repo;
    private final ArrayDeque<Card> queue = new ArrayDeque<>();

    private Button btnShowTranslation, btnEasy, btnMedium, btnHard;
    private TextView tvWord, tvTranslation;

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

        // Initial UI
        showQuestionState();
        setButtonsEnabled(false);
        showRandomPhrase();
        switchFoxToNormal();

        // Load first selection
        loadSelection();

        // ---- Buttons ----

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
            if (v.getId() == R.id.btnMedium) {
                gradeAndNext(4); // Medium
            } else {
                gradeAndNext(5); // Easy
            }
        };
        btnMedium.setOnClickListener(okListener);
        btnEasy.setOnClickListener(okListener);

        btnShowTranslation.setOnClickListener(v -> {
            Card c = queue.peekFirst();
            if (c == null) return;
            tvTranslation.setText(c.getBack());
            showAnswerState();
            showRandomPhrase();
        });
    }

    // ---------------------------
    // Screen states
    // ---------------------------

    private void showQuestionState() {
        tvTranslation.setVisibility(View.GONE);
        btnDifficultyLayout.setVisibility(View.GONE);
        btnShowTranslation.setVisibility(View.VISIBLE);
    }

    private void showAnswerState() {
        tvTranslation.setVisibility(View.VISIBLE);
        btnDifficultyLayout.setVisibility(View.VISIBLE);
        btnShowTranslation.setVisibility(View.GONE);
    }

    // ---------------------------
    // Data loading / selection
    // ---------------------------

    /**
     * Loads a selection of unlearned & non-excluded cards for the deck.
     * If there are no such cards, shows a final "all learned" message.
     * If there are cards, shuffles them and starts (or restarts) the cycle.
     */
    private void loadSelection() {
        AppDatabase.databaseExecutor.execute(() -> {
            long now = System.currentTimeMillis();

            // Ensure review_state rows exist. This does not affect selection filters.
            db.reviewDao().seedReviewState(deckId, now);

            List<WordWithStats> selection = db.cardDao().getSelection(deckId, 800);

            // Convert to Card and shuffle
            List<Card> cards = new ArrayList<>();
            if (selection != null && !selection.isEmpty()) {
                Collections.shuffle(selection);
                for (WordWithStats w : selection) {
                    Card c = new Card();
                    c.id = w.cardId;
                    c.deckId = deckId;
                    c.front = (w.front != null) ? w.front : "";
                    c.back  = (w.back  != null) ? w.back  : "";
                    cards.add(c);
                }
            }

            runOnUiThread(() -> {
                queue.clear();
                if (cards.isEmpty()) {
                    // No unlearned & non-excluded cards left – stop the cycle.
                    tvWord.setText("All cards are learned");
                    tvTranslation.setVisibility(View.GONE);
                    btnDifficultyLayout.setVisibility(View.GONE);
                    btnShowTranslation.setVisibility(View.GONE);
                    setButtonsEnabled(false);
                } else {
                    queue.addAll(cards);
                    showNext();
                }
            });
        });
    }

    /**
     * Shows the next card in the queue.
     * If the queue is empty but there are still unlearned cards,
     * a new selection is loaded (cycle continues).
     */
    private void showNext() {
        Card c = queue.peekFirst();
        if (c == null) {
            // Queue ended: reload selection (infinite cycle for unlearned words).
            loadSelection();
            return;
        }

        tvWord.setText(c.getFront());
        tvTranslation.setText("");
        showQuestionState();
        setButtonsEnabled(true);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnHard.setEnabled(enabled);
        btnMedium.setEnabled(enabled);
        btnEasy.setEnabled(enabled);
        btnShowTranslation.setEnabled(enabled);
    }

    /**
     * Applies grade, updates SM-2 state and moves to the next card.
     */
    private void gradeAndNext(int grade) {
        Card current = queue.pollFirst();
        if (current == null) {
            showNext();
            return;
        }

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
        if (bubbleText == null || normalPhrases.length == 0) return;
        String phrase = normalPhrases[rnd.nextInt(normalPhrases.length)];
        setBubbleText(phrase);
    }

    private void setBubbleText(String text) {
        if (bubbleText == null) return;
        bubbleText.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    bubbleText.setText(text);
                    bubbleText.animate().alpha(1f).setDuration(120).start();
                })
                .start();
    }

    private void switchFoxToSupport() {
        crossfadeFox(R.drawable.fox_support);
    }

    private void switchFoxToNormal() {
        crossfadeFox(R.drawable.fox_study);
    }

    private void crossfadeFox(int drawableRes) {
        if (foxImage == null) return;
        foxImage.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    foxImage.setImageResource(drawableRes);
                    foxImage.animate().alpha(1f).setDuration(120).start();
                })
                .start();
    }
}
