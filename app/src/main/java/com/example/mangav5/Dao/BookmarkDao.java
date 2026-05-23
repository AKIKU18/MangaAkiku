package com.example.mangav5.Dao;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.mangav5.Entity.BookmarkEntity;

import java.util.List;

@Dao
public interface BookmarkDao {
    @Insert
    void insert(BookmarkEntity bookmark);
    @Query("DELETE FROM bookmarks")
    void deleteAllBookmarks();

    @Delete
    void delete(BookmarkEntity bookmark);

    @Query("SELECT * FROM bookmarks")
    List<BookmarkEntity> getAllBookmarks();

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE mangaId = :id)")
    boolean isBookmarked(String id);

    @Query("UPDATE bookmarks SET lastChapter = :lastChapter WHERE mangaId = :mangaId")
    void updateLastChapter(String mangaId, String lastChapter);



    @Query("SELECT lastChapter FROM bookmarks WHERE mangaId = :mangaId LIMIT 1")
    String getLastChapter(String mangaId);



    @Query("SELECT * FROM bookmarks WHERE mangaId = :mangaId LIMIT 1")
    BookmarkEntity getBookmarkByMangaId (String mangaId);


}
