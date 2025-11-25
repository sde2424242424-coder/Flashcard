package com.example.cards.data.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.LearnedState;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AppDatabase
 *
 * Main Room database definition for the app. It is used in two modes:
 * 1) Global DB "cards.db" via {@link #getInstance(Context)} — for generic operations.
 * 2) Per-deck DBs "cards_deck_{id}.db" via {@link DbFactory#forDeck(Context, long)}.
 *
 * Entities:
 * - {@link Card}         – base card data
 * - {@link ReviewState}  – SM-2 review state per card
 * - {@link ReviewLog}    – history of reviews
 * - {@link LearnedState} – user-controlled learned flag
 *
 * Notes:
 * - Version = 1, exportSchema = false, destructive migration is used.
 * - Foreign keys are enabled on open with PRAGMA foreign_keys = ON.
 */
@Database(
        entities = { Card.class, ReviewState.class, ReviewLog.class, LearnedState.class },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Executor for background DB operations (queries, preloading, etc.).
     */
    public static final ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(4);

    private static volatile AppDatabase INSTANCE;

    public abstract CardDao cardDao();
    public abstract ReviewDao reviewDao();

    /**
     * Returns a singleton instance of the global database "cards.db".
     * This DB can be used when you do not rely on per-deck prepackaged files.
     *
     * Uses:
     * - fallbackToDestructiveMigration()
     * - foreign_keys pragma enabled on open
     */
    public static AppDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDatabase.class,
                                    "cards.db"   // can be renamed if needed (e.g. "cards_v2.db")
                            )
                            // No migrations are added; destructive migration is acceptable here.
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    db.execSQL("PRAGMA foreign_keys = ON");
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Quick integrity check for a DB file:
     * - File must exist.
     * - File size must be larger than a minimal threshold (not an empty SQLite header).
     * - Table "cards" must exist.
     * - Table "cards" must contain at least one row (expected preloaded content).
     *
     * @param dbFile file object pointing to the database
     * @return true if the DB looks valid, false otherwise
     */
    private static boolean isDbHealthy(File dbFile) {
        if (!dbFile.exists()) return false;
        if (dbFile.length() <= 4096) return false; // almost empty SQLite file

        try (SQLiteDatabase sql = SQLiteDatabase.openDatabase(
                dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {

            // Check that table "cards" exists.
            try (Cursor t = sql.rawQuery(
                    "SELECT 1 FROM sqlite_master WHERE type='table' AND name='cards'", null)) {
                if (!t.moveToFirst()) return false;
            }

            // Check that "cards" contains some data (if prepackaged DB is expected).
            try (Cursor c = sql.rawQuery("SELECT COUNT(*) FROM cards", null)) {
                if (c.moveToFirst()) {
                    int n = c.getInt(0);
                    if (n == 0) return false;
                } else {
                    return false;
                }
            }

            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * Copies a DB file from assets to the destination path.
     * Throws a RuntimeException on failure so that the caller can abort startup.
     *
     * @param assets    AssetManager to read from
     * @param assetPath path inside assets (e.g. "db/cards_deck_1.db")
     * @param dest      destination file inside /databases
     */
    private static void copyFromAssetsOrThrow(android.content.res.AssetManager assets,
                                              String assetPath,
                                              java.io.File dest) {
        try (java.io.InputStream in = assets.open(assetPath);
             java.io.OutputStream out = new java.io.FileOutputStream(dest)) {

            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();

            Log.d("DB", "Prepackaged DB copied from assets: " + assetPath +
                    " → " + dest.getAbsolutePath());

        } catch (Exception e) {
            Log.e("DB", "Error copying " + assetPath, e);
            throw new RuntimeException("Failed to copy prepackaged DB: " + assetPath, e);
        }
    }

    /**
     * Factory for deck-specific databases.
     *
     * Each deck uses its own DB file:
     *   cards_deck_{deckId}.db
     *
     * Behavior:
     * - Checks integrity of the existing deck DB file using {@link #isDbHealthy(File)}.
     * - If the file is missing or invalid, it is deleted and then copied
     *   from assets/db/cards_deck_{deckId}.db.
     * - After that, Room builds a database instance for that file.
     */
    public static class DbFactory {

        /**
         * Returns an AppDatabase instance bound to a deck-specific file,
         * e.g. "cards_deck_1.db".
         *
         * @param ctx    context
         * @param deckId deck identifier (1..N)
         */
        public static AppDatabase forDeck(Context ctx, long deckId) {
            String dbFileName = "cards_deck_" + deckId + ".db";
            File dbPath = ctx.getDatabasePath(dbFileName);
            dbPath.getParentFile().mkdirs();

            // Integrity check; if the DB is not healthy, recreate it from assets.
            if (!isDbHealthy(dbPath)) {
                if (dbPath.exists()) dbPath.delete();
                copyFromAssetsOrThrow(ctx.getAssets(), "db/" + dbFileName, dbPath);
            } else {
                Log.d("DB", "Using existing healthy DB: " + dbPath);
            }

            return Room.databaseBuilder(ctx.getApplicationContext(), AppDatabase.class, dbFileName)
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            db.execSQL("PRAGMA foreign_keys = ON");
                        }
                    })
                    .build();
        }
    }

    /**
     * Utility method to log PRAGMA user_version and file size
     * for a given DB file, useful for debugging migrations/prepackaged DBs.
     *
     * @param ctx    context
     * @param dbName database file name (e.g. "cards.db" or "cards_deck_1.db")
     */
    public static void logUserVersion(Context ctx, String dbName) {
        File f = ctx.getDatabasePath(dbName);
        if (!f.exists()) {
            Log.d("DB", dbName + " : file not found");
            return;
        }

        SQLiteDatabase sql = SQLiteDatabase.openDatabase(
                f.getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY
        );
        Cursor c = sql.rawQuery("PRAGMA user_version", null);
        if (c.moveToFirst()) {
            int v = c.getInt(0);
            Log.d("DB", dbName + " user_version=" + v + " size=" + f.length());
        }
        c.close();
        sql.close();
    }
}
