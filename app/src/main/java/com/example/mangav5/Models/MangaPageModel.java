package com.example.mangav5.Models;

import java.util.List;

public class MangaPageModel {
    private String mangaId;
    private String title;
    private String description;
    private String coverImageUrl;
    private Boolean isBookmarked;
    private List<ChapterListModel> chapters;
    public MangaPageModel(String mangaId, String title, String description, String coverImageUrl, Boolean isBookmarked, List<ChapterListModel> chapters) {
        this.mangaId = mangaId;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.isBookmarked = isBookmarked;
        this.chapters = chapters;
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

    public List<ChapterListModel> getChapters() {
        return chapters;
    }
    public void setChapters(List<ChapterListModel> chapters) {
        this.chapters = chapters;
    }

}
