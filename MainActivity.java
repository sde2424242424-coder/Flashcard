package com.example.flashcard;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.cards.data.db.AppDatabase;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Создание базы
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "cards.db").allowMainThreadQueries().build();

        Log.d("DB", "База создана: " + db.toString());
    }
}
