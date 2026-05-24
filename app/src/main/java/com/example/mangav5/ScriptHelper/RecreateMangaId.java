package com.example.mangav5.ScriptHelper;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.MangaItemEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class RecreateMangaId {
    private static final String TAG = "RecreateMangaId";

    /**
     * Executes the migration process to update all mangaId entries in the database
     * to the new hex UUID format derived from the manga URL.
     */
    public static void execute(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                Log.d(TAG, "Starting Manga ID recreation...");

                // 1. Process mangaItem table
                List<MangaItemEntity> mangaItems = db.mangaItemDao().getAllManga();
                for (MangaItemEntity manga : mangaItems) {
                    migrateId(db, manga.mangaId, manga.mangaUrl, "mangaItem");
                }

                // 2. Process bookmarks table
                List<BookmarkEntity> bookmarks = db.bookmarkDao().getAllBookmarks();
                for (BookmarkEntity bookmark : bookmarks) {
                    migrateId(db, bookmark.mangaId, bookmark.mangaUrl, "bookmarks");
                }

                Log.d(TAG, "Manga ID recreation finished successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Error during Manga ID recreation", e);
            }
        });
    }

    private static void migrateId(AppDatabase db, String oldId, String url, String tableName) {
        if (url == null || url.isEmpty() || oldId == null) return;
        
        String newId = GenerateMangaIDHex.generateUuidHex(url);
        if (oldId.equals(newId)) return;

        db.runInTransaction(() -> {
            try {
                // Check if newId already exists in the target table to avoid PK conflicts
                boolean exists = false;
                Cursor cursor = db.getOpenHelper().getReadableDatabase().query(
                        "SELECT 1 FROM " + tableName + " WHERE mangaId = ?", new Object[]{newId});
                if (cursor != null) {
                    exists = cursor.getCount() > 0;
                    cursor.close();
                }

                // Update dependent tables: chapterItem and history
                // We use UPDATE OR IGNORE to handle cases where the same chapter/history might already exist under the new ID
                db.getOpenHelper().getWritableDatabase().execSQL(
                        "UPDATE OR IGNORE chapterItem SET mangaId = ? WHERE mangaId = ?", new Object[]{newId, oldId});
                db.getOpenHelper().getWritableDatabase().execSQL(
                        "UPDATE OR IGNORE history SET mangaId = ? WHERE mangaId = ?", new Object[]{newId, oldId});

                if (exists) {
                    // If newId already exists in the main table, we delete the old entry
                    // (references have already been updated or ignored above)
                    db.getOpenHelper().getWritableDatabase().execSQL(
                            "DELETE FROM " + tableName + " WHERE mangaId = ?", new Object[]{oldId});
                    Log.d(TAG, "Merged " + oldId + " -> " + newId + " in " + tableName + " (newId already existed)");
                } else {
                    // If newId doesn't exist, we safely update the old entry to the new ID
                    db.getOpenHelper().getWritableDatabase().execSQL(
                            "UPDATE " + tableName + " SET mangaId = ? WHERE mangaId = ?", new Object[]{newId, oldId});
                    Log.d(TAG, "Migrated " + oldId + " -> " + newId + " in " + tableName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate " + oldId + " in " + tableName, e);
            }
        });
    }
}
