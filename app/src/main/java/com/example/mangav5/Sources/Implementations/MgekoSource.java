package com.example.mangav5.Sources.Implementations;

import android.content.Context;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.ServicesMangaWebsites.ServiceMgeko.MgekoFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceMgeko.MgekoSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceMgeko.MgekoChaptersService;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.ChapterModel;
import java.util.List;

public class MgekoSource implements MangaSource {

    @Override
    public String getSourceName() {
        return "Mgeko";
    }

    @Override
    public void fetchFeed(int page, MangaListCallback callback) {
        MgekoFeedService.getMangaFeedMgeko(page, new MgekoFeedService.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchDetails(Context context, String mangaUrl, MangaCallback callback) {
        MgekoFeedService.getMangaDetailsMgeko(mangaUrl, new MgekoFeedService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
            @Override
            public void onError(String errorMessage) { callback.onError(errorMessage); }
        });
    }

    @Override
    public void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback) {
        MgekoChaptersService.getChaptersMgeko(mangaUrl, new MgekoChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) { callback.onSuccess(chapters); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void fetchPages(Context context, String chapterUrl, PagesCallback callback) {
        MgekoChaptersService.getChapterMgeko(chapterUrl, new MgekoChaptersService.ChapterCallback() {
            @Override
            public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    @Override
    public void search(String query, MangaListCallback callback) {
        MgekoSearchService.search(query, new MgekoSearchService.MangaListCallBack() {
            @Override
            public void onSuccess(List<MangaItemModel> results) { callback.onSuccess(results); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }
}
