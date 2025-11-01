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

/**
 * The main database class for the application, using Android Room Persistence Library.
 * This class is abstract and extends RoomDatabase. Room will generate an implementation of it.
 * It serves as the central access point to the underlying database.
 */
@Database(
        // Lists all the entity classes that are part of this database.
        // Each entity corresponds to a table in the database.
        entities = {BookmarkEntity.class, ChapterItemEntity.class, MangaItemEntity.class, HistoryEntity.class},
        // The version of the database schema. Must be incremented when the schema changes.
        version = 1,
        // Disables the export of the database schema into a folder.
        // For production apps, it's recommended to set this to true and check the schema into version control.
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    // A volatile instance of the database to ensure it is always up-to-date and visible to all threads.
    private static AppDatabase INSTANCE;

    // Abstract methods for each DAO (Data Access Object).
    // Room will generate the implementation for these methods, providing access to database operations.
    public abstract BookmarkDao bookmarkDao();
    public abstract ChapterDao chapterDao();
    public abstract MangaItemDao mangaItemDao();
    public abstract HistoryDao historyDao();

    /**
     * Returns a singleton instance of the AppDatabase.
     * This method ensures that only one instance of the database is created throughout the app's lifecycle,
     * which is an efficient and recommended practice.
     *
     * @param context The application context, used to get the database instance.
     * @return The single, synchronized instance of AppDatabase.
     */
    public static synchronized AppDatabase getInstance(Context context) {
        // If the INSTANCE is null, create a new database instance.
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "manga_db") // Creates the database with the specified name "manga_db".
                    // Defines a migration strategy. fallbackToDestructiveMigration will wipe and
                    // recreate the database if the version number changes, instead of performing a complex migration.
                    // This is simple for development but can lead to data loss in production.
                    .fallbackToDestructiveMigration()
                    .build(); // Builds and initializes the Room database.
        }
        // Return the existing instance.
        return INSTANCE;
    }
}
