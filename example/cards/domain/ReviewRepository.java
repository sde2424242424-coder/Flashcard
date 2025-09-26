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
    public List<com.example.cards.data.model.card> getDueCards(long now, int limit) {
        return reviewDao.dueCards(now, limit);
    }
    @WorkerThread
    public ReviewState reviewAndSchedule(long cardId, int grade, long nowMillis) {
        ReviewState st = reviewDao.getState(cardId);
        if (st == null) {
            st = new ReviewState();
            st.cardId = cardId;
            st.intervalDays = 0;
            st.ease = 2.5;
            st.step = 0;
            st.dueAt = nowMillis;
        }
        double prevEase = st.ease;
        int prevInterval = st.intervalDays;
        int prevStep = st.step;
        Sm2.review(st,grade);
        st.dueAt = Sm2.nextDueAtFromNowMillis(st.intervalDays, nowMillis);
        reviewDao.upsertState(st);
        ReviewLog log = new ReviewLog();
        log.cardId = cardId;
        log.reviewedAt = nowMillis;
        log.grade = grade;
        reviewDao.insertLog(log);
        return st;
    }
}

Sm2.State s = new Sm2.State();
s.intervalDays = st.intervalDays;
s.ease = st.ease;
s.step = st.step;

s = Sm2.review(s,grade);

st.intervalDays = s.intervalDays;
st.ease = s.ease;
st.step = s.step;
st.lastGrade = grade;
st.dueAt = Sm2.nextDueAtFromNowMillis(st.intervalDays, nowMillis);

reviewDao.upsertState(st);

ReviewLog log = new ReviewLog();
log.cardId = cardId;
log.reviewedAt = nowMillis;
log.grade = grade;
log.resultIntervalDays = st.intervalDays;
log.resultEase = st.ease;
log.resultStep = st.step;
reviewDao.insertLog(log);

return st; 