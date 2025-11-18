package com.example.cards;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.data.model.Deck;
import com.example.cards.ui.DeckAdapter;
import com.example.cards.ui.OverlapDecoration;
import com.example.cards.util.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainMenuActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    private RecyclerView rvDecks;
    private DeckAdapter adapter;
    private final List<Deck> decks = new ArrayList<>();

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu); // layout с DrawerLayout и RecyclerView @id/decksList

        // ==== Drawer / Toolbar ====
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);
        ImageButton btnExit = findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(v -> showExitDialog());



        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
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
        }

        // ==== RecyclerView со списком колод ====
        rvDecks = findViewById(R.id.decksList); // <= твой id из XML
        rvDecks.setLayoutManager(new LinearLayoutManager(this));

        // Если нужен разделитель между элементами — раскомментируй:
        // rvDecks.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        rvDecks.setClipToPadding(false);
        rvDecks.addItemDecoration(new OverlapDecoration(this, 0, 0));

        //rvDecks.setClipToPadding(false);
// было 120dp → слишком много. Используй 48dp подъёма и 12dp зазор.
        //rvDecks.addItemDecoration(new OverlapDecoration(this, 48f, 12f));

        // Данные по колодам (подставь свой источник, если есть)
        String[] deckNames = {
                "Слова 1급",
                "Слова 2급",
                "Слова 3급",
                "Слова 4급",
                "Слова 5급",
                "Слова 6급"
        };
        decks.clear();
        for (int i = 0; i < deckNames.length; i++) {
            decks.add(new Deck(i + 1, (i + 1) + ". " + deckNames[i]));
        }

        // Адаптер: клик открывает экран колоды (DeckActivity)
        adapter = new DeckAdapter(decks, deck -> {
            Intent i = new Intent(MainMenuActivity.this, DeckActivity.class);
            i.putExtra(DeckActivity.EXTRA_DECK_ID, deck.id);
            i.putExtra(DeckActivity.EXTRA_DECK_TITLE, deck.title);
            startActivity(i);
        });
        rvDecks.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перебиндить элементы → адаптер снова посчитает % для каждой колоды
        if (rvDecks != null && rvDecks.getAdapter() != null) {
            rvDecks.getAdapter().notifyDataSetChanged();
        }
    }
    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_exit, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        Button btnYes = view.findViewById(R.id.btn_yes);
        Button btnNo = view.findViewById(R.id.btn_no);

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
        });

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.nav_drawer_menu, menu); // твой xml меню
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) { // id пункта меню "настройки"
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }





}
