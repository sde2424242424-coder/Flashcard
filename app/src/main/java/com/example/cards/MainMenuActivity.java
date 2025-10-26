package com.example.cards;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.db.CardDao;
import com.example.cards.data.db.DbProvider;
import com.example.cards.data.db.ReviewDao;
import com.example.cards.data.model.Card;
import com.example.cards.data.model.Deck;
import com.example.cards.ui.DeckAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainMenuActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_menu); // XML с DrawerLayout

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        // Открытие меню по нажатию на иконку в Toolbar
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Список колод
        RecyclerView rv = findViewById(R.id.decksList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        String[] deckNames = {
                "Слова 1급",
                "Слова 2급",
                "Слова 3급",
                "Слова 4급",
                "Слова 5급",
                "Слова 6급"
        };

        List<Deck> decks = new ArrayList<>();
        for (int i = 0; i < deckNames.length; i++) {
            decks.add(new Deck(i + 1, (i + 1) + ". " + deckNames[i]));
        }

        DeckAdapter adapter = new DeckAdapter(decks, deck -> {
            // MainMenuActivity
            Intent i = new Intent(MainMenuActivity.this, DeckActivity.class);
            i.putExtra(DeckActivity.EXTRA_DECK_ID, deck.id);
            i.putExtra(DeckActivity.EXTRA_DECK_TITLE, deck.title);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        // Асинхронно рассчитываем процент выученных слов для каждой колоды
        AppDatabase.databaseExecutor.execute(() -> {
            for (int i = 0; i < decks.size(); i++) {
                Deck deck = decks.get(i);
                try {
                    // Получаем per-deck DB (DbProvider.getDatabase кэширует экземпляры)
                    AppDatabase db = DbProvider.getDatabase(this, deck.id);
                    CardDao cardDao = db.cardDao();
                    ReviewDao reviewDao = db.reviewDao();

                    // total карт в этой БД
                    int total = 0;
                    try {
                        total = cardDao.countAll();
                    } catch (Exception e) {
                        Log.w("MainMenu", "countAll failed for deck " + deck.id, e);
                    }

                    // learned — сначала через DAO (SQL), это быстрее и безопаснее
                    int learned = 0;
                    try {
                        learned = cardDao.countLearnedCards(deck.id);
                    } catch (Exception e) {
                        Log.w("MainMenu", "countLearnedCards failed for deck " + deck.id + ", fallback to scanning", e);
                        // fallback: посчитать в памяти, если в схеме нет countLearnedCards или если запрос не подходит
                        try {
                            List<Card> all = cardDao.getAll(); // может быть тяжёлым для больших БД
                            int localLearned = 0;
                            for (Card c : all) {
                                if (c == null) continue;
                                // Card.learned у вас boolean (Room maps 0/1 -> false/true)
                                try {
                                    if (c.learned) localLearned++;
                                } catch (Throwable ignore) {
                                    // на случай, если поле отсутствует в runtime модели
                                }
                            }
                            learned = localLearned;
                        } catch (Exception ex) {
                            Log.w("MainMenu", "fallback scan failed for deck " + deck.id, ex);
                        }
                    }

                    final int pct = (total == 0) ? 0 : (int) Math.round(learned * 100.0 / total);
                    deck.setPercent(pct);

                    final int pos = i;
                    runOnUiThread(() -> adapter.notifyItemChanged(pos));
                } catch (Exception e) {
                    Log.w("MainMenu", "Failed to compute percent for deck " + deck.id, e);
                }
            }
        });
    }
}