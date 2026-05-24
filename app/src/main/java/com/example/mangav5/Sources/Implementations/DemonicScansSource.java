package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.DemonicScansFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.DemonicScansSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.DemonicScansChaptersService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class DemonicScansSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "DemonicScans";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        DemonicScansFeedService.getMangaFeedDemonicScans(page, new DemonicScansFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        DemonicScansFeedService.getMangaDetailsDemonicScans(mangaUrl, new DemonicScansFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        DemonicScansChaptersService.getChaptersDemonicScans(mangaUrl, new DemonicScansChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        DemonicScansChaptersService.getChapterDemonicScans(chapterUrl, new DemonicScansChaptersService.ChapterCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        DemonicScansSearchService.search(query, new DemonicScansSearchService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }
}
