package com.example.mangav5.Database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class DatabaseMigrations {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE history ADD COLUMN scrollPosition INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `settingsItem` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `setting_key` TEXT DEFAULT 'undefined', `setting_value` TEXT DEFAULT 'undefined')");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS settingsItem");
            database.execSQL("CREATE TABLE IF NOT EXISTS `settingsItem` (`key` TEXT NOT NULL PRIMARY KEY, `value` TEXT)");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE bookmarks ADD COLUMN lastChapter TEXT DEFAULT ''");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `sourceEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mainSource` TEXT)");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `sourceEntity_new` (`id` INTEGER NOT NULL, `mainSource` TEXT, PRIMARY KEY(`id`))");
            database.execSQL("INSERT INTO sourceEntity_new (id, mainSource) SELECT 1, mainSource FROM sourceEntity LIMIT 1");
            database.execSQL("DROP TABLE sourceEntity");
            database.execSQL("ALTER TABLE sourceEntity_new RENAME TO sourceEntity");
        }
    };

    public static Migration[] getAllMigrations() {
        return new Migration[]{MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7};
    }
}
