package com.example.cards.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;


import com.example.cards.data.model.Card;
import com.example.cards.data.model.WordWithStats;

import java.util.List;
@Dao
public interface CardDao {
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
      c.id                AS id,
      c.front             AS front,
      c.back              AS back,
      s.ease              AS ease,
      s.lastGrade         AS lastGrade,
      s.intervalDays      AS intervalDays,
      s.dueAt             AS dueAt,
      COUNT(l.id)         AS totalReviews
    FROM cards c
    LEFT JOIN review_state s ON s.cardId = c.id
    LEFT JOIN review_log   l ON l.cardId = c.id
    GROUP BY c.id
    ORDER BY c.createdAt DESC, c.id DESC
""")
    List<WordWithStats> getWordsWithStats();


    //@Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt DESC")
    //LiveData<List<Card>> getByDeckLive(long deckId);

    //@Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt DESC")
    //List<Card> getByDeckBlocking(long deckId);
}


/*@Dao
public interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Card c);

    @Query("SELECT COUNT(*) FROM cards")  int countCards();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAllIgnore(List<Card> cards);

    //@Insert(onConflict = OnConflictStrategy.REPLACE)
    //List<Long> insertAll(List<Card> list);

    //@Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id DESC")
    //LiveData<List<Card>> getByDeck(long deckId);
    @Query("SELECT * FROM cards ORDER BY createdAt DESC LIMIT :limit")
    List<Card> latest(int limit);

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY RANDOM() LIMIT 1")
    Card getRandomBlocking(long deckId);

    @Query("DELETE FROM cards WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM cards")
    List<Card> getAllBlocking();*/


