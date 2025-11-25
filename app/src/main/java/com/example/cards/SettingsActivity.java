package com.example.cards;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.util.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * SettingsActivity
 *
 * Screen that allows the user to:
 * - Toggle the application theme (light / dark).
 * - Reset all application data (clear main and per-deck databases).
 *
 * Responsibilities:
 * - Read and apply the saved theme mode from SharedPreferences.
 * - Persist theme changes through ThemeHelper.
 * - Provide a confirmation dialog before deleting all data.
 * - Clear all Room databases and show a confirmation message.
 */
public class SettingsActivity extends AppCompatActivity {

    // Name of the SharedPreferences file used to store settings.
    private static final String PREFS_NAME = "app_settings";
    // Key used for storing the current theme mode.
    private static final String KEY_THEME  = "theme_mode";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply theme (light/dark) from preferences before inflating layout.
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Toolbar with back button.
        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Theme switch: toggles between light and dark mode.
        SwitchMaterial switchTheme = findViewById(R.id.switch_theme);
        // "Reset DB" button: clears all app data.
        MaterialButton btnResetDb = findViewById(R.id.btn_reset_db);

        // Initial switch state based on stored theme preference.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String mode = prefs.getString(KEY_THEME, "light");
        // Checked = dark theme; unchecked = light theme.
        switchTheme.setChecked("dark".equals(mode));

        // Handle theme changes when user toggles the switch.
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // true -> dark theme, false -> light theme.
            ThemeHelper.setTheme(this, isChecked);
            // Recreate activity to apply the new theme immediately.
            recreate();
        });

        // Click listener for "Reset all data" button.
        btnResetDb.setOnClickListener(v -> {
            // Show confirmation dialog before deleting all databases.
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Data reset")
                    .setMessage("Delete progress and clear all databases?")
                    .setPositiveButton("Yes", (dialog, which) -> resetAllDatabases())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Performs a full reset of all application data:
     * 1. Clears all tables in the main Room database (cards.db).
     * 2. Deletes the main database file if it exists.
     * 3. Deletes all per-deck databases whose names start with "cards_deck_".
     *
     * This work is done in a background thread and a confirmation Toast is
     * shown on the main thread when the reset completes.
     */
    private void resetAllDatabases() {
        new Thread(() -> {
            Context ctx = getApplicationContext();

            // 1. Clear main Room database (cards.db) tables.
            try {
                AppDatabase db = AppDatabase.getInstance(ctx);
                db.clearAllTables();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 2. Optionally delete the main database file itself.
            try {
                ctx.deleteDatabase("cards.db");
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 3. Delete all deck-specific databases (cards_deck_*.db).
            try {
                String[] dbNames = ctx.databaseList();
                if (dbNames != null) {
                    for (String name : dbNames) {
                        if (name != null && name.startsWith("cards_deck_")) {
                            ctx.deleteDatabase(name);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Notify user on the main thread that all data has been reset.
            runOnUiThread(() ->
                    Toast.makeText(this, "All data has been reset", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }
}
