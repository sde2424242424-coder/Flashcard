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

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_THEME  = "theme_mode";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // apply theme (light/dark) from preferences
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // toolbar back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // theme switch
        SwitchMaterial switchTheme = findViewById(R.id.switch_theme);
        // reset DB button
        MaterialButton btnResetDb = findViewById(R.id.btn_reset_db);

        // initial switch state from shared preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String mode = prefs.getString(KEY_THEME, "light");
        switchTheme.setChecked("dark".equals(mode));

        // theme change
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // true = dark theme
            ThemeHelper.setTheme(this, isChecked);
            recreate();
        });

        // click on "Reset all data"
        btnResetDb.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Data reset")
                    .setMessage("Delete progress and clear all databases?")
                    .setPositiveButton("Yes", (dialog, which) -> resetAllDatabases())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Full reset: Room database + all cards_deck_*.db
     */
    private void resetAllDatabases() {
        new Thread(() -> {
            Context ctx = getApplicationContext();

            // 1. Clear main Room database (cards.db)
            try {
                AppDatabase db = AppDatabase.getInstance(ctx);
                db.clearAllTables();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 2. Optionally delete cards.db file itself
            try {
                ctx.deleteDatabase("cards.db");
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 3. Delete all deck databases cards_deck_*.db
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

            runOnUiThread(() ->
                    Toast.makeText(this, "All data has been reset", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }
}
