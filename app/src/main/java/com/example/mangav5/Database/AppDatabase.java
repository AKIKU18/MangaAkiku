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
import com.example.mangav5.Dao.SourceDao;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Entity.SettingsItemEntity;
import com.example.mangav5.Entity.SourceEntity;

@Database(
        entities = {BookmarkEntity.class, ChapterItemEntity.class, MangaItemEntity.class, HistoryEntity.class, SettingsItemEntity.class, SourceEntity.class},
        version = 7, // ⬅️ incremented from 6 → 7
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract BookmarkDao bookmarkDao();
    public abstract ChapterDao chapterDao();
    public abstract MangaItemDao mangaItemDao();
    public abstract HistoryDao historyDao();
    public abstract SettingsDao settingsDao();
    public abstract SourceDao sourceDao();


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

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new column 'lastChapter' with default empty string to avoid null issues
            database.execSQL("ALTER TABLE bookmarks ADD COLUMN lastChapter TEXT DEFAULT ''");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sourceEntity` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`mainSource` TEXT" +
                            ")"
            );
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            // 1. Create the new table exactly as you want
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sourceEntity_new` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`mainSource` TEXT, " +
                            "PRIMARY KEY(`id`))"
            );

            // 2. Copy existing data (use id=1 for all existing rows if replacing)
            database.execSQL(
                    "INSERT INTO sourceEntity_new (id, mainSource) " +
                            "SELECT 1, mainSource FROM sourceEntity LIMIT 1"
            );

            // 3. Drop the old table
            database.execSQL("DROP TABLE sourceEntity");

            // 4. Rename new table to original name
            database.execSQL("ALTER TABLE sourceEntity_new RENAME TO sourceEntity");
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
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)

                    // Optionally keep fallback for dev
                    // .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}
