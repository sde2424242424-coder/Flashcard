package com.example.cards,data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;

import java.util.list;

@Dao
public interface ReviewDao{
    void upsertState(ReviewState s);
    @Query("""
    SELECT Card.*
    FROM Card
    JOIN ReviewState ON Card.id = ReviewState.cardId
    WHERE ReviewState.dueAt <= :now
    ORDER BY ReviewState.dueAt ASC
    LIMIT :limit
    """)
    List<Card> dueCards(long now, int limit);
    @Query("SELECT * FROM ReviewState WHERE cardId = :cardId")
    ReviewState getState(long cardId);
    @Insert
    void insertLog(ReviewLog log);

}