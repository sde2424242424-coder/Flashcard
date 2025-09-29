package com.example.cards.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;

@Database(
        entities = { Card.class, ReviewState.class, ReviewLog.class },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CardDao cardDao();
    public abstract ReviewDao reviewDao();
}
