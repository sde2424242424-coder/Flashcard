package com.example.cards.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Delete;


import com.example.cards.data.model.Card;
import com.example.cards.data.model.LearnedState;
import com.example.cards.data.model.WordWithSchedule;
import com.example.cards.data.model.WordWithStats;

import java.util.List;

@Dao
public interface CardDao {

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    int countByDeck(long deckId);

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id LIMIT :limit OFFSET :offset")
    List<Card> getPageByDeck(long deckId, int limit, int offset);

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND learned = 1")
    int countLearnedCards(long deckId);

    @Query("SELECT COUNT(*) FROM learned_state ls JOIN cards c ON c.id = ls.cardId WHERE c.deckId = :deckId AND ls.learned = 1")
    int countLearnedState(long deckId);

    // Поиск
    @Query("""
SELECT 
    c.id   AS cardId,
    c.front AS front,
    c.back  AS back,
    c.learned AS learned,
    rs.ease       AS ease,
    rs.lastGrade  AS lastGrade,
    COALESCE((SELECT COUNT(*) FROM review_log rl WHERE rl.cardId = c.id), 0) AS totalReviews
FROM cards c
LEFT JOIN review_state rs ON rs.cardId = c.id
WHERE c.deckId = :deckId
  AND (c.front LIKE '%' || :q || '%' OR c.back LIKE '%' || :q || '%')
ORDER BY c.id ASC
""")
    List<WordWithStats> searchWords(long deckId, String q);

    // Статистика по словам — ОБЯЗАТЕЛЬНО фильтровать все join’ы по deckId
    @Query("""
SELECT
  c.id            AS cardId,
  c.front         AS front,
  c.back          AS back,
  c.learned       AS learned,
  rs.ease         AS ease,
  rs.lastGrade    AS lastGrade,
  COALESCE(COUNT(l.id),0) AS totalReviews
FROM cards c
LEFT JOIN review_state  rs ON rs.cardId = c.id
LEFT JOIN review_log    l  ON l.cardId = c.id
LEFT JOIN learned_state ls ON ls.cardId = c.id
WHERE c.deckId = :deckId
GROUP BY c.id
ORDER BY c.id ASC
""")
    List<WordWithStats> getWordsWithStats(long deckId);


    @Query("SELECT COUNT(*) FROM cards")
    int countAll();

    @Query("SELECT * FROM cards ORDER BY id LIMIT :limit OFFSET :offset")
    List<Card> getPage(int limit, int offset);

    // Если всё-таки хочешь фильтровать (когда в файле смешаны разные deckId):






    @Query("UPDATE cards SET learned = :learned WHERE id = :cardId")
    void setLearned(long cardId, int learned);


    @RewriteQueriesToDropUnusedColumns // можно оставить, но мы и так убрали лишнее
    @Query("""
    SELECT 
        c.id AS cardId,
        c.deckId AS deckId,
        c.front AS front,
        c.back  AS back,
        COALESCE(rs.ease, 0) AS ease,
        rs.lastGrade AS lastGrade,
        (SELECT COUNT(*) FROM review_log rl WHERE rl.cardId = c.id) AS totalReviews,
        COALESCE(ls.learned, 0) AS learned
    FROM cards c
    LEFT JOIN review_state rs ON rs.cardId = c.id
    LEFT JOIN learned_state ls ON ls.cardId = c.id
    WHERE c.deckId = :deckId
      AND COALESCE(c.excluded, 0) = 0
      AND COALESCE(ls.learned, 0) = 0   -- 💡 Отсекаем "выполненные"
    ORDER BY rs.dueAt ASC, c.id ASC
    LIMIT :limit
    """)
    List<WordWithStats> getSelection(long deckId, int limit);


    @Query("UPDATE cards SET excluded = CASE WHEN :excluded THEN 1 ELSE 0 END WHERE id=:cardId")
    void setExcluded(long cardId, boolean excluded);

    // Обновление/вставка флага learned (без ON CONFLICT, совместимо со старым SQLite):
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

    @Query("""
    SELECT 
        c.id AS cardId,
        c.front AS front,
        c.back  AS back,
        COALESCE(rs.ease, 0)      AS ease,
        rs.lastGrade              AS lastGrade,
        (SELECT COUNT(*) FROM review_log rl WHERE rl.cardId = c.id) AS totalReviews,
        COALESCE(ls.learned, 0)   AS learned
    FROM cards c
    LEFT JOIN review_state rs ON rs.cardId = c.id
    LEFT JOIN learned_state ls ON ls.cardId = c.id
    ORDER BY rs.dueAt ASC, c.id ASC
    """)
    List<WordWithStats> getWordsWithStatsAll();

    @Query("""
    SELECT
        c.id                         AS cardId,         -- нужно для WordWithStats.cardId
        c.front                      AS front,
        c.back                       AS back,
        COALESCE(ls.learned, 0)      AS learned,        -- boolean/int в POJO
        rs.ease                      AS ease,           -- если в POJO не-null, сделай COALESCE
        rs.lastGrade                 AS lastGrade,      -- если примитив int, сделай COALESCE
        (
          SELECT COUNT(*) FROM review_log rl
          WHERE rl.cardId = c.id
        )                            AS totalReviews    -- нужно для WordWithStats.totalReviews
    FROM cards c
    LEFT JOIN learned_state ls ON ls.cardId = c.id
    LEFT JOIN review_state  rs ON rs.cardId = c.id
    WHERE COALESCE(ls.learned, 0) = 0          -- только невыученные
      AND c.excluded = 0
      AND c.deckId = :deckId
    ORDER BY c.id
    """)
    List<WordWithStats> getUnlearnedWords(long deckId);

    @Query("""
    SELECT
      c.id                                  AS cardId,
      c.front                               AS front,
      c.back                                AS back,
      COALESCE(ls.learned, 0)               AS learned,
      rs.ease                               AS ease,
      rs.lastGrade                          AS lastGrade,
      COALESCE(rl.cnt, 0)                   AS totalReviews
    FROM cards c
    LEFT JOIN learned_state ls ON ls.cardId = c.id
    LEFT JOIN review_state  rs ON rs.cardId = c.id
    LEFT JOIN (
        SELECT cardId, COUNT(*) AS cnt
        FROM review_log
        GROUP BY cardId
    ) rl ON rl.cardId = c.id
    WHERE c.deckId = :deckId
    """)
    List<WordWithStats> getWordsWithLearned(long deckId);







}

/*@Dao
public interface CardDao {

    @Query("SELECT COUNT(*) FROM cards WHERE deckId=:deckId AND excluded=1")
    int countExcluded(long deckId);

    @Query("SELECT COUNT(*) FROM cards WHERE deckId=:deckId AND learned=1")
    int countLearnedCards(long deckId); // только если используешь cards.learned

    @Query("""
SELECT COUNT(*) FROM learned_state ls
JOIN cards c ON c.id=ls.cardId
WHERE c.deckId=:deckId AND ls.learned=1
""")
    int countLearnedState(long deckId); // если используешь learned_state


    @Query("UPDATE cards SET excluded = :v WHERE id = :cardId")
    void setExcluded(long cardId, boolean v);

    @Query("UPDATE cards SET learned  = :v WHERE id = :cardId")
    void setLearned(long cardId, boolean v);

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id DESC")
    List<Card> getByDeck(long deckId);

    @Transaction
    default void upsertLearnedCompat(long cardId, boolean isLearned) {
        try {
            LearnedState ls = new LearnedState();
            ls.cardId = cardId;
            ls.learned = isLearned;
            _insertLearned(ls); // @Insert(ABORT)
        } catch (Exception ignore) {
            _updateLearned(cardId, isLearned);
        }
    }


    @Insert(onConflict = OnConflictStrategy.ABORT)
    void _insertLearned(LearnedState ls);

    @Query("UPDATE learned_state SET learned = :isLearned WHERE cardId = :cardId")
    void _updateLearned(long cardId, boolean isLearned);
    @Query("SELECT COUNT(*) FROM cards")
    int countCards();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAllIgnore(List<Card> cards);

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY RANDOM() LIMIT 1")
    Card getRandomBlocking(long deckId);

    @Query("SELECT * FROM cards")
    List<Card> getAllBlocking();

    @Query("SELECT * FROM cards ORDER BY id DESC")
    List<Card> getAll();

    @Query("""
    SELECT c.*
    FROM cards c
    LEFT JOIN review_state s ON s.cardId = c.id
    WHERE (s.dueAt IS NULL OR s.dueAt <= :nowMillis)
    ORDER BY IFNULL(s.dueAt, 0) ASC
    LIMIT :limit
""")
    List<Card> dueCards(long nowMillis, int limit);

    @Query("""
    SELECT
      c.id            AS id,
      c.front         AS front,
      c.back          AS back,
      s.ease          AS ease,
      s.lastGrade     AS lastGrade,
      s.intervalDays  AS intervalDays,
      s.dueAt         AS dueAt,
      COUNT(l.id)     AS totalReviews
    FROM cards c
    LEFT JOIN review_state s ON s.cardId = c.id
    LEFT JOIN review_log   l ON l.cardId = c.id
    GROUP BY c.id
    ORDER BY c.createdAt DESC, c.id DESC
""")
    List<WordWithSchedule> getWordsWithSchedule();


    @Query("""
    SELECT
        c.id                  AS cardId,
        c.front               AS front,
        c.back                AS back,
        IFNULL(ls.learned,0)  AS learned,
        rs.ease               AS ease,
        rs.lastGrade          AS lastGrade,
        COUNT(l.id)           AS totalReviews
    FROM cards c
    LEFT JOIN review_state  rs ON rs.cardId = c.id
    LEFT JOIN review_log    l  ON l.cardId = c.id
    LEFT JOIN learned_state ls ON ls.cardId = c.id
    WHERE IFNULL(ls.learned,0) = 0
    GROUP BY c.id
    ORDER BY c.id ASC
""")
    List<WordWithStats> getWordsWithStatsNotLearned();

    @Query("""
    SELECT
        c.id                 AS cardId,
        c.front              AS front,
        c.back               AS back,
        IFNULL(ls.learned,0) AS learned,
        rs.ease              AS ease,
        rs.lastGrade         AS lastGrade,
        COUNT(l.id)          AS totalReviews
    FROM cards c
    LEFT JOIN review_state  rs ON rs.cardId = c.id
    LEFT JOIN review_log    l  ON l.cardId = c.id
    LEFT JOIN learned_state ls ON ls.cardId = c.id
    GROUP BY c.id
    ORDER BY c.id ASC
""")
    List<WordWithStats> getWordsWithStatsAll();



}*/

