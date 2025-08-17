package com.example.mangav5.Database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Dao.ChapterDao;
import com.example.mangav5.Dao.HistoryDao;
import com.example.mangav5.Dao.MangaItemDao;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.Entity.MangaItemEntity;

@Database(
        entities = {BookmarkEntity.class, ChapterItemEntity.class, MangaItemEntity.class, HistoryEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase INSTANCE;

    public abstract BookmarkDao bookmarkDao();
    public abstract ChapterDao chapterDao();
    public abstract MangaItemDao mangaItemDao();
    public abstract HistoryDao historyDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "manga_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}
