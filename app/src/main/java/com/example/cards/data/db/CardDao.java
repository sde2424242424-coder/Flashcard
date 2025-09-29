package com.example.cards.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cards.data.model.Card;

import java.util.List;

@Dao
public interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Card c);

    @Query("SELECT * FROM Card ORDER BY createdAt DESC LIMIT :limit")
    List<Card> latest(int limit);

    @Query("SELECT * FROM Card WHERE id = :id")
    Card getById(long id);

    @Query("DELETE FROM Card WHERE id = :id")
    void deleteById(long id);
}
