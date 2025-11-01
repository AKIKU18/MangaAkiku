package com.example.mangav5.ServiceMaster;

import android.content.Context;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesAsuraScans.AsuraScansChapterPagesService;
import com.example.mangav5.ServicesAsuraScans.AsuraScansFeedService;
import com.example.mangav5.ServicesAsuraScans.AsuraScansSearchService;
import com.example.mangav5.ServicesMangaDex.MangaDexChaptersService;
import com.example.mangav5.ServicesMangaDex.MangaDexFeedManga;
import com.example.mangav5.ServicesMangaDex.MangaDexSearchService;

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
                MangaDexFeedManga.fetchMangaList(offsetOrPage, limit, new MangaDexFeedManga.MangaListCallback() {
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
                AsuraScansFeedService.getAsuraScansMangaFeed(offsetOrPage, new AsuraScansFeedService.MangaListCallback() {
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
                MangaDexFeedManga.fetchMangaById(mangaUrlOrId, new MangaDexFeedManga.MangaCallback() {
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
                AsuraScansFeedService.getMangaInfoAsuraScans(mangaUrlOrId, new AsuraScansFeedService.MangaCallback() {
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
                MangaDexFeedManga.fetchMangaById(mangaId, new MangaDexFeedManga.MangaCallback() {
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
                AsuraScansFeedService.getMangaInfoAsuraScans(mangaUrl, new AsuraScansFeedService.MangaCallback() {
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

    public static String getMangaIdOrMangaUrl(String source, String mangaId, String mangaUrl) {
        final String mangaUrlOrIdFinal;
        switch (source) {
            case "MangaDex":
                mangaUrlOrIdFinal = mangaId;
                break;
            case "AsuraScans":
                mangaUrlOrIdFinal = mangaUrl;
                break;
            default:
                return "";

        }
        return mangaUrlOrIdFinal;
    }

    public static String getChapterIdOrChapterUrl(String source, String chapterId, String chapterUrl) {
        final String chapterIdOrUrlFinal;

        switch (source) {
            case "MangaDex":
                chapterIdOrUrlFinal = chapterId;
                break;
            case "AsuraScans":
                chapterIdOrUrlFinal = chapterUrl;
                break;
            default:
                return "";

        }
        return chapterIdOrUrlFinal;
    }

    public static void fetchChapterListController(String serviceFeed, String mangaUrlOrId, int offset, int limit, String descAsc, ChapterListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }

        switch (serviceFeed) {
            case "MangaDex":
                MangaDexChaptersService.fetchAllChapters(mangaUrlOrId, descAsc, offset, limit, new MangaDexChaptersService.ChapterListCallback() {
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
                AsuraScansFeedService.getMangaChaptersAsuraScans(mangaUrlOrId, new AsuraScansFeedService.ChapterListCallback() {
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

    public static void getMangaItem(String source, String mangaUrlId, MangaCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }
        switch (source) {
            case "MangaDex":
                MangaDexFeedManga.fetchMangaById(mangaUrlId, new MangaDexFeedManga.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        onError(errorMessage);
                    }
                });
                break;
            case "AsuraScans":
                AsuraScansFeedService.getMangaInfoAsuraScans(mangaUrlId, new AsuraScansFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        onError(errorMessage);
                    }
                });
        }
    }

    public static void getChapterPages(Context context, String source, String chapterUrlId, PagesCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }
        switch (source) {
            case "AsuraScans":
                AsuraScansChapterPagesService scraper = new AsuraScansChapterPagesService();
                scraper.GetChapterPages(context, chapterUrlId, new AsuraScansChapterPagesService.PagesCallback() {
                    @Override
                    public void onSuccess(List<String> pages) {
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
                MangaDexChaptersService.fetchChapterPages(chapterUrlId, new MangaDexChaptersService.PagesCallback() {
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

    public static void fetchSearchMangas(String query, String source, MangaListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Callback is null!");
            return;
        }

        switch (source) {
            case "MangaDex":
                MangaDexSearchService.searchManga(query.trim(), 0, 50, new MangaDexSearchService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("HomePageSearch", "Error: " + message);
                    }
                });
                break;
            case "AsuraScans":
                AsuraScansSearchService.search(query, new AsuraScansSearchService.SearchCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {

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
