package com.example.mangav5.ServiceMaster;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.mangav5.MainActivitys.ChapterPage;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesAsuraScans.AsuraScansChapterPages;
import com.example.mangav5.ServicesAsuraScans.AsuraScraperTask;
import com.example.mangav5.ServicesMangaDex.ChaptersService;
import com.example.mangav5.ServicesMangaDex.FeedMangaService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ServiceController {

    private static final String TAG = "ServiceController";

    public static void fetchMangaListController(String serviceFeed, int offsetOrPage, int limit, MangaListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }

        switch (serviceFeed) {
            case "MangaDex":
                FeedMangaService.fetchMangaList(offsetOrPage, limit, new FeedMangaService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
                break;

            case "AsuraScans":
                AsuraScraperTask.getAsuraScansMangaFeed(offsetOrPage, new AsuraScraperTask.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
                break;

            default:
                callback.onError("Unknown service feed: " + serviceFeed);
                Log.e(TAG, "Unknown service feed: " + serviceFeed);
                break;
        }
    }

    public static void fetchMangaDetails(String serviceFeed, String mangaUrlOrId, MangaCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }

        switch (serviceFeed) {
            case "MangaDex":
                FeedMangaService.fetchMangaById(mangaUrlOrId, new FeedMangaService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
                break;

            case "AsuraScans":
                AsuraScraperTask.getMangaInfoAsuraScans(mangaUrlOrId, new AsuraScraperTask.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
                break;

            default:
                callback.onError("Unknown service feed: " + serviceFeed);
                Log.e(TAG, "Unknown service feed: " + serviceFeed);
                break;
        }
    }

    public static void mangaGetDescription(String serviceFeed, String mangaId, String mangaUrl, DescriptionCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }

        switch (serviceFeed) {
            case "MangaDex":
                FeedMangaService.fetchMangaById(mangaId, new FeedMangaService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "AsuraScans":
                AsuraScraperTask.getMangaInfoAsuraScans(mangaUrl, new AsuraScraperTask.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());

                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                });
                break;

        }
    }

    public static void fetchChapterListController(String serviceFeed, String mangaId, String mangaUrl, int offset, int limit, String descAsc, ChapterListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }

        switch (serviceFeed) {
            case "MangaDex":
                ChaptersService.fetchAllChapters(mangaId, descAsc, offset, limit, new ChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
                break;
            case "AsuraScans":
                AsuraScraperTask.getMangaChaptersAsuraScans(mangaUrl, new AsuraScraperTask.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) {
                            Collections.reverse(chapters);
                        }
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
                break;
            default:
                callback.onError("Unknown service feed: " + serviceFeed);
                Log.e(TAG, "Unknown service feed chapter List: " + serviceFeed);
                break;
        }
    }

    public static void getChapterPages(Context context, String source, String chapterUrlId, PagesCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }
        switch (source) {
            case "AsuraScans":
                AsuraScansChapterPages scraper = new AsuraScansChapterPages();
                scraper.GetChapterPages(context, chapterUrlId, new AsuraScansChapterPages.PagesCallback() {
                    @Override
                    public void onSuccess(List<String> pages) {
                        Log.i(TAG, "COMIC: " + pages);

                        callback.onSuccess(pages);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
                break;
            case "MangaDex":
                Log.e(TAG, "ServiceController MangaDex: " + chapterUrlId);
                ChaptersService.fetchChapterPages(chapterUrlId, new ChaptersService.PagesCallback() {
                    @Override
                    public void onSuccess(List<String> pages) {

                        callback.onSuccess(pages);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
        }

    }

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);

        void onError(String message);
    }

    public interface PagesCallback {
        void onSuccess(List<String> chapters);

        void onError(String message);
    }


    // --- Callback Interfaces ---
    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);

        void onError(String message);
    }

    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);

        void onError(String errorMessage);
    }

    public interface DescriptionCallback {
        void onSuccess(String description);

        void onError(String errorMessage);
    }
}
