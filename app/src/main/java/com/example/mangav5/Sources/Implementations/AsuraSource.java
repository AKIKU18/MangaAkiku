package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans.AsuraScansFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans.AsuraScansSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans.AsuraScansChapterPagesService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class AsuraSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "AsuraScans";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        AsuraScansFeedService.getAsuraScansMangaFeed(page, new AsuraScansFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        AsuraScansFeedService.getMangaInfoAsuraScans(mangaUrl, new AsuraScansFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        AsuraScansFeedService.getMangaChaptersAsuraScans(mangaUrl, new AsuraScansFeedService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        new AsuraScansChapterPagesService().GetChapterPages(context, chapterUrl, new AsuraScansChapterPagesService.PagesCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        AsuraScansSearchService.search(query, new AsuraScansSearchService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }
}
