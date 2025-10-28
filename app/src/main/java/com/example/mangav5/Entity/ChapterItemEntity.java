package com.example.mangav5.Entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chapterItem")
public class ChapterItemEntity {
    @PrimaryKey
    @NonNull
    public String chapterId;
    public String mangaId;
    public String title;
    public String number;
    public String chapterUrl;


    public ChapterItemEntity(String chapterId, String mangaId, String title, String number, String chapterUrl) {
        this.chapterId = chapterId;
        this.mangaId = mangaId;
        this.title = title;
        this.number = number;
        this.chapterUrl = chapterUrl;
    }

    // Getters and setters for mangaId, chapterId, title, and number

    public String getMangaId() {
        return mangaId;
    }
    public void setMangaId(String mangaId) {
        this.mangaId = mangaId;
    }

    public String getChapterId() {
        return chapterId;
    }
    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getNumber() {
        return number;
    }
    public void setNumber(String number) {
        this.number = number;
    }
    public String getChapterUrl() {
        return chapterUrl;
    }

    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }
}
