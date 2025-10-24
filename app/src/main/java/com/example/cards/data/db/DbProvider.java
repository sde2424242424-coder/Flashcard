package com.example.cards.data.db;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class DbProvider {

    private static final Map<Long, AppDatabase> CACHE = new HashMap<>();

    private DbProvider() {}

    public static synchronized AppDatabase getDatabase(@NonNull Context ctx, long deckId) {
        if (deckId < 1) throw new IllegalArgumentException("deckId must start from 1");

        AppDatabase cached = CACHE.get(deckId);
        if (cached != null) return cached;

        // ОДИН ЖЁСТКИЙ ШАБЛОН ИМЕНИ ДЛЯ ВСЕХ КОЛОД:
        final String dbFileName = "cards_deck_" + deckId + ".db";

        // (опц.) Если хочешь держать БД именно в стандартной папке БД приложения:
        File dbPath = ctx.getDatabasePath(dbFileName);
        File parent = dbPath.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        AppDatabase db = Room.databaseBuilder(ctx.getApplicationContext(), AppDatabase.class, dbFileName)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                // Если у тебя бывают изменения схемы без миграций:
                // .fallbackToDestructiveMigration()
                .addCallback(new RoomDatabase.Callback() {
                    @Override public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        db.execSQL("PRAGMA foreign_keys = ON");
                        android.util.Log.d(
                                "DB",
                                "Opened deck " + deckId + ": " + dbFileName +
                                        " (" + new Date(dbPath.lastModified()) + ")"
                        );
                    }
                })
                .build();

        CACHE.put(deckId, db);
        return db;
    }
}
