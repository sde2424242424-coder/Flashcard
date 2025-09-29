package com.example.cards.data.db;

import android.content.Context;

import androidx.room.Room;

public final class DbProvider {
    private static volatile AppDatabase INSTANCE;
    public static AppDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (DbProvider.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        ctx.getApplicationContext(),
                        AppDatabase.class,
                        "cards.db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    private DbProvider(){}
}