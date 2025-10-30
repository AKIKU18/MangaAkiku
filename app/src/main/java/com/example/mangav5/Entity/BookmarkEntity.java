package com.example.mangav5.Entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks")
public class BookmarkEntity {
    @PrimaryKey
    @NonNull
    public String mangaId;

    public String title;
    public String coverUrl;
    public String description;
    public String mangaUrl;
    public String source;

    public BookmarkEntity(String mangaId, String title, String coverUrl, String description, String mangaUrl,String source) {
        this.mangaId = mangaId;
        this.title = title;
        this.coverUrl = coverUrl;
        this.description = description;
        this.mangaUrl = mangaUrl;
        this.source = source;
    }

    public String getMangaId() {
        return mangaId;
    }

    public void setMangaId(String mangaId) {
        this.mangaId = mangaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getMangaUrl() {
        return mangaUrl;
    }

    public void setMangaUrl(String mangaUrl) {
        this.mangaUrl = mangaUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
