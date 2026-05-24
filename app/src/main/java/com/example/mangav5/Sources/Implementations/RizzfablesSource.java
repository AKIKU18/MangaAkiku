package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables.RizzfablesFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables.RizzfablesSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables.RizzfablesChaptersService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class RizzfablesSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "Rizzfables";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        RizzfablesFeedService.getMangaFeedRizzfables(new RizzfablesFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        RizzfablesFeedService.getMangaDetailsRizzfables(mangaUrl, new RizzfablesFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        RizzfablesChaptersService.getChaptersRizzfables(mangaUrl, new RizzfablesChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        RizzfablesChaptersService.getChapterRizzfables(chapterUrl, new RizzfablesChaptersService.ChapterCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        RizzfablesSearchService.search(query, new RizzfablesSearchService.MangaListCallBack() {
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
