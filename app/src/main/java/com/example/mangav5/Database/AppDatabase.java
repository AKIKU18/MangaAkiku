package com.example.mangav5.Database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;

import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Dao.ChapterDao;
import com.example.mangav5.Dao.HistoryDao;
import com.example.mangav5.Dao.MangaItemDao;
import com.example.mangav5.Dao.SettingsDao;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Entity.SettingsItemEntity;

@Database(
        entities = {BookmarkEntity.class, ChapterItemEntity.class, MangaItemEntity.class, HistoryEntity.class, SettingsItemEntity.class},
        version = 4, // ⬅️ incremented from 3 → 4
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract BookmarkDao bookmarkDao();
    public abstract ChapterDao chapterDao();
    public abstract MangaItemDao mangaItemDao();
    public abstract HistoryDao historyDao();
    public abstract SettingsDao settingsDao();


    // 🔹 Define your migration here
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new column with default value
            database.execSQL("ALTER TABLE history ADD COLUMN scrollPosition INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new Settings table with correct column names
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `settingsItem` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`setting_key` TEXT DEFAULT 'undefined', " +
                            "`setting_value` TEXT DEFAULT 'undefined')"
            );
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Drop the old table if it exists
            database.execSQL("DROP TABLE IF EXISTS settingsItem");

            // Create new table with `key` as primary key
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `settingsItem` (" +
                            "`key` TEXT NOT NULL PRIMARY KEY, " +
                            "`value` TEXT)"
            );
        }
    };





    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "manga_db")
                    // 👇 Register the migration instead of destructive migration
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    // Optionally keep fallback for dev
                    // .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}
