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
        // применяем тему (день/ночь)
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // тулбар "назад"
        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // переключатель темы
        SwitchMaterial switchTheme = findViewById(R.id.switch_theme);
        // кнопка сброса БД
        MaterialButton btnResetDb = findViewById(R.id.btn_reset_db);

        // начальное состояние свитчера из настроек
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String mode = prefs.getString(KEY_THEME, "light");
        switchTheme.setChecked("dark".equals(mode));

        // смена темы
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // true = тёмная
            ThemeHelper.setTheme(this, isChecked);
            recreate();
        });

        // клик по "Сбросить все данные"
        btnResetDb.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Сброс данных")
                    .setMessage("Удалить прогресс и очистить все базы данных?")
                    .setPositiveButton("Да", (dialog, which) -> resetAllDatabases())
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    /** Полный сброс: Room-база + все cards_deck_*.db */
    private void resetAllDatabases() {
        new Thread(() -> {
            Context ctx = getApplicationContext();

            // 1. Очистить основную Room-базу (cards.db)
            try {
                AppDatabase db = AppDatabase.getInstance(ctx);
                db.clearAllTables();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 2. При желании удалить сам файл cards.db
            try {
                ctx.deleteDatabase("cards.db");
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 3. Удалить все БД колод вида cards_deck_*.db
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
                    Toast.makeText(this, "Все данные сброшены", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }
}
