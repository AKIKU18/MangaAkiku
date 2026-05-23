package com.example.mangav5.Models;

import java.util.List;

public class NotificationModel {
    public String mangaId;

    public String title;
    public String coverUrl;
    public String description;
    public String mangaUrl;
    public String source;
    public String lastChapter;
    private Boolean hasNewChapter;
    public NotificationModel(String mangaId, String title, String description, String coverUrl,  Boolean hasNewChapter) {
        this.mangaId = mangaId;
        this.title = title;
        this.description = description;
        this.coverUrl = coverUrl;
        this.hasNewChapter = hasNewChapter;
    }

    // Getters and setters for mangaId, title, description, and coverImageUrl
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
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCoverUrl() {
        return coverUrl;
    }
    public void setCoverImageUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Boolean getHasNewChapter() {
        return hasNewChapter;
    }
    public void setHasNewChapter(Boolean hasNewChapter) {
        this.hasNewChapter = hasNewChapter;
    }

}
