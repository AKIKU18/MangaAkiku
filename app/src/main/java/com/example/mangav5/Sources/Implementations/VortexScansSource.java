package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansFeedService;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansSearchService;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansChaptersService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class VortexScansSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "VortexScans";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        VortexScansFeedService.getMangaFeedVortexScans(page, new VortexScansFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        VortexScansFeedService.getMangaDetailsVortexScans(mangaUrl, new VortexScansFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        VortexScansChaptersService.getChaptersVortexScans(mangaUrl, new VortexScansChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        VortexScansChaptersService.getChapterVortexScans(chapterUrl, new VortexScansChaptersService.ChapterCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        VortexScansSearchService.search(query, new VortexScansSearchService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }
}
