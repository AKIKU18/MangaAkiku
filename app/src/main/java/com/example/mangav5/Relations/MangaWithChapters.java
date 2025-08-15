package com.example.mangav5.Relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.MangaItemEntity;

import java.util.List;

public class MangaWithChapters {
    @Embedded
    public MangaItemEntity manga;

    @Relation(
            parentColumn = "mangaId",
            entityColumn = "mangaId"
    )
    public List<ChapterItemEntity> chapters;
}
