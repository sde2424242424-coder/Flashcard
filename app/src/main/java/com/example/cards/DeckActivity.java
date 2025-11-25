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

/**
 * DeckActivity
 *
 * Screen that represents a single vocabulary deck.
 * Responsibilities:
 * - Display deck title, subtitle (level), and description.
 * - Open deck-specific database file (cards_deck_{deckId}.db) via AppDatabase.DbFactory.
 * - Provide navigation to:
 *   - {@link WordListActivity}: full list of words in this deck.
 *   - {@link StudyActivity}: study / review session for this deck.
 *
 * Behavior:
 * - Receives deckId and deckTitle via Intent extras.
 * - Validates deckId (closes if missing).
 * - Logs basic DB information and a sample of cards for debugging.
 */
public class DeckActivity extends AppCompatActivity {

    /** Intent extra: logical deck ID (1..N). */
    public static final String EXTRA_DECK_ID    = "deckId";
    /** Intent extra: human-readable deck title. */
    public static final String EXTRA_DECK_TITLE = "deckTitle";
    /** Optional extra: deck description (currently unused). */
    public static final String EXTRA_DECK_DESC  = "deckDescription";

    private long deckId;
    private CardDao cardDao;
    private AppDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply theme (light/dark) before inflating layout.
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck);

        // --- Read extras ---
        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, -1L);
        String deckTitle = getIntent().getStringExtra(EXTRA_DECK_TITLE);
        // String deckDesc  = getIntent().getStringExtra(EXTRA_DECK_DESC);

        Log.d("DeckActivity", "onCreate: deckId=" + deckId + " title=" + deckTitle);

        if (deckId == -1L) {
            Log.e("DeckActivity", "Missing EXTRA_DECK_ID! Finishing.");
            finish();
            return;
        }

        // --- UI bindings ---
        MaterialToolbar toolbar    = findViewById(R.id.toolbar);
        TextView tvTitle           = findViewById(R.id.tvDeckTitle);
        TextView tvSubtitle        = findViewById(R.id.tvDeckSubtitle);
        TextView tvDesc            = findViewById(R.id.tvDeckDescription);
        MaterialButton btnWordList = findViewById(R.id.btnWordList);
        MaterialButton btnStudy    = findViewById(R.id.btnStudy);

        // --- Title / toolbar setup ---
        if (deckTitle != null && !deckTitle.isEmpty()) {
            tvTitle.setText(deckTitle);
            toolbar.setTitle(deckTitle);
        } else {
            String fallbackTitle = "Deck " + deckId;
            tvTitle.setText(fallbackTitle);
            toolbar.setTitle(fallbackTitle);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Per-deck subtitle and description.
        setupDeckTexts((int) deckId, tvSubtitle, tvDesc);

        // --- Deck-specific database file ---
        String dbFileName = "cards_deck_" + deckId + ".db";
        File expected = getDatabasePath(dbFileName);
        Log.d(
                "DeckActivity",
                "expected DB file = " + expected.getAbsolutePath() +
                        " exists=" + expected.exists() +
                        " size=" + expected.length()
        );

        // Open DB for this deck (factory will copy from assets if needed).
        db = AppDatabase.DbFactory.forDeck(this, deckId);
        cardDao = db.cardDao();

        // Log again after opening (in case DB was just copied).
        Log.d("DeckActivity", "after forDeck: exists=" + expected.exists() + " size=" + expected.length());

        // Optional background diagnostics: print DB path and a small sample of cards.
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

        // --- Buttons ---
        // Open word list screen for this deck.
        btnWordList.setOnClickListener(v -> {
            Intent i = new Intent(this, WordListActivity.class);
            i.putExtra(EXTRA_DECK_ID, deckId);
            startActivity(i);
        });

        // Open study screen for this deck.
        btnStudy.setOnClickListener(v -> {
            Intent i = new Intent(this, StudyActivity.class);
            i.putExtra(EXTRA_DECK_ID, deckId);
            i.putExtra(EXTRA_DECK_TITLE, tvTitle.getText().toString());
            startActivity(i);
        });
    }

    /**
     * Configures subtitle and description for a deck based on its numeric ID.
     * Deck IDs are 1..27 and are mapped to TOPIK-like levels (1급..6급).
     *
     * @param deckId        logical deck ID
     * @param tvSubtitle    TextView for short subtitle (e.g. "for beginners")
     * @param tvDescription TextView for longer per-deck description
     */
    private void setupDeckTexts(int deckId,
                                TextView tvSubtitle,
                                TextView tvDescription) {

        switch (deckId) {
            case 1:
                tvSubtitle.setText("for beginners");
                tvDescription.setText("Basic vocabulary of level 1급, part 1: the most frequent starter words for daily practice.");
                break;

            case 2:
                tvSubtitle.setText("for beginners");
                tvDescription.setText("Basic vocabulary of level 1급, part 2: additional essential words for initial learning and everyday use.");
                break;

            case 3:
                tvSubtitle.setText("keep learning");
                tvDescription.setText("Level 2급 vocabulary, part 1: expanding your word stock for everyday situations and basic conversations.");
                break;

            case 4:
                tvSubtitle.setText("keep learning");
                tvDescription.setText("Level 2급 vocabulary, part 2: more practical words to improve fluency in daily communication.");
                break;

            case 5:
                tvSubtitle.setText("keep learning");
                tvDescription.setText("Level 2급 vocabulary, part 3: additional useful terms for confident interaction in common scenarios.");
                break;

            case 6:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 3급 vocabulary, part 1: more advanced words for conversations and reading simple news.");
                break;

            case 7:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 3급 vocabulary, part 2: expanding your vocabulary for broader topics and written content.");
                break;

            case 8:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 3급 vocabulary, part 3: additional advanced terms to strengthen reading and speaking skills.");
                break;

            case 9:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 3급 vocabulary, part 4: further lexical expansion for more detailed discussions.");
                break;

            case 10:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 3급 vocabulary, part 5: reinforcing advanced usage for daily and academic contexts.");
                break;

            case 11:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 4급 vocabulary, part 1: advanced words suitable for reading news and extended conversations.");
                break;

            case 12:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 4급 vocabulary, part 2: additional terminology to support fluent comprehension and expression.");
                break;

            case 13:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 4급 vocabulary, part 3: more high-level words for understanding various topics.");
                break;

            case 14:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 4급 vocabulary, part 4: strengthening command of complex vocabulary for nuanced situations.");
                break;

            case 15:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 4급 vocabulary, part 5: extended word set for advanced reading and structured dialogues.");
                break;

            case 16:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 4급 vocabulary, part 6: additional complex terms for improved language precision.");
                break;

            case 17:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 5급 vocabulary, part 1: high-level words for professional, academic, and detailed discussions.");
                break;

            case 18:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 5급 vocabulary, part 2: expanding advanced lexical resources for complex texts.");
                break;

            case 19:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 5급 vocabulary, part 3: additional high-level terms for precise communication.");
                break;

            case 20:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 5급 vocabulary, part 4: vocabulary aimed at deeper comprehension of long, informative texts.");
                break;

            case 21:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 5급 vocabulary, part 5: strengthening mastery of advanced expressions across various topics.");
                break;

            case 22:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 5급 vocabulary, part 6: further advanced words for confident reading and speaking at a high level.");
                break;

            case 23:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 6급 vocabulary, part 1: top-level Korean words used in academic, news, and formal contexts.");
                break;

            case 24:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 6급 vocabulary, part 2: additional sophisticated terms for nuanced comprehension.");
                break;

            case 25:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 6급 vocabulary, part 3: vocabulary required for understanding complex articles and discussions.");
                break;

            case 26:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 6급 vocabulary, part 4: extended academic and professional terminology for expert-level use.");
                break;

            case 27:
                tvSubtitle.setText("confident level");
                tvDescription.setText("Level 6급 vocabulary, part 5: the most advanced words for full proficiency in all communication domains.");
                break;

            default:
                tvSubtitle.setText("deck");
                tvDescription.setText("Deck description will be added soon.");
                break;
        }
    }
}
