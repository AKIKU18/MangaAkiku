package com.example.mangav5.Entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int id; // unique ID for each record
    @NonNull
    public String mangaId;
    public String chapterId;
    public String chapterTitle;

    public String coverUrl;
    public String description;
    public long timestamp;
    public String mangaTitle;
    public String mangaUrl;
    public String chapterUrl;
    public String source;
    public int scrollPosition;


    public HistoryEntity(String mangaId, String chapterId, String chapterTitle, String coverUrl, String description, long timestamp, String mangaTitle, String mangaUrl, String chapterUrl, String source, int scrollPosition) {
        this.mangaId = mangaId;
        this.chapterId = chapterId;
        this.chapterTitle = chapterTitle;
        this.coverUrl = coverUrl;
        this.description = description;
        this.timestamp = timestamp;
        this.mangaTitle = mangaTitle;
        this.mangaUrl = mangaUrl;
        this.chapterUrl = chapterUrl;
        this.source = source;
        this.scrollPosition = scrollPosition;
    }

    // Getters and setters for mangaId, title, coverUrl, description, and timestamp

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

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMangaTitle() {
        return mangaTitle;
    }

    public void setMangaTitle(String mangaTitle) {
        this.mangaTitle = mangaTitle;
    }

    public String getMangaUrl() {
        return mangaUrl;
    }

    public void setMangaUrl(String mangaUrl) {
        this.mangaUrl = mangaUrl;
    }

    public String getChapterUrl() {
        return chapterUrl;
    }


    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getScrollPosition() {
        return scrollPosition;
    }

    public void setScrollPosition(int scrollPosition) {
        this.scrollPosition = scrollPosition;
    }
}
