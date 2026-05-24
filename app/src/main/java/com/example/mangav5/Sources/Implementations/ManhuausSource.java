package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausChaptersService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class ManhuausSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "Manhuaus";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        // Manhuaus doesn't seem to take a page in its current static method?
        // Let's check ManhuausFeedService again.
        ManhuausFeedService.getMangaFeedManhuaus(new ManhuausFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        ManhuausFeedService.getMangaDetailsManhuaus(mangaUrl, new ManhuausFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        ManhuausChaptersService.getChaptersManhuaus(mangaUrl, new ManhuausChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        ManhuausChaptersService.getChapterMangaManhuaus(chapterUrl, new ManhuausChaptersService.ChapterCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        ManhuausSearchService.search(query, new ManhuausSearchService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }

    @Override
    public boolean isPageBased() {
        return false; // Manhuaus current impl doesn't show page support in static call
    }
}
