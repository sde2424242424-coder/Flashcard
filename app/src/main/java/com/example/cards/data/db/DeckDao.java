/*package com.example.cards.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cards.data.model.Deck;

import java.util.List;

@Dao
public interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Deck deck);

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    LiveData<List<Deck>> getAllLive();

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    List<Deck> getAllBlocking();

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    Deck getById(long id);
}*/