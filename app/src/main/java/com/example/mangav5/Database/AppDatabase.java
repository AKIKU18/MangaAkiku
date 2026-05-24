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


    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "manga_db")
                    .addMigrations(DatabaseMigrations.getAllMigrations())
                    .build();
        }
        return INSTANCE;
    }
}
