package com.example.cards;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

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
import com.example.cards.ui.FoxDecoration;
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
        // Применяем тему (светлая/тёмная) из настроек
        ThemeHelper.applyThemeFromPrefs(this);

        super.onCreate(savedInstanceState);
        // Разметка с DrawerLayout и RecyclerView @id/decksList
        setContentView(R.layout.activity_main_menu);

        // ==== Drawer / Toolbar ====
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        ImageButton btnExit = findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(v -> showExitDialog());

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(
                    v -> drawerLayout.openDrawer(GravityCompat.START)
            );
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
        rvDecks = findViewById(R.id.decksList);
        rvDecks.setLayoutManager(new LinearLayoutManager(this));

        // Позволяем элементам и декорациям выходить за padding
        rvDecks.setClipToPadding(false);

        // Ваше старое перекрытие (если нужно)
        rvDecks.addItemDecoration(new OverlapDecoration(this, 0, 0));

        // Лиса, привязанная к первой карточке
        rvDecks.addItemDecoration(new FoxDecoration(this));

        // Данные колод
        String[] deckNames = {
                "Words Level 1, Part 1",   // 1
                "Words Level 1, Part 2",   // 2
                "Words Level 2, Part 1",   // 3
                "Words Level 2, Part 2",   // 4
                "Words Level 2, Part 3",   // 5
                "Words Level 3, Part 1",   // 6
                "Words Level 3, Part 2",   // 7
                "Words Level 3, Part 3",   // 8
                "Words Level 3, Part 4",   // 9
                "Words Level 3, Part 5",   // 10
                "Words Level 4, Part 1",   // 11
                "Words Level 4, Part 2",   // 12
                "Words Level 4, Part 3",   // 13
                "Words Level 4, Part 4",   // 14
                "Words Level 4, Part 5",   // 15
                "Words Level 4, Part 6",   // 16
                "Words Level 5, Part 1",   // 17
                "Words Level 5, Part 2",   // 18
                "Words Level 5, Part 3",   // 19
                "Words Level 5, Part 4",   // 20
                "Words Level 5, Part 5",   // 21
                "Words Level 5, Part 6",   // 22
                "Words Level 6, Part 1",   // 23
                "Words Level 6, Part 2",   // 24
                "Words Level 6, Part 3",   // 25
                "Words Level 6, Part 4",   // 26
                "Words Level 6, Part 5"    // 27
        };

        decks.clear();
        for (int i = 0; i < deckNames.length; i++) {
            int id = i + 1;
            String title = id + ". " + deckNames[i];
            decks.add(new Deck(id, title));
        }

        // Адаптер: при нажатии открываем экран колоды (DeckActivity)
        adapter = new DeckAdapter(decks, deck -> {
            Intent intent = new Intent(MainMenuActivity.this, DeckActivity.class);
            intent.putExtra(DeckActivity.EXTRA_DECK_ID, deck.id);
            intent.putExtra(DeckActivity.EXTRA_DECK_TITLE, deck.title);
            startActivity(intent);
        });

        rvDecks.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем элементы — адаптер пересчитает прогресс для каждой колоды
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

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Меню в тулбаре
        getMenuInflater().inflate(R.menu.nav_drawer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Пункт меню "настройки"
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
