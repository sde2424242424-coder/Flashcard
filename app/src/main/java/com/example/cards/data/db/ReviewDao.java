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

@Dao
public interface ReviewDao {

    @Query("SELECT * FROM review_state WHERE cardId = :cardId LIMIT 1")
    ReviewState getState(long cardId);

    // если ещё не добавил раньше — оставь оба варианта due:
    @Query("""
           SELECT c.* FROM cards c
           JOIN review_state rs ON rs.cardId = c.id
           WHERE rs.dueAt <= :now AND c.excluded = 0
           ORDER BY rs.dueAt ASC
           LIMIT :limit
           """)
    List<Card> dueCards(long now, int limit);

    @Query("""
           SELECT c.* FROM cards c
           JOIN review_state rs ON rs.cardId = c.id
           WHERE c.deckId = :deckId AND rs.dueAt <= :now AND c.excluded = 0
           ORDER BY rs.dueAt ASC
           LIMIT :limit
           """)
    List<Card> getDueCards(long deckId, long now, int limit);

    // ---------- DUE списки ----------
    // Вариант без deckId (если БД на колоду отдельная)


    // Вариант с deckId (если одна общая БД на все колоды)


    // ---------- COUNTS ----------
    @Query("SELECT COUNT(*) FROM review_state rs JOIN cards c ON c.id = rs.cardId WHERE c.deckId = :deckId")
    int countStates(long deckId);

    @Query("INSERT INTO review_state(cardId, intervalDays, ease, step, dueAt) " +
            "SELECT c.id, 0, 2.5, 0, :now FROM cards c " +
            "LEFT JOIN review_state rs ON rs.cardId = c.id " +
            "WHERE c.deckId = :deckId AND rs.cardId IS NULL")
    void seedReviewState(long deckId, long now);

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND excluded = 1")
    int countExcluded(long deckId);

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND learned = 1")
    int countLearnedCards(long deckId);

    @Query("""
           SELECT COUNT(*) FROM cards c
           JOIN review_state rs ON rs.cardId = c.id
           WHERE c.deckId = :deckId AND rs.dueAt <= :now AND c.excluded = 0
           """)
    int countDue(long deckId, long now);

    // ---------- LEARNED ----------
    @Query("UPDATE cards SET learned = :learned WHERE id = :cardId")
    int updateCardLearned(long cardId, int learned);

    @Transaction
    default void setLearnedTx(long cardId, int learned) {
        updateCardLearned(cardId, learned);
        // Если у тебя есть таблица learned_state и её надо синхронить — добавь здесь логику.
    }

    // ---------- STATE & LOG ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsertStateEntity(ReviewState state);

    @Insert
    long insertLog(ReviewLog log);

    @Transaction
    default void saveStateAndLog(ReviewState state, ReviewLog log) {
        upsertStateEntity(state);
        insertLog(log);
    }
}
