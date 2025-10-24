package com.example.cards.domain;

import androidx.annotation.WorkerThread;

import com.example.cards.data.db.CardDao;
import com.example.cards.data.db.ReviewDao;
import com.example.cards.data.model.Card;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;
import com.example.cards.domain.srs.Sm2;

import java.util.List;

public class ReviewRepository {
    private final ReviewDao reviewDao;

    public ReviewRepository(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    // ReviewRepository.java
    public List<Card> getDue(long deckId, long now, int limit) {
        // было: reviewDao.getDueCards(deckId, now, limit);
        return reviewDao.dueCards(now, limit);
    }

    public void setExcluded(long cardId, boolean excluded, CardDao cardDao) {
        cardDao.setExcluded(cardId, excluded);
    }

    public List<Card> getDueCards(long deckId, long now, int limit) {
        return reviewDao.getDueCards(deckId, now, limit);
    }

    // Если где-то ещё используешь вариант без deckId:
    public List<Card> getDue(long now, int limit) {
        return reviewDao.dueCards(now, limit);
    }

    public ReviewState getState(long cardId) {
        return reviewDao.getState(cardId);
    }

    @WorkerThread
    public ReviewState reviewAndSchedule(long cardId, int grade, long nowMillis) {
        // 1) Текущее состояние (может быть null)
        ReviewState st = reviewDao.getState(cardId);

        // 2) Если нет — создаём с дефолтами
        if (st == null) {
            st = new ReviewState();
            st.cardId = cardId;
            st.intervalDays = 0;
            st.ease = 2.5f;     // стартовый ease
            st.step = 0;
            st.dueAt = nowMillis;
        }

        // 3) Преобразуем в состояние SM2
        Sm2.State sm2 = new Sm2.State();
        sm2.intervalDays = st.intervalDays;
        sm2.ease = st.ease;    // если в Sm2.State ease = double, то: (float)st.ease или поменять типы на double
        sm2.step = st.step;

        // 4) Применяем алгоритм
        sm2 = Sm2.review(sm2, grade);

        // 5) Обновляем entity
        st.intervalDays = sm2.intervalDays;
        st.ease = (float) sm2.ease;    // если Sm2.State.ease = double → (float) sm2.ease
        st.step = sm2.step;
        st.lastGrade = grade;
        st.dueAt = Sm2.nextDueAtFromNowMillis(st.intervalDays, nowMillis);

        // 6) Готовим лог
        ReviewLog log = new ReviewLog();
        log.cardId = cardId;
        log.reviewedAt = nowMillis;
        log.grade = grade;
        log.resultIntervalDays = st.intervalDays;
        log.resultEase = st.ease;
        log.resultStep = st.step;

        // 7) Сохраняем атомарно (в одной транзакции)
        reviewDao.saveStateAndLog(st, log);

        return st;
    }

}
