package com.example.mangav5.ServiceMaster;

import android.content.Context;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServiceManhuaPlus.ManhuaPlusChaptersService;
import com.example.mangav5.ServiceManhuaPlus.ManhuaPlusFeedService;
import com.example.mangav5.ServiceManhuaPlus.ManhuaPlusSearchService;
import com.example.mangav5.ServiceManhuas.ManhuausChaptersService;
import com.example.mangav5.ServiceManhuas.ManhuausFeedService;
import com.example.mangav5.ServiceManhuas.ManhuausSearchService;
import com.example.mangav5.ServicesAsuraScans.AsuraScansChapterPagesService;
import com.example.mangav5.ServicesAsuraScans.AsuraScansFeedService;
import com.example.mangav5.ServicesAsuraScans.AsuraScansSearchService;
import com.example.mangav5.ServicesMangaDex.MangaDexChaptersService;
import com.example.mangav5.ServicesMangaDex.MangaDexFeedManga;
import com.example.mangav5.ServicesMangaDex.MangaDexSearchService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceController {
        /*
    ServiceController.java

    HOW TO ADD A NEW MANGA SOURCE:

    1. Create new service classes for your source:
       - Feed Service (fetches manga lists, e.g., `NewSourceFeedService`)
       - Manga Details Service (fetches single manga info)
       - Chapters Service (fetches chapters for a manga)
       - Search Service (optional, if search is supported)

    2. Implement appropriate callback interfaces in each service:
       - MangaListCallback: returns a List<MangaItemModel>
       - MangaCallback: returns a single MangaItemModel
       - ChapterListCallback: returns a List<ChapterModel>
       - PagesCallback: returns a List<String> for chapter pages

    3. Update ServiceController methods to handle the new source:
       - fetchMangaListController(): add a new case for the source and call the new feed service
       - fetchMangaDetails(): add a new case for the source and call the new manga details service
       - mangaGetDescription(): add a new case to fetch the description from your service
       - fetchChapterListController(): add a new case to fetch chapters
       - getChapterPages(): add a new case to fetch chapter pages
       - fetchSearchMangas(): add a new case if your source supports search
       - getMangaItem(): add a new case for fetching a single manga item

    4. Update getMangaIdOrMangaUrl() and getChapterIdOrChapterUrl():
       - Define whether your source uses manga ID or manga URL for identification
       - Same for chapters

    5. Optional:
       - Logging: add descriptive log messages for easier debugging
       - Error handling: ensure callbacks handle onError properly

    Example case for a new source:
        case "NewSource":
            NewSourceFeedService.fetchMangaList(offsetOrPage, limit, new NewSourceFeedService.MangaListCallback() {
                @Override
                public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
                @Override
                public void onError(String message) { callback.onError(message); }
            });
            break;

    */


    private static final String TAG = "ServiceController";

    public static void fetchMangaListController(String serviceFeed, int offsetOrPage, int limit, MangaListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchMangaListController] Callback is null for serviceFeed: " + serviceFeed);
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
                        Log.e(TAG, "[fetchMangaListController:MangaDex] Error fetching manga list at offset " + offsetOrPage + ": " + message);
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
                        Log.e(TAG, "[fetchMangaListController:AsuraScans] Error fetching manga list at page " + offsetOrPage + ": " + message);
                        callback.onError(message);
                    }
                });
                break;

            case "Manhuaus":
                ManhuausFeedService.getMangaFeedManhuaus(new ManhuausFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:Manhuaus] Error fetching manga list: " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "ManhuaPlus":
                ManhuaPlusFeedService.getMangaFeedManhuaPlus(offsetOrPage, new ManhuaPlusFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:ManhuaPlus] Error fetching manga list: " + message);
                        callback.onError(message);
                    }
                });
                break;

            default:
                Log.e(TAG, "[fetchMangaListController] Unknown service feed: " + serviceFeed);
                callback.onError("Unknown service feed: FetchMangaList " + serviceFeed);
                break;
        }
    }

    public static void fetchMangaDetails(String serviceFeed, String mangaUrlOrId, MangaCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchMangaDetails] Callback is null for serviceFeed: " + serviceFeed);
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
                        Log.e(TAG, "[fetchMangaDetails:MangaDex] Error fetching manga ID " + mangaUrlOrId + ": " + message);
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
                        Log.e(TAG, "[fetchMangaDetails:AsuraScans] Error fetching manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;

            case "Manhuaus":
                ManhuausFeedService.getMangaDetailsManhuaus(mangaUrlOrId, new ManhuausFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaDetails:Manhuaus] Error fetching manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "ManhuaPlus":
                ManhuaPlusFeedService.getMangaDetailsManhuaPlus(mangaUrlOrId, new ManhuaPlusFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[fetchMangaDetails:ManhuaPlus] Error fetching manga URL " + mangaUrlOrId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;

            default:
                Log.e(TAG, "[fetchMangaDetails] Unknown service feed: " + serviceFeed);
                callback.onError("Unknown service feed: fetchMangaDetail " + serviceFeed);
                break;
        }
    }

    public static void mangaGetDescription(String serviceFeed, String mangaId, String mangaUrl, DescriptionCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[mangaGetDescription] Callback is null for serviceFeed: " + serviceFeed);
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
                        Log.e(TAG, "[mangaGetDescription:MangaDex] Error fetching description for manga ID " + mangaId + ": " + errorMessage);
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
                        Log.e(TAG, "[mangaGetDescription:AsuraScans] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;

            case "Manhuaus":
                ManhuausFeedService.getMangaDetailsManhuaus(mangaUrl, new ManhuausFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[mangaGetDescription:Manhuaus] Error fetching description for manga URL " + mangaUrl + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "ManhuaPlus":
                ManhuaPlusFeedService.getMangaDetailsManhuaPlus(mangaUrl, new ManhuaPlusFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[mangaGetDescription:ManhuaPlus] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        Log.e(TAG,"[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;

            default:
                Log.e(TAG, "[mangaGetDescription] Unknown service feed: " + serviceFeed);
                callback.onError("Unknown service feed:MangaGetDescription " + serviceFeed);
                break;
        }
    }

    public static String getMangaIdOrMangaUrl(String source, String mangaId, String mangaUrl) {
        switch (source) {
            case "MangaDex":
                return mangaId;
            case "AsuraScans":
                return mangaUrl;
            case "Manhuaus":
                return mangaUrl;
            case "ManhuaPlus":
                return mangaUrl;
            default:
                Log.e(TAG, "[getMangaIdOrMangaUrl] Unknown source:getMangaIdOrUrl " + source);
                return "";
        }
    }


    public static String getChapterIdOrChapterUrl(String source, String chapterId, String chapterUrl) {
        switch (source) {
            case "MangaDex":
                return chapterId;
            case "AsuraScans":
                return chapterUrl;
            case "Manhuaus":
                return chapterUrl;
            case "ManhuaPlus":
                return chapterUrl;
            default:
                Log.e(TAG, "[getChapterIdOrChapterUrl] Unknown source:getChapterIdOrUrl " + source);
                return "";
        }
    }

    public static void fetchChapterListController(String serviceFeed, String mangaUrlOrId, int offset, int limit, String descAsc, ChapterListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchChapterListController] Callback is null for serviceFeed: " + serviceFeed);
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
                        Log.e(TAG, "[fetchChapterListController:MangaDex] Error fetching chapters for manga ID " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;

            case "AsuraScans":
                AsuraScansFeedService.getMangaChaptersAsuraScans(mangaUrlOrId, new AsuraScansFeedService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:AsuraScans] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;

            case "Manhuaus":
                ManhuausChaptersService.getChaptersManhuaus(mangaUrlOrId, new ManhuausChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:Manhuaus] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "ManhuaPlus":
                ManhuaPlusChaptersService.getChaptersManhuaPlus(mangaUrlOrId, new ManhuaPlusChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:ManhuaPlus] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;

            default:
                Log.e(TAG, "[fetchChapterListController] Unknown service feed:getChapters " + serviceFeed);
                callback.onError("Unknown service feed: " + serviceFeed);
                break;
        }
    }

    public static void getMangaItem(String source, String mangaUrlId, MangaCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[getMangaItem] Callback is null for source: " + source);
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
                        Log.e(TAG, "[getMangaItem:MangaDex] Error for manga ID " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
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
                        Log.e(TAG, "[getMangaItem:AsuraScans] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;

            case "Manhuaus":
                ManhuausFeedService.getMangaDetailsManhuaus(mangaUrlId, new ManhuausFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:Manhuaus] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "ManhuaPlus":
                ManhuaPlusFeedService.getMangaDetailsManhuaPlus(mangaUrlId, new ManhuaPlusFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:ManhuaPlus] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;

            default:
                Log.e(TAG, "[getMangaItem] Unknown source: " + source);
                callback.onError("Unknown source:getMangaItem " + source);
                break;
        }
    }

    public static void getChapterPages(Context context, String source, String chapterUrlId, PagesCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[getChapterPages] Callback is null for source: " + source);
            return;
        }

        switch (source) {
            case "AsuraScans":
                new AsuraScansChapterPagesService().GetChapterPages(context, chapterUrlId, new AsuraScansChapterPagesService.PagesCallback() {
                    @Override
                    public void onSuccess(List<String> pages) {
                        callback.onSuccess(pages);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:AsuraScans] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;

            case "MangaDex":
                MangaDexChaptersService.fetchChapterPages(chapterUrlId, new MangaDexChaptersService.PagesCallback() {
                    @Override
                    public void onSuccess(List<String> pages) {
                        callback.onSuccess(pages);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:MangaDex] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;

            case "Manhuaus":
                ManhuausChaptersService.getChapterMangaManhuaus(chapterUrlId, new ManhuausChaptersService.ChapterCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {
                        callback.onSuccess(chapter);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:Manhuaus] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "ManhuaPlus":
                ManhuaPlusChaptersService.getChapterMangaManhuaPlus(context, chapterUrlId, new ManhuaPlusChaptersService.ChapterCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {

                        callback.onSuccess(chapter);
                    }


                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:ManhuaPlus] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;

            default:
                Log.e(TAG, "[getChapterPages] Unknown source:getChapterPages " + source);
                callback.onError("Unknown source: " + source);
                break;
        }
    }

    public static void searchThroughAllSources(String query, MangaListCallback callback) {
        List<String> sources = List.of("MangaDex", "AsuraScans", "Manhuaus", "ManhuaPlus");
        List<MangaItemModel> allResults = new ArrayList<>();
        int[] completed = {0}; // simple counter for async completion

        for (String source : sources) {
            fetchSearchMangas(query, source, new MangaListCallback() {
                @Override
                public void onSuccess(List<MangaItemModel> results) {
                    allResults.addAll(results);
                    completed[0]++;
                    if (completed[0] == sources.size()) {
                        callback.onSuccess(allResults); // called once after all sources complete
                    }
                }

                @Override
                public void onError(String message) {
                    completed[0]++;
                    if (completed[0] == sources.size()) {
                        callback.onSuccess(allResults); // still call success even if some fail
                    }
                }
            });
        }
    }

    public static void fetchSearchMangas(String query, String source, MangaListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchSearchMangas] Callback is null for source: " + source);
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
                        Log.e(TAG, "[fetchSearchMangas:MangaDex] Error searching query '" + query + "': " + message);
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
                        Log.e(TAG, "[fetchSearchMangas:AsuraScans] Error searching query '" + query + "': " + error);
                    }
                });
                break;

            case "Manhuaus":
                ManhuausSearchService.search(query, new ManhuausSearchService.SearchCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:Manhuaus] Error searching query '" + query + "': " + error);
                    }
                });
                break;
            case "ManhuaPlus":
                ManhuaPlusSearchService.search(query, new ManhuaPlusSearchService.SearchCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:ManhuaPlus] Error searching query '" + query + "': " + error);
                    }
                });
                break;

            default:
                Log.e(TAG, "[fetchSearchMangas] Unknown source:fetchSearchManga " + source);
                callback.onError("Unknown source: " + source);
                break;
        }
    }


    // --- Callback Interfaces ---
    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);

        void onError(String message);
    }

    public interface PagesCallback {
        void onSuccess(List<String> chapters);

        void onError(String message);
    }

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
