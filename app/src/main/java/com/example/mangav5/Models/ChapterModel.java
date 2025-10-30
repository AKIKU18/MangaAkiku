package com.example.mangav5.Models;

public class ChapterModel {
    private String chapterId;
    private String title;
    private String number;
    private String chapterUrl;
    private String source;

    public ChapterModel(String chapterId, String title, String number, String chapterUrl,String source) {
        this.chapterId = chapterId;
        this.title = title;
        this.number = number;
        this.chapterUrl = chapterUrl;
        this.source = source;
    }

    // Getters and setters for chapterId, title, number, coverImageUrl, and pages
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
