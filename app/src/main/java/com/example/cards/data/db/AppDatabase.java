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
                    .fallbackToDestructiveMigration()              // апгрейд без миграций
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

/*@Database(
        entities = { Card.class, ReviewState.class, ReviewLog.class, LearnedState.class },
        version = 13,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CardDao cardDao();
    public abstract ReviewDao reviewDao();

    public static final ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(4);

    // ---------- MIGRATIONS ----------

     public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            boolean hasCreatedAt = false;
            try (android.database.Cursor c = db.query("PRAGMA table_info(`cards`)")) {
                while (c.moveToNext()) {
                    if ("createdAt".equals(c.getString(1))) { hasCreatedAt = true; break; }
                }
            }
            if (!hasCreatedAt) {
                db.execSQL("ALTER TABLE `cards` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0");
            }

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cards_deckId_front_back` " +
                    "ON `cards`(`deckId`,`front`,`back`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `review_state`(" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "`cardId` INTEGER NOT NULL," +
                    "`intervalDays` INTEGER NOT NULL," +
                    "`ease` REAL NOT NULL," +
                    "`step` INTEGER NOT NULL," +
                    "`dueAt` INTEGER NOT NULL," +
                    "`lastGrade` INTEGER)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_review_state_cardId` " +
                    "ON `review_state`(`cardId`)");
        }
    };


    public static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `cards` ADD COLUMN `excluded` INTEGER NOT NULL DEFAULT 0");
        }
    };


    public static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            boolean hasExcluded = false;
            try (android.database.Cursor c = db.query("PRAGMA table_info(`cards`)")) {
                while (c.moveToNext()) {
                    if ("excluded".equals(c.getString(1))) { hasExcluded = true; break; }
                }
            }
            if (!hasExcluded) {
                db.execSQL("ALTER TABLE `cards` ADD COLUMN `excluded` INTEGER NOT NULL DEFAULT 0");
            }

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `learned_state` (" +
                            "`cardId` INTEGER NOT NULL, " +
                            "`learned` INTEGER NOT NULL DEFAULT 0, " +
                            "PRIMARY KEY(`cardId`))"
            );
        }
    };


    public static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `cards` ADD COLUMN `learned` INTEGER NOT NULL DEFAULT 0");
        }
    };



    static final Migration MIGRATION_11_13 = new Migration(11, 13) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 1) Создаём новую таблицу с нужной схемой (без UNIQUE на cardId)
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS review_state_new(
              id           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              cardId       INTEGER NOT NULL,
              intervalDays INTEGER NOT NULL,
              ease         REAL    NOT NULL,
              step         INTEGER NOT NULL,
              dueAt        INTEGER NOT NULL,
              lastGrade    INTEGER,
              FOREIGN KEY(cardId) REFERENCES cards(id) ON DELETE CASCADE ON UPDATE NO ACTION
            )
        """);

            // 2) Копируем данные
            db.execSQL("""
            INSERT INTO review_state_new (id, cardId, intervalDays, ease, step, dueAt, lastGrade)
            SELECT id, cardId, intervalDays, ease, step, dueAt, lastGrade
            FROM review_state
        """);

            // 3) Дропаем старую таблицу (уходят и её индексы/UNIQUE)
            db.execSQL("DROP TABLE review_state");

            // 4) Переименовываем новую
            db.execSQL("ALTER TABLE review_state_new RENAME TO review_state");

            // 5) Создаём НЕуникальный индекс с точным именем, которое ждёт Room
            db.execSQL("CREATE INDEX IF NOT EXISTS index_review_state_cardId ON review_state(cardId)");
        }
    };





    // ---------- INSTANCE (общая БД "cards.db") ----------

    public static AppDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDatabase.class,
                                    "cards.db"
                            )
                            .addMigrations(
                                    MIGRATION_9_10,
                                    MIGRATION_10_11,
                                    MIGRATION_11_12,
                                    MIGRATION_12_13,
                                    MIGRATION_11_13
                            )
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    // включаем внешние ключи каждый раз при открытии БД
                                    db.execSQL("PRAGMA foreign_keys = ON");
                                    // или так, если версия androidx.sqlite поддерживает:
                                    // db.setForeignKeyConstraintsEnabled(true);
                                }
                            })

                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // ---------- FACTORY ДЛЯ БАЗ ОТДЕЛЬНЫХ КОЛОД "cards_deck_<id>.db" ----------

    public static class DbFactory {

        public static AppDatabase forDeck(Context ctx, long deckId) {
            String dbFileName = "cards_deck_" + deckId + ".db";
            String assetName  = "db/cards_deck_" + deckId + ".db";

            return Room.databaseBuilder(ctx.getApplicationContext(), AppDatabase.class, dbFileName)
                    .createFromAsset(assetName)
                    .addMigrations(
                            MIGRATION_9_10,
                            MIGRATION_10_11,
                            MIGRATION_11_12,
                            MIGRATION_12_13,
                            MIGRATION_11_13
                    )
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            // включаем внешние ключи каждый раз при открытии БД
                            db.execSQL("PRAGMA foreign_keys = ON");
                            // или так, если версия androidx.sqlite поддерживает:
                            // db.setForeignKeyConstraintsEnabled(true);
                        }
                    })

                    // Удобно во время разработки; в релизе лучше убрать:
                    .fallbackToDestructiveMigration()
                    .build();
        }
    }
}*/
