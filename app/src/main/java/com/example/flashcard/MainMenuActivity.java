package com.example.flashcard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        ImageView logo = findViewById(R.id.logo);
        logo.setAlpha(0f);
        logo.animate().alpha(1f).setDuration(1200).start();

        findViewById(R.id.btn1).setOnClickListener(v ->
                startActivity(new Intent(this, Feature1Activity.class)));
        // Повтори для btn2..btn7
    }
}
