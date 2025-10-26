package com.example.cards.data.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.LearnedState;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(
        entities = { Card.class, ReviewState.class, ReviewLog.class, LearnedState.class },
        version = 1,                // можно оставить 13, но удобнее принять текущую схему как v1
        exportSchema = false        // чтобы не возиться с папкой схем
)
public abstract class AppDatabase extends RoomDatabase {
    public static final ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(4);

    private static volatile AppDatabase INSTANCE;

    public abstract CardDao cardDao();
    public abstract ReviewDao reviewDao();

    public static AppDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDatabase.class,
                                    "cards.db"        // при желании: "cards_v2.db"
                            )
                            // НИКАКИХ addMigrations(...)
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

    public static class DbFactory {
        public static AppDatabase forDeck(Context ctx, long deckId) {
            String dbFileName = "cards_deck_" + deckId + ".db";
            String assetName  = "db/cards_deck_" + deckId + ".db";

            // 1) Копируем из assets только если файла ещё нет
            java.io.File dbPath = ctx.getDatabasePath(dbFileName);
            if (!dbPath.exists()) {
                dbPath.getParentFile().mkdirs();
                try (java.io.InputStream in = ctx.getAssets().open(assetName);
                     java.io.OutputStream out = new java.io.FileOutputStream(dbPath)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to copy prepackaged DB: " + assetName, e);
                }
            }

            // 2) Открываем СУЩЕСТВУЮЩИЙ файл без разрушительных миграций
            return Room.databaseBuilder(ctx.getApplicationContext(), AppDatabase.class, dbFileName)
                    //.fallbackToDestructiveMigration()              // апгрейд без миграций
                    .createFromAsset(assetName)
                    .fallbackToDestructiveMigrationOnDowngrade()   // даунгрейд без миграций (твой случай)
                    .addCallback(new RoomDatabase.Callback() {
                        @Override public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            db.execSQL("PRAGMA foreign_keys = ON");
                        }
                    })
                    .build();

        }
    }
    public static void logUserVersion(Context ctx, String dbName) {
        File f = ctx.getDatabasePath(dbName);
        if (!f.exists()) {
            Log.d("DB", dbName + " : file not found");
            return;
        }
        SQLiteDatabase sql = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        Cursor c = sql.rawQuery("PRAGMA user_version", null);
        if (c.moveToFirst()) {
            int v = c.getInt(0);
            Log.d("DB", dbName + " user_version=" + v + " size=" + f.length());
        }
        c.close();
        sql.close();
    }


}
