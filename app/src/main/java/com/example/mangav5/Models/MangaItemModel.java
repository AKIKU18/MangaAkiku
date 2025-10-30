package com.example.mangav5.Models;

public class MangaItemModel {
    private String mangaId;
    private String title;
    private String description;
    private String coverImageUrl;
    private Boolean isBookmarked;
    private String mangaUrl;
    private String lastChapter;
    private String source;


    public MangaItemModel(){
    }

    public MangaItemModel(String mangaId, String title, String description, String coverImageUrl, Boolean isBookmarked, String mangaUrl,String lastChapter,String source) {
        this.mangaId = mangaId;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.isBookmarked = isBookmarked;
        this.mangaUrl = mangaUrl;
        this.lastChapter = lastChapter;
        this.source = source;

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
    public String getCoverImageUrl() {
        return coverImageUrl;
    }
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
    public Boolean getIsBookmarked() {
        return isBookmarked;
    }
    public void setIsBookmarked(Boolean isBookmarked) {
        this.isBookmarked = isBookmarked;
    }
    public String getMangaUrl() {
        return mangaUrl;
    }

    public void setMangaUrl(String mangaUrl) {
        this.mangaUrl = mangaUrl;
    }

    public String getLastChapter() {
        return lastChapter;
    }

    public void setLastChapter(String lastChapter) {
        this.lastChapter = lastChapter;
    }
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
