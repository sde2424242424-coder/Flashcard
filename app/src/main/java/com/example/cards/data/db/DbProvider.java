package com.example.cards.data.db;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DbProvider {
    private static final String TAG = "DbProvider";
    private static final String DB_PREFIX = "cards_deck_";
    private static final String DB_SUFFIX = ".db";
    private static final String[] ASSET_DIRS = new String[] {
            "",                 // assets/<file>
            "databases/",       // assets/databases/<file>
            "db/"               // assets/db/<file>
    };

    private static final ConcurrentMap<String, AppDatabase> CACHE = new ConcurrentHashMap<>();

    private DbProvider() {}

    public static AppDatabase forDeck(@NonNull Context context, long deckId) {
        String dbName = fileNameForDeck(deckId);
        ensurePrepackagedIfNeeded(context, dbName);
        return CACHE.computeIfAbsent(dbName, key ->
                Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, key)
                        // ВАЖНО: не используем createFromAsset одновременно с ручным копированием
                        // migrations при необходимости добавляйте здесь
                        .fallbackToDestructiveMigration() // можно оставить; не тронет уже скопированный файл
                        .build()
        );
    }

    private static String fileNameForDeck(long deckId) {
        return DB_PREFIX + deckId + DB_SUFFIX; // например, cards_deck_1.db
    }

    /** Если файла базы нет — попытаться скопировать из assets. */
    private static void ensurePrepackagedIfNeeded(@NonNull Context context, @NonNull String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        if (dbFile.exists()) return;

        // гарантируем, что папка databases/ существует
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        boolean copied = copyFromAssets(context.getAssets(), dbName, dbFile);
        if (!copied) {
            Log.e(TAG, "Не удалось найти/скопировать " + dbName + " из assets. Проверьте пути и имя файла.");
            // Создадим пустую БД, чтобы приложение не падало, но в логах будет ошибка
            // Room потом создаст схему. Если нужно – бросайте RuntimeException вместо этого.
        } else {
            Log.d(TAG, "База успешно скопирована: " + dbFile.getAbsolutePath() + " (" + dbFile.length() + " байт)");
        }
    }

    private static boolean copyFromAssets(AssetManager am, String dbName, File dest) {
        for (String dir : ASSET_DIRS) {
            String assetPath = dir + dbName;
            try (InputStream in = am.open(assetPath);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
                out.flush();
                return true;
            } catch (Exception ignore) {
                // пробуем следующий путь
            }
        }
        return false;
    }
}
