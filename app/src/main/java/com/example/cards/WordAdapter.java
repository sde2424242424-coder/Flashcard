package com.example.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;
import com.example.cards.data.model.WordWithStats;
import java.util.List;

public class WordAdapter extends ArrayAdapter<WordWithStats> {
    public WordAdapter(@NonNull Context context, @NonNull List<WordWithStats> words) {
        super(context, 0, words);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_word, parent, false);
        }

        WordWithStats w = getItem(position);

        TextView tvFront = convertView.findViewById(R.id.tvFront);
        TextView tvBack = convertView.findViewById(R.id.tvBack);
        TextView tvScore = convertView.findViewById(R.id.tvScore);
        TextView tvGrade = convertView.findViewById(R.id.tvGrade);
        TextView tvRev = convertView.findViewById(R.id.tvRev);

        tvFront.setText(w.front);
        tvBack.setText(w.back);
        tvScore.setText(String.valueOf(scoreFromEase(w.ease)));
        tvGrade.setText(w.lastGrade == null ? "—" : String.valueOf(w.lastGrade));
        tvRev.setText(String.valueOf(w.totalReviews));

        return convertView;
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
