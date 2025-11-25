
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

/**
 * MainMenuActivity
 *
 * Central hub screen of the application. Displays a list of available decks,
 * provides navigation through a DrawerLayout, and allows access to Settings,
 * About screen, and Exit dialog.
 *
 * Responsibilities:
 * - Initialize UI components (Toolbar, Drawer, RecyclerView).
 * - Load predefined deck list.
 * - Handle navigation menu selections.
 * - Open DeckActivity when a deck is selected.
 * - Apply theme from saved preferences.
 */
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
        // Apply the selected theme (light/dark) stored in preferences.
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // ==== Drawer / Toolbar initialization ====
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        // Exit button: opens a confirmation dialog before closing the app.
        ImageButton btnExit = findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(v -> showExitDialog());

        // Setup toolbar and attach drawer-opening behavior.
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // Navigation menu: handles Settings, About, and Home.
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

        // ==== RecyclerView with deck list ====
        rvDecks = findViewById(R.id.decksList);
        rvDecks.setLayoutManager(new LinearLayoutManager(this));

        // Optional divider between items:
        // rvDecks.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        rvDecks.setClipToPadding(false);

        // Custom item decoration for overlapping card effect.
        rvDecks.addItemDecoration(new OverlapDecoration(this, 0, 0));

        // Predefined deck names (can be replaced with dynamic data source).
        String[] deckNames = {
                "Words Level 1, Part 1",
                "Words Level 1, Part 2",
                "Words Level 2, Part 1",
                "Words Level 2, Part 2",
                "Words Level 2, Part 3",
                "Words Level 3, Part 1",
                "Words Level 3, Part 2",
                "Words Level 3, Part 3",
                "Words Level 3, Part 4",
                "Words Level 3, Part 5",
                "Words Level 4, Part 1",
                "Words Level 4, Part 2",
                "Words Level 4, Part 3",
                "Words Level 4, Part 4",
                "Words Level 4, Part 5",
                "Words Level 4, Part 6",
                "Words Level 5, Part 1",
                "Words Level 5, Part 2",
                "Words Level 5, Part 3",
                "Words Level 5, Part 4",
                "Words Level 5, Part 5",
                "Words Level 5, Part 6",
                "Words Level 6, Part 1",
                "Words Level 6, Part 2",
                "Words Level 6, Part 3",
                "Words Level 6, Part 4",
                "Words Level 6, Part 5"
        };

        // Convert deck names into Deck objects.
        decks.clear();
        for (int i = 0; i < deckNames.length; i++) {
            decks.add(new Deck(i + 1, (i + 1) + ". " + deckNames[i]));
        }

        // Adapter: clicking on a deck opens the DeckActivity.
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
        // Ensure deck items update progress and refresh UI.
        if (rvDecks != null && rvDecks.getAdapter() != null) {
            rvDecks.getAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Displays an exit confirmation dialog.
     * "Yes" closes the entire application using finishAffinity().
     * "No" simply closes the dialog.
     */
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
        // Inflate top-right menu (Settings icon)
        getMenuInflater().inflate(R.menu.nav_drawer_menu, menu);
        return true;
    }

    /**
     * Handles toolbar menu item clicks.
     * Opens Settings when the settings button is pressed.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
