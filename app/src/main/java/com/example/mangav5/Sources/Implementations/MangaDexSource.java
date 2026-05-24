package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServicesMangaDex.MangaDexFeedManga;
import com.example.mangav5.ServicesMangaWebsites.ServicesMangaDex.MangaDexSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServicesMangaDex.MangaDexChaptersService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class MangaDexSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "MangaDex";
    }

    @Override
    public void fetchFeed(int offset, MangaListCallback callback) {
        MangaDexFeedManga.fetchMangaList(offset, 10, new MangaDexFeedManga.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaId, MangaCallback callback) {
        MangaDexFeedManga.fetchMangaById(mangaId, new MangaDexFeedManga.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaId, ChapterListCallback callback) {
        MangaDexChaptersService.fetchAllChapters(mangaId, "desc", 0, 100, new MangaDexChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterId, PagesCallback callback) {
        MangaDexChaptersService.fetchChapterPages(chapterId, new MangaDexChaptersService.PagesCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        MangaDexSearchService.searchManga(query, 0, 50, new MangaDexSearchService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public boolean useUrlAsId() {
        return false;
    }

    @Override
    public boolean useUrlAsChapterId() {
        return false;
    }

    @Override
    public boolean isPageBased() {
        return false; // Uses Offset
    }

    @Override
    public int getStartingPage() {
        return 0; // Starts from 0
    }
}
