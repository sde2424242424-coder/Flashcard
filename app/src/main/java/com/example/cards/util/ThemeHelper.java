package com.example.cards.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * ThemeHelper
 *
 * Utility class responsible for storing and applying the app's theme mode
 * (light or dark). The selected mode is persisted in SharedPreferences and
 * restored every time an activity starts.
 *
 * Responsibilities:
 * - Read theme preference when the app or activity launches.
 * - Save new theme setting when the user toggles the switch.
 * - Apply light/dark mode using AppCompatDelegate.
 */
public class ThemeHelper {

    // SharedPreferences file name.
    private static final String PREFS_NAME = "app_settings";

    // Theme preference key: values are "light" or "dark".
    private static final String KEY_THEME = "theme_mode";

    /**
     * Applies the stored theme mode.
     * Should be called before setContentView() in each activity.
     *
     * @param context activity or application context
     */
    public static void applyThemeFromPrefs(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String mode = prefs.getString(KEY_THEME, "light");

        if ("dark".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Saves and applies the new theme mode (light/dark).
     * Called when the user toggles the theme switch.
     *
     * @param context activity or application context
     * @param dark    true for dark theme, false for light theme
     */
    public static void setTheme(Context context, boolean dark) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putString(KEY_THEME, dark ? "dark" : "light")
                .apply();

        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
