package com.example.cards;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cards.util.ThemeHelper;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);

        TextView aboutText = findViewById(R.id.aboutText);

        //String versionName = BuildConfig.VERSION_NAME;
        String appName = getString(R.string.app_name);

        String about =
                appName + "\n" +
                        "Version: " + "1.0" + "\n\n" +
                        "Description:\n" +
                        "This is a cute flashcard application that helps you learn words calmly and gradually, track your progress\n" +
                        ", and customize the appearance to your liking.\n\n" +
                        "What you can do:\n" +
                        "• study words by levels and databases;\n" +
                        "• mark learned and skipped cards;\n" +
                        "• track progress for each database;\n" +
                        "• choose a theme (with the fox);\n" +
                        "• adjust card display to fit your style.\n\n" +
                        "Thank you for using the app!";


        aboutText.setText(about);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
