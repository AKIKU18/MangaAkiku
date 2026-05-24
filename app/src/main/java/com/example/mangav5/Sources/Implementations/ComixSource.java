package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServiceComix.ComixFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceComix.ComixSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceComix.ComixChapterListService;
import com.example.mangav5.ServicesMangaWebsites.ServiceComix.ComixChapterPagesService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class ComixSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "Comix";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        ComixFeedService.getMangaFeedComix(page, new ComixFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        ComixFeedService.getMangaDetailsComix(context, mangaUrl, new ComixFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        new ComixChapterListService().getChapterList(context, mangaUrl, new ComixChapterListService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        new ComixChapterPagesService().getChapterPages(context, chapterUrl, new ComixChapterPagesService.PagesCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        ComixSearchService.search(query, new ComixSearchService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }
}
