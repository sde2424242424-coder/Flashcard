package com.example.cards;

import static com.example.cards.R.layout.activity_brand;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class BrandActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_brand);

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
    handler.postDelayed(() -> {
        startActivity(new Intent(this, MainMenuActivity.class));
        finish();
    }, 1000);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);

    }
}