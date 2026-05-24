package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast.ManhuaFastFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast.ManhuaFastSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast.ManhuaFastChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausFeedService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class ManhuaFastSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "ManhuaFast";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        ManhuaFastFeedService.getMangaFeedManhuaFast(page, new ManhuaFastFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        ManhuaFastFeedService.getMangaDetailsManhuaFast(mangaUrl, new ManhuausFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        ManhuaFastChaptersService.getChaptersManhuaFast(context, mangaUrl, new ManhuaFastChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        ManhuaFastChaptersService.getChapterMangaManhuaFast(chapterUrl, new ManhuaFastChaptersService.ChapterCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        ManhuaFastSearchService.search(query, new ManhuaFastSearchService.MangaListCallBack() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }
}
