package com.example.cards.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import androidx.room.Transaction;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.WordWithStats;

import java.util.List;

@Dao
public interface CardDao {

    // Сколько всего карточек (не исключённых) в колоде
    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND COALESCE(excluded,0) = 0")
    int totalInDeck(long deckId);

    // Сколько выученных (берём learned_state приоритетно, иначе fallback на cards.learned)
    @Query(
            "SELECT COUNT(*) " +
                    "FROM cards c " +
                    "LEFT JOIN learned_state ls ON ls.cardId = c.id " +
                    "WHERE c.deckId = :deckId " +
                    "  AND COALESCE(c.excluded,0) = 0 " +
                    "  AND COALESCE(ls.learned, c.learned, 0) = 1"
    )
    int learnedInDeck(long deckId);

    // Наблюдаемая версия процента (если пользуешься LiveData)
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

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    int countByDeck(long deckId);

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id LIMIT :limit OFFSET :offset")
    List<Card> getPageByDeck(long deckId, int limit, int offset);

    // Поиск
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

    // Статистика по словам — фильтруем join’ы по deckId
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

    @Query("SELECT COUNT(*) FROM cards")
    int countAll();

    @Query("SELECT * FROM cards ORDER BY id LIMIT :limit OFFSET :offset")
    List<Card> getPage(int limit, int offset);

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
                    "  AND COALESCE(ls.learned, 0) = 0 " +   // Отсекаем выученные
                    "ORDER BY rs.dueAt ASC, c.id ASC " +
                    "LIMIT :limit"
    )
    List<WordWithStats> getSelection(long deckId, int limit);

    @Query("UPDATE cards SET excluded = CASE WHEN :excluded THEN 1 ELSE 0 END WHERE id=:cardId")
    void setExcluded(long cardId, boolean excluded);

    // --- upsert для learned_state ---
    @Query("UPDATE learned_state SET learned=:learned WHERE cardId=:cardId")
    int updateLearned(long cardId, int learned);

    @Query("INSERT OR IGNORE INTO learned_state(cardId, learned) VALUES(:cardId, :learned)")
    void insertLearnedIfMissing(long cardId, int learned);

    @Transaction
    default void upsertLearned(long cardId, boolean isLearned) {
        int n = updateLearned(cardId, isLearned ? 1 : 0);
        if (n == 0) insertLearnedIfMissing(cardId, isLearned ? 1 : 0);
    }

    @Query("SELECT * FROM cards")
    List<Card> getAll();

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

    // === ПРОЦЕНТ ВЫУЧЕННЫХ (int 0..100) ===
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

    // === Обновление "выучено" сразу в обеих таблицах ===
    @Transaction
    default void setLearnedBoth(long cardId, boolean isLearned) {
        int v = isLearned ? 1 : 0;
        setLearned(cardId, v);           // зеркалим в cards.learned (если поле используешь)
        upsertLearned(cardId, isLearned); // и в learned_state
    }

    @Query("UPDATE cards SET learned = :learned WHERE id = :cardId")
    void setLearned(long cardId, int learned);
}
