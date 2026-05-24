package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics.FlameComicsFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics.FlameComicsSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics.FlameComicsChaptersService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class FlameComicsSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "FlameComics";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        FlameComicsFeedService.getMangaFeedFlameComics(new FlameComicsFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        FlameComicsFeedService.getMangaDetailsFlameComics(mangaUrl, new FlameComicsFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        FlameComicsChaptersService.getChaptersFlameComics(mangaUrl, new FlameComicsChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        FlameComicsChaptersService.getChapterFlameComics(chapterUrl, new FlameComicsChaptersService.ChapterCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        FlameComicsSearchService.search(query, new FlameComicsSearchService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }

    @Override
    public boolean isPageBased() {
        return false;
    }
}
