package com.example.cards.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.cards.data.model.Card;
import com.example.cards.data.model.ReviewLog;
import com.example.cards.data.model.ReviewState;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = { Card.class, ReviewState.class, ReviewLog.class },
        version = 11,                 // <-- одна миграция 1->2
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CardDao cardDao();
    public abstract ReviewDao reviewDao();

    public static final ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(4);

    // ---------- MIGRATIONS ----------
    // 9 -> 10: createdAt + индекс + review_state (всё с проверками)
    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            // helper: есть ли колонка?
            boolean hasCreatedAt = false;
            try (android.database.Cursor c = db.query("PRAGMA table_info(`cards`)")) {
                while (c.moveToNext()) if ("createdAt".equals(c.getString(1))) { hasCreatedAt = true; break; }
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

    // 10 -> 11: ТОЛЬКО ts в review_log (+ индекс)
    public static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            boolean hasTs = false;
            try (android.database.Cursor c = db.query("PRAGMA table_info(`review_log`)")) {
                while (c.moveToNext()) if ("ts".equals(c.getString(1))) { hasTs = true; break; }
            }
            if (!hasTs) {
                db.execSQL("ALTER TABLE `review_log` ADD COLUMN `ts` INTEGER NOT NULL DEFAULT 0");
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_log_cardId` ON `review_log`(`cardId`)");
        }
    };

    public static class DbFactory {
        public static AppDatabase forDeck(Context ctx, long deckId) {
            String dbFileName = "cards_deck_" + deckId + ".db";     // имя файла БД на устройстве
            String assetName  = "db/cards_deck_" + deckId + ".db";  // имя файла в assets/

            return Room.databaseBuilder(ctx.getApplicationContext(), AppDatabase.class, dbFileName)
                    .createFromAsset(assetName)
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11) // <-- ОБЕ
                    .addCallback(READ_ONLY_CALLBACK)
                    .build();

        }

        private static final Callback READ_ONLY_CALLBACK = new Callback() {
            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
                // Блокируем любые записи защитными триггерами:
                db.execSQL("CREATE TRIGGER IF NOT EXISTS block_insert_cards " +
                        "BEFORE INSERT ON cards BEGIN SELECT RAISE(ABORT,'Read-only DB'); END;");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS block_update_cards " +
                        "BEFORE UPDATE ON cards BEGIN SELECT RAISE(ABORT,'Read-only DB'); END;");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS block_delete_cards " +
                        "BEFORE DELETE ON cards BEGIN SELECT RAISE(ABORT,'Read-only DB'); END;");

                // Если есть другие таблицы (decks, tags и т.п.) — продублируй триггеры для них.
            }
        };
    }
}
    /*
    private static final List<Card> SEED_LIST = Arrays.asList(
            make(1, "일", "работа; день"),
            make(1, "학교", "школа"),
            make(1, "시간", "время; час"),
            make(1, "사람", "человек"),
            make(1, "물", "вода"),
            make(1, "사랑", "любовь"),
            make(1, "책", "книга"),
            make(1, "음식", "еда"),
            make(1, "컴퓨터", "компьютер"),
            make(1, "친구", "друг"),
            make(1, "가족", "семья"),
            make(1, "집", "дом"),
            make(1, "자동차", "автомобиль"),
            make(1, "길", "дорога"),
            make(1, "마음", "душа; сердце"),
            make(1, "눈", "глаз; снег"),
            make(1, "손", "рука (кисть)"),
            make(1, "전화", "телефон"),
            make(1, "영화", "фильм"),
            make(1, "공부", "учёба"),
            make(1, "음악", "музыка"),
            make(1, "여행", "путешествие"),
            make(1, "책상", "письменный стол"),
            make(1, "창문", "окно"),
            make(1, "문", "дверь"),
            make(1, "가방", "сумка"),
            make(1, "연필", "карандаш"),
            make(1, "지우개", "ластик"),
            make(1, "학생", "студент"),
            make(1, "도시", "город"),
            make(1, "공항", "аэропорт"),
            make(1, "병원", "больница"),
            make(1, "지하철", "метро"),
            make(1, "버스", "автобус"),
            make(1, "학교", "учебное заведение"),
            make(1, "작품", "произведение (искусства)"),
            make(1, "음료수", "напиток"),
            make(1, "강아지", "щенок"),
            make(1, "고양이", "кот"),
            make(1, "식물", "растение"),
            make(1, "동물", "животное"),
            make(1, "손톱", "ноготь"),
            make(1, "발톱", "коготь"),
            make(1, "가을", "осень"),
            make(1, "봄", "весна"),
            make(1, "여름", "лето"),
            make(1, "겨울", "зима"),
            make(1, "꽃", "цветок"),
            make(1, "나무", "дерево"),
            make(1, "바다", "море"),
            make(1, "산", "гора"),
            make(1, "강", "река"),
            make(1, "호수", "озеро"),
            make(1, "자전거", "велосипед"),
            make(1, "자동차", "машина"),
            make(1, "비행기", "самолёт"),
            make(1, "배", "лодка"),
            make(1, "스키", "лыжи"),
            make(1, "수영", "плавание"),
            make(1, "운동", "упражнение, спорт"),
            make(1, "노래", "песня"),
            make(1, "춤", "танец"),
            make(1, "사진", "фотография"),
            make(1, "동영상", "видео"),
            make(1, "그림", "картина"),
            make(1, "책", "книга"),
            make(1, "신문", "газета"),
            make(1, "잡지", "журнал"),
            make(1, "인터넷", "интернет"),
            make(1, "전화", "телефон"),
            make(1, "컴퓨터", "компьютер"),
            make(1, "프린터", "принтер"),
            make(1, "모니터", "монитор"),
            make(1, "키보드", "клавиатура"),
            make(1, "마우스", "мышь"),
            make(1, "의자", "стул"),
            make(1, "책상", "письменный стол"),
            make(1, "침대", "кровать"),
            make(1, "소파", "диван"),
            make(1, "탁자", "стол"),
            make(1, "부엌", "кухня"),
            make(1, "욕실", "ванная комната"),
            make(1, "화장실", "туалет"),
            make(1, "거실", "гостиная"),
            make(1, "방", "комната"),
            make(1, "창문", "окно"),
            make(1, "문", "дверь"),
            make(1, "계단", "лестница"),
            make(1, "엘리베이터", "лифт"),
            make(1, "복도", "коридор"),
            make(1, "지붕", "крыша"),
            make(1, "벽", "стена"),
            make(1, "바닥", "пол")
    );*/
