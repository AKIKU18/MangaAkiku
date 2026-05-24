package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusChaptersService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class ManhuaPlusSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "ManhuaPlus";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        ManhuaPlusFeedService.getMangaFeedManhuaPlus(page, new ManhuaPlusFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        ManhuaPlusFeedService.getMangaDetailsManhuaPlus(mangaUrl, new ManhuaPlusFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        ManhuaPlusChaptersService.getChaptersManhuaPlus(mangaUrl, new ManhuaPlusChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        ManhuaPlusChaptersService.getChapterMangaManhuaPlus(context, chapterUrl, new ManhuaPlusChaptersService.ChapterCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        ManhuaPlusSearchService.search(query, new ManhuaPlusSearchService.MangaListCallBack() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }
}
