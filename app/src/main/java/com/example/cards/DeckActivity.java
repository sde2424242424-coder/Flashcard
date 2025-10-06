package com.example.cards;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.Card;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class DeckActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CardAdapter adapter;

    private long deckId;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck);

        recyclerView = findViewById(R.id.recycler);
        if (recyclerView == null) {
            throw new IllegalStateException("В activity_deck.xml должен быть RecyclerView с id 'recycler'");
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        deckId = getIntent().getLongExtra("deck_id", 1L);
        db = AppDatabase.DbFactory.forDeck(this, deckId = 1); // отдельная БД на колоду (read-only контент)

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView deckTitle = findViewById(R.id.deckTitle);
        Button btnStudy = findViewById(R.id.btnStudy);

        String title = getIntent().getStringExtra("deck_title");
        if (title != null) deckTitle.setText(title);

        toolbar.setNavigationOnClickListener(v -> finish());

        //recyclerView = findViewById(R.id.recycler);
        adapter = new CardAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadCards();

        btnStudy.setOnClickListener(v -> {
            Intent i = new Intent(DeckActivity.this, StudyActivity.class);
            i.putExtra("deck_id", deckId);
            i.putExtra("deck_title", deckTitle.getText().toString());
            startActivity(i);
        });

        // НИКАКИХ кнопок добавления/редактирования
        // Убедись, что в layout нет btnAdd/fabAdd. Если есть — удали/скрой.
    }

    private void loadCards() {
        AppDatabase.databaseExecutor.execute(() -> {
            List<Card> list = db.cardDao().getAll(); // для пер-колодной БД getAll() ОК
            runOnUiThread(() -> adapter.submitList(list));
        });
    }
}
