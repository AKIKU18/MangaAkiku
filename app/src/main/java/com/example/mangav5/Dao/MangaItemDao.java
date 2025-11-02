package com.example.mangav5.Dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Relations.MangaWithChapters;

import java.util.List;

@Dao
public interface MangaItemDao {
    // Fetch a manga with all its chapters
    @Transaction
    @Query("SELECT * FROM mangaItem WHERE mangaId = :id")
    MangaWithChapters getMangaWithChapters(String id);

    // Insert a manga
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertManga(MangaItemEntity manga);

    // Insert chapters
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapters(List<ChapterItemEntity> chapters);

    @Query("SELECT * FROM mangaItem WHERE mangaId = :id")
    MangaItemEntity getMangaById(String id);
    @Query("SELECT * FROM mangaItem WHERE mangaUrl = :mangaUrl")
    MangaItemEntity getMangaByMangaUrl(String mangaUrl);

    //Delete on manga
    @Delete
    void deleteManga(MangaItemEntity manga);

    //Delete all manga
    @Query("DELETE FROM mangaItem")
    void deleteAllManga();

    //Get all manga
    @Query("SELECT * FROM mangaItem")
    List<MangaItemEntity> getAllManga();
}
