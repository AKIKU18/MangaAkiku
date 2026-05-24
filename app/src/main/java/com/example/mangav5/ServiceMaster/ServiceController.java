package com.example.mangav5.ServiceMaster;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServiceComix.ComixChapterListService;
import com.example.mangav5.ServicesMangaWebsites.ServiceComix.ComixChapterPagesService;
import com.example.mangav5.ServicesMangaWebsites.ServiceComix.ComixFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceComix.ComixSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.DemonicScansChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.DemonicScansFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.DemonicScansSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.demonicScansTest;
import com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics.FlameComicsChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics.FlameComicsFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics.FlameComicsSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast.ManhuaFastChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast.ManhuaFastFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast.ManhuaFastSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceMgeko.MgekoChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServiceMgeko.MgekoFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceMgeko.MgekoSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables.RizzfablesChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables.RizzfablesFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables.RizzfablesSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans.AsuraScansChapterPagesService;
import com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans.AsuraScansFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans.AsuraScansSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServicesMangaDex.MangaDexChaptersService;
import com.example.mangav5.ServicesMangaWebsites.ServicesMangaDex.MangaDexFeedManga;
import com.example.mangav5.ServicesMangaWebsites.ServicesMangaDex.MangaDexSearchService;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansChaptersService;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansFeedService;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansSearchService;

import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.Sources.SourceManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

        MangaSource source = SourceManager.getInstance().getSource(serviceFeed);
        if (source != null) {
            source.fetchFeed(offsetOrPage, new MangaSource.MangaListCallback() {
                @Override
                public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
                @Override
                public void onError(String message) { callback.onError(message); }
            });
        } else {
            Log.e(TAG, "[fetchMangaListController] Unknown source: " + serviceFeed);
            callback.onError("Unknown source: " + serviceFeed);
        }
    }

    public static void fetchMangaDetails(Context context, String serviceFeed, String mangaUrlOrId, MangaCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchMangaDetails] Callback is null for serviceFeed: " + serviceFeed);
            return;
        }

        MangaSource source = SourceManager.getInstance().getSource(serviceFeed);
        if (source != null) {
            source.fetchDetails(context, mangaUrlOrId, new MangaSource.MangaCallback() {
                @Override
                public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
                @Override
                public void onError(String message) { callback.onError(message); }
            });
        } else {
            Log.e(TAG, "[fetchMangaDetails] Unknown source: " + serviceFeed);
            callback.onError("Unknown source: " + serviceFeed);
        }
    }

    public static void mangaGetDescription(Context context, String serviceFeed, String mangaId, String mangaUrl, DescriptionCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[mangaGetDescription] Callback is null for serviceFeed: " + serviceFeed);
            return;
        }

        MangaSource source = SourceManager.getInstance().getSource(serviceFeed);
        if (source != null) {
            String targetUrlId = source.useUrlAsId() ? mangaUrl : mangaId;
            source.fetchDetails(context, targetUrlId, new MangaSource.MangaCallback() {
                @Override
                public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga.getDescription()); }
                @Override
                public void onError(String message) { callback.onError(message); }
            });
        } else {
            Log.e(TAG, "[mangaGetDescription] Unknown source: " + serviceFeed);
            callback.onError("Unknown source: " + serviceFeed);
        }
    }

    public static String getMangaIdOrMangaUrl(String source, String mangaId, String mangaUrl) {
        MangaSource mangaSource = SourceManager.getInstance().getSource(source);
        if (mangaSource != null) {
            return mangaSource.useUrlAsId() ? mangaUrl : mangaId;
        }
        Log.e(TAG, "[getMangaIdOrMangaUrl] Unknown source: " + source);
        return "";
    }


    public static String getChapterIdOrChapterUrl(String source, String chapterId, String chapterUrl) {
        MangaSource mangaSource = SourceManager.getInstance().getSource(source);
        if (mangaSource != null) {
            return mangaSource.useUrlAsChapterId() ? chapterUrl : chapterId;
        }
        Log.e(TAG, "[getChapterIdOrChapterUrl] Unknown source: " + source);
        return "";
    }

    public static void fetchChapterListController(Context context, String serviceFeed, String mangaUrlOrId, int offset, int limit, String descAsc, ChapterListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchChapterListController] Callback is null for serviceFeed: " + serviceFeed);
            return;
        }

        MangaSource source = SourceManager.getInstance().getSource(serviceFeed);
        if (source != null) {
            source.fetchChapters(context, mangaUrlOrId, new MangaSource.ChapterListCallback() {
                @Override
                public void onSuccess(List<ChapterModel> chapters) {
                    if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                    callback.onSuccess(chapters);
                }
                @Override
                public void onError(String message) { callback.onError(message); }
            });
        } else {
            Log.e(TAG, "[fetchChapterListController] Unknown source: " + serviceFeed);
            callback.onError("Unknown source: " + serviceFeed);
        }
    }

    public static void getMangaItem(Context context, String source, String mangaUrlId, MangaCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[getMangaItem] Callback is null for source: " + source);
            return;
        }

        MangaSource mangaSource = SourceManager.getInstance().getSource(source);
        if (mangaSource != null) {
            mangaSource.fetchDetails(context, mangaUrlId, new MangaSource.MangaCallback() {
                @Override
                public void onSuccess(MangaItemModel manga) { callback.onSuccess(manga); }
                @Override
                public void onError(String message) { callback.onError(message); }
            });
        } else {
            Log.e(TAG, "[getMangaItem] Unknown source: " + source);
            callback.onError("Unknown source: " + source);
        }
    }

    public static void getChapterPages(Context context, String source, String chapterUrlId, PagesCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[getChapterPages] Callback is null for source: " + source);
            return;
        }

        MangaSource mangaSource = SourceManager.getInstance().getSource(source);
        if (mangaSource != null) {
            mangaSource.fetchPages(context, chapterUrlId, new MangaSource.PagesCallback() {
                @Override
                public void onSuccess(List<String> pages) { callback.onSuccess(pages); }
                @Override
                public void onError(String message) { callback.onError(message); }
            });
        } else {
            Log.e(TAG, "[getChapterPages] Unknown source: " + source);
            callback.onError("Unknown source: " + source);
        }
    }

    public static void searchThroughAllSources(String query, MangaListCallback callback) {
        Set<String> sources = SourceManager.getInstance().getAvailableSources();
        List<MangaItemModel> allResults = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completed = new AtomicInteger(0);
        int totalSources = sources.size();

        for (String sourceName : sources) {
            fetchSearchMangas(query, sourceName, new ServiceController.MangaListCallback() {
                private boolean called = false; // ensure only once

                @Override
                public void onSuccess(List<MangaItemModel> results) {
                    if (!called) {
                        called = true;
                        if (results != null && !results.isEmpty()) allResults.addAll(results);
                        checkCompletion();
                    }
                }

                @Override
                public void onError(String message) {
                    if (!called) {
                        called = true;
                        Log.e("SearchAllSources", "[" + sourceName + "] " + message);
                        checkCompletion();
                    }
                }

                private void checkCompletion() {
                    if (completed.incrementAndGet() == totalSources) {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> {
                            if (allResults.isEmpty()) {
                                callback.onError("No results found.");
                            } else {
                                callback.onSuccess(new ArrayList<>(allResults));
                                Log.d("SearchAllSources", "Total results: " + allResults.size());
                            }
                        });
                    }
                }
            });

            // Failsafe timeout
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (completed.get() < totalSources) {
                    completed.incrementAndGet();
                    Log.e("SearchAllSources", "[" + sourceName + "] timed out");
                    if (completed.get() == totalSources) {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> {
                            if (allResults.isEmpty()) callback.onError("No results found.");
                            else callback.onSuccess(new ArrayList<>(allResults));
                        });
                    }
                }
            }, 5000);
        }
    }

    public static void fetchSearchMangas(String query, String source, MangaListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchSearchMangas] Callback is null for source: " + source);
            return;
        }

        MangaSource mangaSource = SourceManager.getInstance().getSource(source);
        if (mangaSource != null) {
            mangaSource.search(query, new MangaSource.MangaListCallback() {
                @Override
                public void onSuccess(List<MangaItemModel> mangas) { callback.onSuccess(mangas); }
                @Override
                public void onError(String message) { callback.onError(message); }
            });
        } else {
            Log.e(TAG, "[fetchSearchMangas] Unknown source: " + source);
            callback.onError("Unknown source: " + source);
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

    public interface LastChapterTitleCallback {
        void onSucces(String lastChapterTitle);

        void onError(String errorMessage);
    }
}
