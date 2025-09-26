package com.example.cards.date.db;

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.onConflictStrategy;
import androidx.room.Query;

import com.example.cards.date.model.Card;

@Dao
public interface CardDao {
    @Insert(onConflict = onConflictStrategy.REPLACE)
    long upsert(Card c);
    @Query("SELECT * FROM Card ORDER BY createdAt DESC LIMIT :limit")
    list<Card> latest(int limit);
    @Query("SELECT * FROM card WHERE id = :id")
    Card getById(long id);
    void deleteById(long id);
}