package com.example.mangav5.Models;

import java.util.List;

public class ChapterListModel {
    private List<ChapterModel> chapters;

    public ChapterListModel(List<ChapterModel> chapters) {
        this.chapters = chapters;
    }

    public List<ChapterModel> getChapters() {
        return chapters;
    }

    public void setChapters(List<ChapterModel> chapters) {
        this.chapters = chapters;
    }
}
