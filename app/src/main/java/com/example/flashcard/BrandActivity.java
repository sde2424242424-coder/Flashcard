package com.example.flashcard;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class BrandActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brand);

        View logo = findViewById(R.id.imgLogo);

        logo.setAlpha(0f);
        logo.animate()
            .alpha(1f)
            .setDuration(1500)
            .withEndAction(() -> {
                logo.animate()
                    .setStartDelay(700)
                    .alpha(0f)
                    .setDuration(1200)
                    .start();
            })
            .start();
    }   
}