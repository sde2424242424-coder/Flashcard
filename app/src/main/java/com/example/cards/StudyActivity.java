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

/**
 * StudyActivity
 *
 * Screen responsible for running a review session for a single deck
 * using spaced-repetition logic. Displays one card at a time, allows
 * the user to reveal the translation, and then choose a difficulty
 * (Easy / Medium / Hard) which affects the next review time.
 *
 * Additional responsibilities:
 * - Loads due cards for the selected deck from the database.
 * - Manages a queue of cards to review.
 * - Updates review state in the database based on user grading.
 * - Shows a fox character with supportive speech bubble messages.
 */
public class StudyActivity extends AppCompatActivity {

    // Repository for spaced-repetition review logic.
    private ReviewRepository repo;

    // Queue of cards to be reviewed in the current session.
    private final ArrayDeque<Card> queue = new ArrayDeque<>();

    // Main controls.
    private Button btnShowTranslation, btnEasy, btnMedium, btnHard;
    private TextView tvWord, tvTranslation;

    // ---- Fox character + speech bubble UI ----
    private ImageView foxImage;
    private TextView bubbleText;
    private int hardClicks = 0;
    private final Random rnd = new Random();

    // Default supportive phrases (shown in normal fox mode).
    private final String[] normalPhrases = new String[] {
            "Great! Just a little more 🦊",
            "You’re doing great, keep it up 💪",
            "Every word makes you stronger ✨",
            "You’re doing well! The fox is proud of you 🧡"
    };

    // Special phrase after three consecutive "Hard" clicks.
    private final String hard3Phrase =
            "It’s okay! Even the fox doesn’t understand everything the first time 🦊💤";

    // Container for difficulty buttons.
    private LinearLayout btnDifficultyLayout;

    // Database and current deck identifier.
    private AppDatabase db;
    private long deckId = 1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply selected theme (light/dark) before inflating the layout.
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

        // Toolbar back arrow closes this activity.
        toolbar.setNavigationOnClickListener(v -> finish());

        // ----- Extras -----
        // Deck ID passed from DeckActivity. Default value is 1L if missing.
        deckId = getIntent().getLongExtra(DeckActivity.EXTRA_DECK_ID, 1L);

        // ----- DB/Repo -----
        db = AppDatabase.DbFactory.forDeck(this, deckId);
        repo = new ReviewRepository(db.reviewDao());

        // Initial UI state before cards are loaded.
        showQuestionState();
        setButtonsEnabled(false);
        showRandomPhrase();         // initial bubble phrase
        switchFoxToNormal();        // initial fox sprite

        // Load all due cards for the current deck.
        loadDueCards();

        // Answers (with fox + phrase logic).
        // "Hard" button: after 3 consecutive presses shows a special supportive message.
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
            gradeAndNext(3); // Grade "Hard"
        });

        // Shared listener for Easy / Medium.
        View.OnClickListener okListener = v -> {
            hardClicks = 0;
            switchFoxToNormal();
            showRandomPhrase();

            // Map button to grade value: Medium=4, Easy=5
            if (v.getId() == R.id.btnMedium) gradeAndNext(4);
            else gradeAndNext(5);
        };
        btnMedium.setOnClickListener(okListener);
        btnEasy.setOnClickListener(okListener);

        // Show translation: reveals the back side of the current card
        // and switches the screen to "answer" mode.
        btnShowTranslation.setOnClickListener(v -> {
            Card c = queue.peekFirst();
            if (c == null) return;
            tvTranslation.setText(c.getBack());
            showAnswerState(); // show translation and ONLY difficulty buttons
            showRandomPhrase(); // new supportive phrase when showing answer
        });
    }

    // ---------------------------
    // Screen states
    // ---------------------------

    /**
     * Puts the screen into "question" mode:
     * - Hides translation and difficulty buttons.
     * - Shows the "Show translation" button.
     */
    private void showQuestionState() {
        tvTranslation.setVisibility(View.GONE);
        btnDifficultyLayout.setVisibility(View.GONE);
        btnShowTranslation.setVisibility(View.VISIBLE);
    }

    /**
     * Puts the screen into "answer" mode:
     * - Shows translation and difficulty buttons.
     * - Hides the "Show translation" button.
     */
    private void showAnswerState() {
        tvTranslation.setVisibility(View.VISIBLE);
        btnDifficultyLayout.setVisibility(View.VISIBLE);
        btnShowTranslation.setVisibility(View.GONE);
    }

    // ---------------------------
    // Business logic
    // ---------------------------

    /**
     * Loads due cards for the current deck on a background thread.
     * Seeds review state if necessary, then enqueues up to 800 due cards.
     * Once data is loaded, updates the UI on the main thread.
     */
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
                /* Debug toast left as a reference for troubleshooting:
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
                */

                queue.clear();
                if (due != null) queue.addAll(due);
                showNext();
            });
        });
    }

    /**
     * Displays the next card from the queue.
     * If there are no cards left, shows a message and disables controls.
     */
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

    /**
     * Enables or disables all review-related buttons.
     *
     * @param enabled true to enable all buttons, false to disable them.
     */
    private void setButtonsEnabled(boolean enabled) {
        btnHard.setEnabled(enabled);
        btnMedium.setEnabled(enabled);
        btnEasy.setEnabled(enabled);
        btnShowTranslation.setEnabled(enabled);
    }

    /**
     * Applies the given grade to the current card, schedules its next review,
     * and moves on to the next card. Database work is done in a background thread.
     *
     * @param grade integer grade code (e.g. 3=Hard, 4=Medium, 5=Easy).
     */
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

    /**
     * Chooses a random supportive phrase from {@link #normalPhrases}
     * and displays it in the speech bubble with a fade animation.
     */
    private void showRandomPhrase() {
        if (bubbleText == null) return;
        String phrase = normalPhrases[rnd.nextInt(normalPhrases.length)];
        setBubbleText(phrase);
    }

    /**
     * Sets a new text into the speech bubble, using a small crossfade animation.
     *
     * @param text phrase to display inside the bubble.
     */
    private void setBubbleText(String text) {
        if (bubbleText == null) return;
        bubbleText.animate().alpha(0f).setDuration(120).withEndAction(() -> {
            bubbleText.setText(text);
            bubbleText.animate().alpha(1f).setDuration(120).start();
        }).start();
    }

    /**
     * Switches fox image to a "support" pose, used after three "Hard" clicks.
     */
    private void switchFoxToSupport() {
        crossfadeFox(R.drawable.fox_support);
    }

    /**
     * Switches fox image to the default "study" pose.
     */
    private void switchFoxToNormal() {
        crossfadeFox(R.drawable.fox_study);
    }

    /**
     * Crossfades the fox image to the given drawable resource.
     *
     * @param drawableRes drawable resource ID for the new fox image.
     */
    private void crossfadeFox(int drawableRes) {
        if (foxImage == null) return;
        foxImage.animate().alpha(0f).setDuration(120).withEndAction(() -> {
            foxImage.setImageResource(drawableRes);
            foxImage.animate().alpha(1f).setDuration(120).start();
        }).start();
    }
}
