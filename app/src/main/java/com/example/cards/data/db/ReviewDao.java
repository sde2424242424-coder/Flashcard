package com.example.cards.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;

import java.util.List;

/**
 * ReviewDao
 *
 * Data access object for all review-related operations:
 * - Querying due cards (with or without deck filter).
 * - Seeding initial review_state rows for cards that do not have them yet.
 * - Counting states, due items, excluded and learned cards.
 * - Updating "learned" flag.
 * - Saving review state and review log in a single transaction.
 */
@Dao
public interface ReviewDao {

    /**
     * Returns the current review state for the specified card, if it exists.
     *
     * @param cardId card identifier
     * @return {@link ReviewState} or null if no state exists yet
     */
    @Query("SELECT * FROM review_state WHERE cardId = :cardId LIMIT 1")
    ReviewState getState(long cardId);

    /**
     * Returns cards that are due for review at the given time, across all decks.
     * Uses review_state.dueAt and respects the card "excluded" flag.
     *
     * @param now   current time in milliseconds
     * @param limit maximum number of cards to return
     * @return list of due {@link Card} items
     */
    @Query("""
           SELECT c.* FROM cards c
           JOIN review_state rs ON rs.cardId = c.id
           WHERE rs.dueAt <= :now AND c.excluded = 0
           ORDER BY rs.dueAt ASC
           LIMIT :limit
           """)
    List<Card> dueCards(long now, int limit);

    /**
     * Returns cards that are due for review at the given time within a specific deck.
     *
     * @param deckId deck identifier
     * @param now    current time in milliseconds
     * @param limit  maximum number of cards to return
     * @return list of due {@link Card} items for the given deck
     */
    @Query("""
       SELECT c.* FROM cards c
       JOIN review_state rs ON rs.cardId = c.id
       WHERE c.deckId = :deckId AND rs.dueAt <= :now AND c.excluded = 0
       ORDER BY RANDOM()              -- ← вместо ORDER BY rs.dueAt ASC
       LIMIT :limit
       """)
    List<Card> getDueCards(long deckId, long now, int limit);


    // ---------- COUNTS ----------

    /**
     * Counts how many review_state rows exist for the given deck.
     *
     * @param deckId deck identifier
     * @return number of states for cards in that deck
     */
    @Query("SELECT COUNT(*) FROM review_state rs JOIN cards c ON c.id = rs.cardId WHERE c.deckId = :deckId")
    int countStates(long deckId);

    /**
     * Seeds review_state rows for all cards in the given deck that do not yet
     * have a state. Newly created states will have:
     * - intervalDays = 0
     * - ease         = 2.5
     * - step         = 0
     * - dueAt        = :now
     *
     * @param deckId deck identifier
     * @param now    current time in milliseconds
     */
    @Query("INSERT INTO review_state(cardId, intervalDays, ease, step, dueAt) " +
            "SELECT c.id, 0, 2.5, 0, :now FROM cards c " +
            "LEFT JOIN review_state rs ON rs.cardId = c.id " +
            "WHERE c.deckId = :deckId AND rs.cardId IS NULL")
    void seedReviewState(long deckId, long now);

    /**
     * Counts how many cards in the deck are marked as excluded.
     *
     * @param deckId deck identifier
     * @return number of excluded cards
     */
    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND excluded = 1")
    int countExcluded(long deckId);

    /**
     * Counts how many cards in the deck are marked as learned.
     *
     * @param deckId deck identifier
     * @return number of learned cards
     */
    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND learned = 1")
    int countLearnedCards(long deckId);

    /**
     * Counts how many cards are due for review in the given deck at the given time.
     *
     * @param deckId deck identifier
     * @param now    current time in milliseconds
     * @return number of due cards
     */
    @Query("""
           SELECT COUNT(*) FROM cards c
           JOIN review_state rs ON rs.cardId = c.id
           WHERE c.deckId = :deckId AND rs.dueAt <= :now AND c.excluded = 0
           """)
    int countDue(long deckId, long now);

    // ---------- LEARNED ----------

    /**
     * Updates the "learned" flag for a single card.
     *
     * @param cardId  card identifier
     * @param learned 1 if learned, 0 otherwise
     * @return number of rows affected
     */
    @Query("UPDATE cards SET learned = :learned WHERE id = :cardId")
    int updateCardLearned(long cardId, int learned);

    /**
     * Transactional helper to update the "learned" flag.
     * If a separate learned_state table is used, its synchronization
     * can be added here.
     *
     * @param cardId  card identifier
     * @param learned 1 if learned, 0 otherwise
     */
    @Transaction
    default void setLearnedTx(long cardId, int learned) {
        updateCardLearned(cardId, learned);
        // If you have a learned_state table and need to sync it,
        // add the corresponding logic here.
    }

    // ---------- STATE & LOG ----------

    /**
     * Inserts or updates a ReviewState entity.
     *
     * @param state review state to upsert
     * @return row ID of the inserted/updated state
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsertStateEntity(ReviewState state);

    /**
     * Inserts a single ReviewLog entry.
     *
     * @param log review log to insert
     * @return row ID of the inserted log
     */
    @Insert
    long insertLog(ReviewLog log);

    /**
     * Saves review state and review log atomically in a single transaction.
     *
     * @param state updated review state
     * @param log   review log entry to store
     */
    @Transaction
    default void saveStateAndLog(ReviewState state, ReviewLog log) {
        upsertStateEntity(state);
        insertLog(log);
    }
}
