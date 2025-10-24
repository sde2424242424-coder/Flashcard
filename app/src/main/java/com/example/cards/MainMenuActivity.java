package com.example.cards;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
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

import java.io.File;
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

        /*long deckId = getIntent().getLongExtra("deck_id", 1L);

        // 🔍 Проверяем какой файл реально используется
        File f = getDatabasePath("cards_deck_" + deckId + ".db");
        Log.d("DB", "Using DB: " + f.getAbsolutePath() +
                " size=" + f.length() +
                " mtime=" + new java.util.Date(f.lastModified()));

        // Теперь открываем базу как обычно
        AppDatabase db = DbProvider.forDeck(this, deckId);
        CardDao cardDao = db.cardDao();
        ReviewDao reviewDao = db.reviewDao();*/

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        // Открытие меню по нажатию на иконку в Toolbar
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Обработка пунктов бокового меню
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Мы уже на главном — просто закрываем меню.
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Список колод
        RecyclerView rv = findViewById(R.id.decksList);
        rv.setLayoutManager(new LinearLayoutManager(this));
       // rv.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));


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



        // ===== БАЗА ДАННЫХ: пример чтения карточек из колоды №1 =====
        /*AppDatabase db = AppDatabase.getInstance(this);
        new Thread(() -> {
            List<Card> cards = db.cardDao().getByDeck(1L).getValue(); // колода №1
            runOnUiThread(() -> {
                // здесь обнови UI/лог или передай данные дальше
                // например можно просто залогировать размер:
                // Toast.makeText(this, "В колоде №1: " + cards.size() + " карт", Toast.LENGTH_SHORT).show();
            });
        }).start();*/
        // ============================================================
    }
}
