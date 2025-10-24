package com.example.cards;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.WordWithStats;
import com.example.cards.data.model.WordWithSchedule;

import java.util.List;

public class WordAdapter extends ArrayAdapter<WordWithStats> {

    private final AppDatabase db;  // база данных для обновления состояния

    public WordAdapter(@NonNull Context context, @NonNull List<WordWithStats> words, @NonNull AppDatabase db) {
        super(context, 0, words);
        this.db = db;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_word, parent, false);
        }

        WordWithStats w = getItem(position);
        if (w == null) return convertView; // на всякий

        TextView tvFront = convertView.findViewById(R.id.tvFront);
        TextView tvBack  = convertView.findViewById(R.id.tvBack);
        TextView tvScore = convertView.findViewById(R.id.tvScore);
        TextView tvGrade = convertView.findViewById(R.id.tvGrade);
        TextView tvRev   = convertView.findViewById(R.id.tvRev);
        CheckBox cbLearned = convertView.findViewById(R.id.checkbox_learned);

        // 🔹 Текст/поля
        tvFront.setText(w.front);
        tvBack.setText(w.back);
        tvScore.setText(String.valueOf(scoreFromEase(w.ease)));
        tvGrade.setText(w.lastGrade == null ? "—" : String.valueOf(w.lastGrade));
        tvRev.setText(String.valueOf(w.totalReviews));

        // 🔹 Снять слушатель, проставить состояние, снова повесить слушатель
        cbLearned.setOnCheckedChangeListener(null);
        cbLearned.setChecked(w.learned); // learned = boolean в модели


        cbLearned.setOnCheckedChangeListener((btn, checked) -> {
            try {
                AppDatabase.databaseExecutor
                        .submit(() -> db.runInTransaction(() ->
                                db.reviewDao().setLearnedTx(w.cardId, checked ? 1 : 0)
                        ))
                        .get(); // дожидаемся записи (гарантия сохранения до закрытия)
                w.learned = checked;
                ((Activity) getContext()).runOnUiThread(this::notifyDataSetChanged);
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) getContext()).runOnUiThread(() -> {
                    btn.setOnCheckedChangeListener(null);
                    btn.setChecked(!checked);
                    // верни листенер обратно так же, как ставишь его выше
                    Toast.makeText(getContext(), "Не удалось сохранить флаг", Toast.LENGTH_SHORT).show();
                });
            }
        });



        return convertView;
    }

    public void updateData(@Nullable List<WordWithStats> newData) {
        setNotifyOnChange(false);  // чтобы не дергать перерисовку на каждый add
        clear();                   // очистили текущие элементы
        if (newData != null && !newData.isEmpty()) {
            addAll(newData);       // добавили новые
        }
        notifyDataSetChanged();    // перерисовали список
    }



    private int scoreFromEase(Double ease) {
        if (ease == null) return 0;
        double min = 1.3, max = 3.0;
        double s = (ease - min) / (max - min);
        if (s < 0) s = 0;
        if (s > 1) s = 1;
        return (int) Math.round(s * 100);
    }
}
