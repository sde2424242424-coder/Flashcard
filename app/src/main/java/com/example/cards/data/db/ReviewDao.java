package com.example.cards.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;

import java.util.List;

@Dao
public interface ReviewDao {

    @Query("""
        SELECT c.* 
        FROM cards c
        LEFT JOIN review_state s ON s.cardId = c.id
        WHERE (s.dueAt IS NULL OR s.dueAt <= :nowMillis)
        ORDER BY IFNULL(s.dueAt, 0) ASC
        LIMIT :limit
    """)
    List<Card> dueCards(long nowMillis, int limit);

    @Query("SELECT * FROM review_state WHERE cardId = :cardId LIMIT 1")
    ReviewState getState(long cardId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsertState(ReviewState state);

    @Insert
    long insertLog(ReviewLog log);
}
