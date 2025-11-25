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

/**
 * DbProvider
 *
 * Utility class responsible for providing {@link AppDatabase} instances
 * for deck-specific databases. Each deck is stored in its own file:
 * <pre>
 *   cards_deck_{deckId}.db
 * </pre>
 *
 * Behavior:
 * - On first access, checks whether the DB file exists in /data/data/.../databases/.
 * - If not, tries to copy a prepackaged DB from assets using several possible paths:
 *   "", "databases/", "db/".
 * - After copy (or if it already exists), builds a Room database with that file name.
 * - Caches Room instances in a static map to avoid rebuilding them.
 *
 * Notes:
 * - Manual asset copy is used instead of Room's createFromAsset to keep control.
 * - fallbackToDestructiveMigration() is enabled; it will not touch the manually
 *   copied file, but will handle schema changes if Room needs to recreate it.
 */
public final class DbProvider {

    private static final String TAG = "DbProvider";

    // File name pattern: cards_deck_<id>.db
    private static final String DB_PREFIX = "cards_deck_";
    private static final String DB_SUFFIX = ".db";

    // Possible asset subdirectories where deck DBs may reside.
    private static final String[] ASSET_DIRS = new String[] {
            "",           // assets/<file>
            "databases/", // assets/databases/<file>
            "db/"         // assets/db/<file>
    };

    // Cache of AppDatabase instances keyed by DB file name.
    private static final ConcurrentMap<String, AppDatabase> CACHE = new ConcurrentHashMap<>();

    private DbProvider() {
        // Utility class; no instances.
    }

    /**
     * Returns a deck-specific Room database instance for the given deckId.
     * If the corresponding DB file does not exist yet, tries to copy it
     * from assets before building the Room database.
     *
     * @param context application or activity context
     * @param deckId  deck identifier used in the DB file name
     * @return {@link AppDatabase} instance bound to cards_deck_{deckId}.db
     */
    public static AppDatabase forDeck(@NonNull Context context, long deckId) {
        String dbName = fileNameForDeck(deckId);

        // Ensure the prepackaged DB file is present before opening with Room.
        ensurePrepackagedIfNeeded(context, dbName);

        // Build or return cached Room instance.
        return CACHE.computeIfAbsent(dbName, key ->
                Room.databaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class,
                                key
                        )
                        // IMPORTANT: do not use createFromAsset together with manual copy.
                        // Add migrations here if needed.
                        .fallbackToDestructiveMigration()
                        .build()
        );
    }

    /**
     * Converts deckId into a DB file name, e.g. "cards_deck_1.db".
     */
    private static String fileNameForDeck(long deckId) {
        return DB_PREFIX + deckId + DB_SUFFIX;
    }

    /**
     * Ensures the prepackaged DB file exists in the databases folder.
     * If the file is missing, attempts to copy it from the assets directory.
     *
     * @param context app context
     * @param dbName  database file name
     */
    private static void ensurePrepackagedIfNeeded(@NonNull Context context,
                                                  @NonNull String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        if (dbFile.exists()) return;

        // Ensure /databases directory exists.
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        boolean copied = copyFromAssets(context.getAssets(), dbName, dbFile);
        if (!copied) {
            Log.e(TAG, "Failed to find/copy " + dbName +
                    " from assets. Check paths and file name.");
            // We do NOT throw; Room will create an empty DB with the schema.
            // If you want to crash on missing asset, throw a RuntimeException here instead.
        } else {
            Log.d(TAG, "Database copied successfully: " + dbFile.getAbsolutePath() +
                    " (" + dbFile.length() + " bytes)");
        }
    }

    /**
     * Tries to copy the DB file from assets into the destination file.
     * Checks several possible asset directory prefixes.
     *
     * @param am      AssetManager
     * @param dbName  file name to look for
     * @param dest    target file in the app's databases directory
     * @return true if copy succeeded from any path, false otherwise
     */
    private static boolean copyFromAssets(AssetManager am, String dbName, File dest) {
        for (String dir : ASSET_DIRS) {
            String assetPath = dir + dbName;
            try (InputStream in = am.open(assetPath);
                 FileOutputStream out = new FileOutputStream(dest)) {

                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                }
                out.flush();
                return true;
            } catch (Exception ignore) {
                // Try the next possible asset path.
            }
        }
        return false;
    }
}
