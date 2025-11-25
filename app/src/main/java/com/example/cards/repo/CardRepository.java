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

/**
 * CardRepository
 *
 * Repository that provides a simple API for:
 * - Fetching cards that are due for review.
 * - Applying the SM-2 spaced repetition algorithm to schedule the next review.
 * - Logging each review event.
 *
 * This repository uses a single main Room database: "cards.db".
 * It is intended to be used in view models or other domain-level components.
 */
public class CardRepository {

    // Main Room database instance.
    private final AppDatabase db;

    /**
     * Creates a repository using the main "cards.db" database.
     *
     * @param context application or activity context used to build the Room database
     */
    public CardRepository(Context context) {
        db = Room.databaseBuilder(
                context,
                AppDatabase.class,
                "cards.db"
        ).build();
    }

    /**
     * Returns a list of cards that are due for review at the given time.
     *
     * @param now   current timestamp in milliseconds
     * @param limit maximum number of cards to return
     * @return list of {@link Card} that should be reviewed now (never null)
     */
    public List<Card> getDueCards(long now, int limit) {
        List<Card> due = db.reviewDao().dueCards(now, limit);
        return due != null ? due : Collections.emptyList();
    }

    /**
     * Handles user's answer for a specific card and schedules the next review
     * using the SM-2 algorithm.
     *
     * Steps:
     * 1. Load current {@link ReviewState} for the card.
     * 2. Build an SM-2 state and call {@link Sm2#review(Sm2.State, int)} with the grade.
     * 3. Update ReviewState fields (interval, ease, step, dueAt).
     * 4. Persist the updated state in the database.
     * 5. Insert a {@link ReviewLog} entry for analytics/history.
     *
     * @param cardId ID of the card being reviewed
     * @param grade  review grade selected by the user (SM-2 compatible value)
     * @param now    current timestamp in milliseconds
     */
    public void reviewAndSchedule(long cardId, int grade, long now) {
        // Get current review state for the card.
        ReviewState st = db.reviewDao().getState(cardId);

        // Map ReviewState to SM-2 state.
        Sm2.State s = new Sm2.State();
        s.intervalDays = st.intervalDays;
        s.ease = st.ease;
        s.step = st.step;

        // Apply SM-2 review logic.
        s = Sm2.review(s, grade);

        // Map SM-2 result back to ReviewState.
        st.intervalDays = s.intervalDays;
        st.ease = (float) s.ease;
        st.step = s.step;
        st.dueAt = now + (s.intervalDays * 24L * 60L * 60L * 1000L); // days → ms

        // Save updated review state.
        db.reviewDao().upsertStateEntity(st);

        // Write a review log entry.
        ReviewLog log = new ReviewLog();
        log.cardId = cardId;
        log.ts = now;
        log.grade = grade;
        db.reviewDao().insertLog(log);
    }
}
