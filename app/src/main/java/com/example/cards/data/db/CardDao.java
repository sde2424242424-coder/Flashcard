package com.example.cards.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import androidx.room.Transaction;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.WordWithStats;

import java.util.List;

/**
 * CardDao
 *
 * Data access object for operations on the {@code cards} table and related
 * projections (statistics, learned state, selections).
 *
 * Responsibilities:
 * - Count cards and learned cards per deck.
 * - Read pages of cards for a deck.
 * - Provide search and statistics queries returning {@link WordWithStats}.
 * - Manage "excluded" and "learned" flags in {@code cards} and {@code learned_state}.
 * - Provide learned percent both as int and as LiveData.
 */
@Dao
public interface CardDao {

    // -------------------------------------------------------------------------
    // COUNTS / PROGRESS
    // -------------------------------------------------------------------------

    /**
     * Returns how many non-excluded cards a deck has.
     *
     * @param deckId deck identifier
     */
    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND COALESCE(excluded,0) = 0")
    int totalInDeck(long deckId);

    /**
     * Returns count of learned cards in a deck, prioritizing learned_state.learned
     * (user overrides). If no entry in {@code learned_state}, falls back to
     * {@code cards.learned}.
     *
     * Excluded cards are not counted.
     */
    @Query(
            "SELECT COUNT(*) " +
                    "FROM cards c " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "WHERE c.deckId = :deckId " +
                    "  AND COALESCE(c.excluded,0) = 0 " +
                    "  AND COALESCE(ls.learned, c.learned, 0) = 1"
    )
    int learnedInDeck(long deckId);

    /**
     * Observes learned percent (0..100) as LiveData for a given deck.
     * Only non-excluded cards are included.
     *
     * Learned state is taken from learned_state if present, otherwise from cards.learned.
     */
    @Query(
            "SELECT CAST( " +
                    "  100.0 * " +
                    "  SUM(CASE WHEN COALESCE(ls.learned, c.learned, 0) = 1 THEN 1 ELSE 0 END) " +
                    "  / NULLIF(COUNT(c.id), 0) " +
                    "AS INT) " +
                    "FROM cards c " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "WHERE c.deckId = :deckId " +
                    "  AND COALESCE(c.excluded,0) = 0"
    )
    LiveData<Integer> observeLearnedPercent(long deckId);

    /**
     * Returns total count of cards for a deck (excluded or not).
     */
    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    int countByDeck(long deckId);

    // -------------------------------------------------------------------------
    // PAGING
    // -------------------------------------------------------------------------

    /**
     * Returns a page of cards for a given deck, ordered by id.
     *
     * @param deckId deck identifier
     * @param limit  page size
     * @param offset offset into the result
     */
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id LIMIT :limit OFFSET :offset")
    List<Card> getPageByDeck(long deckId, int limit, int offset);

    // -------------------------------------------------------------------------
    // SEARCH / STAT LISTS
    // -------------------------------------------------------------------------

    /**
     * Simple search by front/back text for a given deck.
     * Returns {@link WordWithStats} combining card info with basic review stats.
     */
    @Query(
            "SELECT " +
                    "    c.id    AS cardId, " +
                    "    c.front AS front, " +
                    "    c.back  AS back, " +
                    "    c.learned AS learned, " +
                    "    rs.ease       AS ease, " +
                    "    rs.lastGrade  AS lastGrade, " +
                    "    COALESCE((SELECT COUNT(*) FROM review_log rl WHERE rl.cardId = c.id), 0) AS totalReviews " +
                    "FROM cards c " +
                    "LEFT JOIN review_state rs ON rs.cardId = c.id " +
                    "WHERE c.deckId = :deckId " +
                    "  AND (c.front LIKE '%' || :q || '%' OR c.back LIKE '%' || :q || '%') " +
                    "ORDER BY c.id ASC"
    )
    List<WordWithStats> searchWords(long deckId, String q);

    /**
     * Returns all words in a deck with stats:
     * - learned flag
     * - SM-2 ease and lastGrade
     * - total number of review_log entries
     */
    @Query(
            "SELECT " +
                    "  c.id            AS cardId, " +
                    "  c.front         AS front, " +
                    "  c.back          AS back, " +
                    "  c.learned       AS learned, " +
                    "  rs.ease         AS ease, " +
                    "  rs.lastGrade    AS lastGrade, " +
                    "  COALESCE(COUNT(l.id),0) AS totalReviews " +
                    "FROM cards c " +
                    "LEFT JOIN review_state  rs ON rs.cardId = c.id " +
                    "LEFT JOIN review_log    l  ON l.cardId = c.id " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "WHERE c.deckId = :deckId " +
                    "GROUP BY c.id " +
                    "ORDER BY c.id ASC"
    )
    List<WordWithStats> getWordsWithStats(long deckId);

    /**
     * Total number of cards (across all decks).
     */
    @Query("SELECT COUNT(*) FROM cards")
    int countAll();

    /**
     * Returns a global page of cards ordered by id.
     */
    @Query("SELECT * FROM cards ORDER BY id LIMIT :limit OFFSET :offset")
    List<Card> getPage(int limit, int offset);

    /**
     * Returns a selection of not-yet-learned cards for a deck
     * with stats, ordered by earliest dueAt first.
     *
     * Uses @RewriteQueriesToDropUnusedColumns to avoid fetching unused data.
     */
    @RewriteQueriesToDropUnusedColumns
    @Query(
            "SELECT " +
                    "    c.id AS cardId, " +
                    "    c.deckId AS deckId, " +
                    "    c.front AS front, " +
                    "    c.back  AS back, " +
                    "    COALESCE(rs.ease, 0) AS ease, " +
                    "    rs.lastGrade AS lastGrade, " +
                    "    (SELECT COUNT(*) FROM review_log rl WHERE rl.cardId = c.id) AS totalReviews, " +
                    "    COALESCE(ls.learned, 0) AS learned " +
                    "FROM cards c " +
                    "LEFT JOIN review_state rs ON rs.cardId = c.id " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "WHERE c.deckId = :deckId " +
                    "  AND COALESCE(c.excluded, 0) = 0 " +
                    "  AND COALESCE(ls.learned, 0) = 0 " +   // Filter already learned
                    "ORDER BY rs.dueAt ASC, c.id ASC " +
                    "LIMIT :limit"
    )
    List<WordWithStats> getSelection(long deckId, int limit);

    // -------------------------------------------------------------------------
    // EXCLUDED FLAG
    // -------------------------------------------------------------------------

    /**
     * Updates the "excluded" flag for a card.
     *
     * @param cardId   card identifier
     * @param excluded true -> excluded, false -> not excluded
     */
    @Query("UPDATE cards SET excluded = CASE WHEN :excluded THEN 1 ELSE 0 END WHERE id=:cardId")
    void setExcluded(long cardId, boolean excluded);

    // -------------------------------------------------------------------------
    // LEARNED_STATE UPSERT
    // -------------------------------------------------------------------------

    /**
     * Updates learned_state.learned for a card.
     *
     * @return number of rows affected
     */
    @Query("UPDATE learned_state SET learned=:learned WHERE cardId=:cardId")
    int updateLearned(long cardId, int learned);

    /**
     * Inserts a row into learned_state if it does not exist.
     */
    @Query("INSERT OR IGNORE INTO learned_state(cardId, learned) VALUES(:cardId, :learned)")
    void insertLearnedIfMissing(long cardId, int learned);

    /**
     * Upserts learned_state: update if exists, otherwise insert.
     */
    @Transaction
    default void upsertLearned(long cardId, boolean isLearned) {
        int n = updateLearned(cardId, isLearned ? 1 : 0);
        if (n == 0) {
            insertLearnedIfMissing(cardId, isLearned ? 1 : 0);
        }
    }

    // -------------------------------------------------------------------------
    // MISC / LISTS
    // -------------------------------------------------------------------------

    /**
     * Returns all cards without filtering.
     */
    @Query("SELECT * FROM cards")
    List<Card> getAll();

    /**
     * Returns stats for all cards in the DB (all decks).
     */
    @Query(
            "SELECT " +
                    "    c.id AS cardId, " +
                    "    c.front AS front, " +
                    "    c.back  AS back, " +
                    "    COALESCE(rs.ease, 0)      AS ease, " +
                    "    rs.lastGrade              AS lastGrade, " +
                    "    (SELECT COUNT(*) FROM review_log rl WHERE rl.cardId = c.id) AS totalReviews, " +
                    "    COALESCE(ls.learned, 0)   AS learned " +
                    "FROM cards c " +
                    "LEFT JOIN review_state rs ON rs.cardId = c.id " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "ORDER BY rs.dueAt ASC, c.id ASC"
    )
    List<WordWithStats> getWordsWithStatsAll();

    /**
     * Returns unlearned, non-excluded words for a deck with stats.
     */
    @Query(
            "SELECT " +
                    "  c.id                         AS cardId, " +
                    "  c.front                      AS front, " +
                    "  c.back                       AS back, " +
                    "  COALESCE(ls.learned, 0)      AS learned, " +
                    "  rs.ease                      AS ease, " +
                    "  rs.lastGrade                 AS lastGrade, " +
                    "  (SELECT COUNT(*) FROM review_log rl WHERE rl.cardId = c.id) AS totalReviews " +
                    "FROM cards c " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "LEFT JOIN review_state  rs ON rs.cardId = c.id " +
                    "WHERE COALESCE(ls.learned, 0) = 0 " +
                    "  AND c.excluded = 0 " +
                    "  AND c.deckId = :deckId " +
                    "ORDER BY c.id"
    )
    List<WordWithStats> getUnlearnedWords(long deckId);

    /**
     * Returns words with learned info and count of reviews for a deck.
     * Uses a pre-aggregated subquery for review_log count.
     */
    @Query(
            "SELECT " +
                    "  c.id                                  AS cardId, " +
                    "  c.front                               AS front, " +
                    "  c.back                                AS back, " +
                    "  COALESCE(ls.learned, 0)               AS learned, " +
                    "  rs.ease                               AS ease, " +
                    "  rs.lastGrade                          AS lastGrade, " +
                    "  COALESCE(rl.cnt, 0)                   AS totalReviews " +
                    "FROM cards c " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "LEFT JOIN review_state  rs ON rs.cardId = c.id " +
                    "LEFT JOIN ( " +
                    "    SELECT cardId, COUNT(*) AS cnt " +
                    "    FROM review_log " +
                    "    GROUP BY cardId " +
                    ") rl ON rl.cardId = c.id " +
                    "WHERE c.deckId = :deckId"
    )
    List<WordWithStats> getWordsWithLearned(long deckId);

    // -------------------------------------------------------------------------
    // LEARNED PERCENT (INT 0..100)
    // -------------------------------------------------------------------------

    /**
     * Returns learned percent (0..100) for a deck as an int.
     * Only non-excluded cards are counted.
     */
    @Query(
            "SELECT CAST( " +
                    "  100.0 * " +
                    "  SUM(CASE WHEN COALESCE(ls.learned, c.learned, 0) = 1 THEN 1 ELSE 0 END) " +
                    "  / NULLIF(COUNT(c.id), 0) " +
                    "AS INT) " +
                    "FROM cards c " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "WHERE c.deckId = :deckId " +
                    "  AND COALESCE(c.excluded, 0) = 0"
    )
    int learnedPercent(long deckId);

    // -------------------------------------------------------------------------
    // SET LEARNED IN BOTH TABLES
    // -------------------------------------------------------------------------

    /**
     * Updates "learned" simultaneously in:
     * - cards.learned
     * - learned_state.learned (via upsert).
     */
    @Transaction
    default void setLearnedBoth(long cardId, boolean isLearned) {
        int v = isLearned ? 1 : 0;
        setLearned(cardId, v);            // mirror in cards.learned
        upsertLearned(cardId, isLearned); // and in learned_state
    }

    /**
     * Low-level update of cards.learned only.
     */
    @Query("UPDATE cards SET learned = :learned WHERE id = :cardId")
    void setLearned(long cardId, int learned);
}
