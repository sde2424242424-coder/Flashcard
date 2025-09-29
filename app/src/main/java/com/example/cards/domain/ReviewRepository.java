package com.example.cards.domain;

import androidx.annotation.WorkerThread;

import com.example.cards.data.db.ReviewDao;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;
import com.example.cards.domain.srs.Sm2;

import java.util.List;

public class ReviewRepository {
    private final ReviewDao reviewDao;

    public ReviewRepository(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    @WorkerThread
    public List<com.example.cards.data.model.Card> getDueCards(long now, int limit) {
        return reviewDao.dueCards(now, limit);
    }

    @WorkerThread
    public ReviewState reviewAndSchedule(long cardId, int grade, long nowMillis) {
        ReviewState st = reviewDao.getState(cardId);

        if (st == null) {
            st = new ReviewState();
            st.cardId = cardId;
            st.intervalDays = 0;
            st.ease = 2.5;   // int вместо 2.5
            st.step = 0;
            st.dueAt = nowMillis;
        }

        // Создать состояние SM2 из текущего
        Sm2.State s = new Sm2.State();
        s.intervalDays = st.intervalDays;
        s.ease = st.ease;
        s.step = st.step;

        // Применить алгоритм SM2
        s = Sm2.review(s, grade);

        // Обновить состояние карточки
        st.intervalDays = s.intervalDays;
        st.ease = s.ease;
        st.step = s.step;
        st.lastGrade = grade;
        st.dueAt = Sm2.nextDueAtFromNowMillis(st.intervalDays, nowMillis);

        // Сохранить новое состояние
        reviewDao.upsertState(st);

        // Добавить запись в лог
        ReviewLog log = new ReviewLog();
        log.cardId = cardId;
        log.reviewedAt = nowMillis;
        log.grade = grade;
        log.resultIntervalDays = st.intervalDays;
        log.resultEase = st.ease;
        log.resultStep = st.step;
        reviewDao.insertLog(log);

        return st;
    }
}
