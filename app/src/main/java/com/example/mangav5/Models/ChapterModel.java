package com.example.mangav5.Models;

import java.util.List;

public class ChapterModel {
    private String chapterId;
    private String title;
    private String number;

    public ChapterModel(String chapterId, String title, String number) {
        this.chapterId = chapterId;
        this.title = title;
        this.number = number;
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

}
