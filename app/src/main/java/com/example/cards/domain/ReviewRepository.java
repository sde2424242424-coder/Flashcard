package com.example.cards.domain;

import androidx.annotation.WorkerThread;

import com.example.cards.data.db.CardDao;
import com.example.cards.data.db.ReviewDao;
import com.example.cards.data.model.Card;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;
import com.example.cards.domain.srs.Sm2;

import java.util.List;

/**
 * ReviewRepository
 *
 * Repository that encapsulates all operations related to spaced repetition
 * review logic:
 * - Loading due cards (optionally by deck).
 * - Reading and updating review state for a card.
 * - Applying the SM-2 algorithm and scheduling the next review time.
 * - Saving review logs for analytics/history.
 *
 * This repository works on top of {@link ReviewDao} and optionally uses
 * {@link CardDao} for card-specific flags (e.g. excluded).
 */
public class ReviewRepository {

    // DAO that provides access to review-related tables (state, logs, due queries).
    private final ReviewDao reviewDao;

    /**
     * Constructs a ReviewRepository using the given ReviewDao.
     *
     * @param reviewDao DAO for review state and logs
     */
    public ReviewRepository(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    /**
     * Returns due cards without explicitly filtering by deckId
     * (legacy helper, uses dueCards(now, limit)).
     *
     * @param deckId logical deck ID (currently not used in this method)
     * @param now    current time in milliseconds
     * @param limit  maximum number of cards to return
     * @return list of due cards
     */
    public List<Card> getDue(long deckId, long now, int limit) {
        // Previously: reviewDao.getDueCards(deckId, now, limit);
        return reviewDao.dueCards(now, limit);
    }

    /**
     * Sets or clears the "excluded" flag on a card.
     * Uses {@link CardDao} because the flag is stored at card level.
     *
     * @param cardId   ID of the card
     * @param excluded true if the card should be excluded from review
     * @param cardDao  DAO that can update the card's excluded flag
     */
    public void setExcluded(long cardId, boolean excluded, CardDao cardDao) {
        cardDao.setExcluded(cardId, excluded);
    }

    /**
     * Returns due cards for a specific deck.
     *
     * @param deckId target deck ID
     * @param now    current time in milliseconds
     * @param limit  maximum number of cards to return
     * @return list of due cards for the deck
     */
    public List<Card> getDueCards(long deckId, long now, int limit) {
        return reviewDao.getDueCards(deckId, now, limit);
    }

    /**
     * Returns due cards ignoring deckId (legacy variant).
     *
     * @param now   current time in milliseconds
     * @param limit maximum number of cards to return
     * @return list of due cards
     */
    public List<Card> getDue(long now, int limit) {
        return reviewDao.dueCards(now, limit);
    }

    /**
     * Returns current review state for the given card.
     *
     * @param cardId ID of the card
     * @return {@link ReviewState} or null if not yet created
     */
    public ReviewState getState(long cardId) {
        return reviewDao.getState(cardId);
    }

    /**
     * Applies the review result (grade) for the card, updates the SM-2 state,
     * calculates the next due time, and stores both the updated state and
     * a review log entry in a single transaction.
     *
     * This method is intended to be called on a background thread.
     *
     * Steps:
     * 1. Load current ReviewState; if absent, create a default one.
     * 2. Map ReviewState to {@link Sm2.State}.
     * 3. Call {@link Sm2#review(Sm2.State, int)} with the provided grade.
     * 4. Map the result back to ReviewState (interval, ease, step, dueAt).
     * 5. Create a {@link ReviewLog} with the result.
     * 6. Save state and log atomically via {@link ReviewDao#saveStateAndLog(ReviewState, ReviewLog)}.
     *
     * @param cardId    ID of the reviewed card
     * @param grade     grade given by the user (SM-2 compatible)
     * @param nowMillis current time in milliseconds
     * @return updated ReviewState
     */
    @WorkerThread
    public ReviewState reviewAndSchedule(long cardId, int grade, long nowMillis) {
        // 1) Current state (can be null on first review).
        ReviewState st = reviewDao.getState(cardId);

        // 2) If state does not exist yet, create a default one.
        if (st == null) {
            st = new ReviewState();
            st.cardId = cardId;
            st.intervalDays = 0;
            st.ease = 2.5f;     // initial ease
            st.step = 0;
            st.dueAt = nowMillis;
        }

        // 3) Map ReviewState to SM-2 state.
        Sm2.State sm2 = new Sm2.State();
        sm2.intervalDays = st.intervalDays;
        sm2.ease = st.ease;     // if Sm2.State.ease is double, cast types accordingly
        sm2.step = st.step;

        // 4) Apply SM-2 review algorithm.
        sm2 = Sm2.review(sm2, grade);

        // 5) Update entity with SM-2 result.
        st.intervalDays = sm2.intervalDays;
        st.ease = (float) sm2.ease;
        st.step = sm2.step;
        st.lastGrade = grade;
        st.dueAt = Sm2.nextDueAtFromNowMillis(st.intervalDays, nowMillis);

        // 6) Prepare review log entry.
        ReviewLog log = new ReviewLog();
        log.cardId = cardId;
        log.reviewedAt = nowMillis;
        log.grade = grade;
        log.resultIntervalDays = st.intervalDays;
        log.resultEase = st.ease;
        log.resultStep = st.step;

        // 7) Save state and log atomically in one transaction.
        reviewDao.saveStateAndLog(st, log);

        return st;
    }
}
