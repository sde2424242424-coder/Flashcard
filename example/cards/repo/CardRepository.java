package com.example.cards.repo;

import android.content.Context;

import androidx.room.Room;

import com.example.cards.data.db.AppDatabase;
import com.example.cards.data.model.Card;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;
import com.example.cards.domain.srs.Sm2;

import java.util.Collections;
import java.util.List;

public class CardRepository {
    private final AppDatabase db;

    public CardRepository(Context context) {
        db = Room.databaseBuilder(context,
                AppDatabase.class, "cards.db").build();
    }

    // Выбрать карточки «к сроку»
    public List<Card> getDueCards(long now, int limit) {
        List<Card> due = db.reviewDao().dueCards(now, limit);
        return due != null ? due : Collections.emptyList();
    }

    // Пользователь ответил
    public void reviewAndSchedule(long cardId, int grade, long now) {
        ReviewState st = db.reviewDao().getState(cardId);

        Sm2.State s = new Sm2.State();
        s.intervalDays = st.intervalDays;
        s.ease = st.ease;
        s.step = st.step;

        s = Sm2.review(s, grade);

        st.intervalDays = s.intervalDays;
        st.ease = s.ease;
        st.step = s.step;
        st.dueAt = now + (s.intervalDays * 24L * 60L * 60L * 1000L);

        db.reviewDao().upsertState(st);

        ReviewLog log = new ReviewLog();
        log.cardId = cardId;
        log.ts = now;
        log.grade = grade;
        db.reviewDao().insertLog(log);
    }
}
