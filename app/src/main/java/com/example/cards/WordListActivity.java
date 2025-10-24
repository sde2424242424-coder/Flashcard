package com.example.cards;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;            // <--- добавьте это
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.appcompat.widget.SearchView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.Card;
import com.example.cards.data.model.WordWithStats;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Objects;

public class WordListActivity extends AppCompatActivity {

    private long deckId;
    private AppDatabase db;
    private ListView listView; // сохраним ссылку
    private WordAdapter adapter;

    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        // 1) достаём deckId — поддержим оба варианта ключа на всякий случай
        long deckIdFromCamel = getIntent().getLongExtra("deckId", -1L);
        long deckIdFromSnake = getIntent().getLongExtra("deck_id", -1L);
        deckId = (deckIdFromCamel != -1L) ? deckIdFromCamel
                : (deckIdFromSnake != -1L ? deckIdFromSnake : 1L);

        // 2) открываем именно БД этой колоды
        db = AppDatabase.DbFactory.forDeck(this, deckId);

        // 3) полезный лог-проверка: какой файл реально открылся
        android.util.Log.d("DB", "WordList uses deckId=" + deckId
                + " file=" + getDatabasePath("cards_deck_" + deckId + ".db"));


        listView = findViewById(R.id.listWords);
        if (listView == null) throw new IllegalStateException("ListView R.id.listWords not found");

        TextInputEditText searchInput = findViewById(R.id.searchInput);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s == null ? "" : s.toString().trim();
                AppDatabase.databaseExecutor.execute(() -> {
                    List<WordWithStats> data = db.cardDao().searchWords(deckId, q);
                    runOnUiThread(() -> adapter.updateData(data));
                });
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });




        AppDatabase.databaseExecutor.execute(() -> {
            List<WordWithStats> rows = db.cardDao().getWordsWithStatsAll();
            runOnUiThread(() -> {
                adapter = new WordAdapter(WordListActivity.this, rows, db);
                listView.setAdapter(adapter);
            });
        });

    }

    @Override
    protected void onPause() {
        // Гасим рано...
        clearTooltips(getWindow().getDecorView());
        super.onPause();
    }

    @Override
    protected void onStop() {
        // ...и ещё раз прямо перед разрушением окна (важно вызвать ДО super)
        clearTooltips(getWindow().getDecorView());
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // Доп. страховка: разлинковать адаптер, чтобы уничтожились вью айтемов
        if (listView != null) listView.setAdapter(null);
        super.onDestroy();
    }

    private void clearTooltips(View v) {
        try {
            TooltipCompat.setTooltipText(v, null); // снять tooltipText у этого View
        } catch (Throwable ignored) {}
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                clearTooltips(vg.getChildAt(i));
            }
        }
        // На случай, если где-то включали свои ховеры/лонг-клик для тултипов
        v.setOnHoverListener(null);
        v.setOnLongClickListener(null);
        // v.removeCallbacks(...) — УДАЛЕНО: null передавать нельзя
    }
    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_word_list, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
        searchView.setOnCloseListener(() -> {
            adapter.getFilter().filter("");
            return false;
        });
        return true;
    }*/
}
