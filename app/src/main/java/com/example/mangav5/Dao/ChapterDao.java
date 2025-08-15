package com.example.mangav5.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Models.ChapterModel;

import java.util.List;

@Dao
public interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapters(List<ChapterItemEntity> chapters);
    @Query("SELECT * FROM chapterItem WHERE mangaId = :mangaId ORDER BY CAST(number AS REAL) DESC")
    List<ChapterItemEntity> getChaptersByMangaId(String mangaId);

    @Query("SELECT * FROM chapterItem WHERE chapterId = :chapterId")
    ChapterItemEntity getChapterById(String chapterId);

    // or just check existence
    @Query("SELECT EXISTS(SELECT 1 FROM chapterItem WHERE mangaId = :mangaId)")
    boolean hasChapters(String mangaId);

    @Query("DELETE FROM chapterItem WHERE chapterId = :chapterId")
    void deleteChapterById(String chapterId);
}