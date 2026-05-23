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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            case "DemonicScans":
                DemonicScansFeedService.getMangaFeedDemonicScans(offsetOrPage, new DemonicScansFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:DemonicScans] Error fetching manga list: " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "ManhuaFast":
                ManhuaFastFeedService.getMangaFeedManhuaFast(offsetOrPage, new ManhuaFastFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:ManhuaFast] Error fetching manga list: " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "FlameComics":
                FlameComicsFeedService.getMangaFeedFlameComics(new FlameComicsFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:FlameComics] Error fetching manga list: " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "Rizzfables":
                RizzfablesFeedService.getMangaFeedRizzfables(new RizzfablesFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:Rizzfables] Error fetching manga list: " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "Mgeko":
                MgekoFeedService.getMangaFeedMgeko(offsetOrPage, new MgekoFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:Mgeko] Error fetching manga list: " + message);
                        callback.onError(message);
                    }
                });
                break;

            case "Comix":
                ComixFeedService.getMangaFeedComix(offsetOrPage, new ComixFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:Comix] Error fetching manga list: " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "VortexScans":
                VortexScansFeedService.getMangaFeedVortexScans(offsetOrPage, new VortexScansFeedService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> mangas) {
                        callback.onSuccess(mangas);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchMangaListController:VortexScans] Error fetching manga list: " + message);
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

    public static void fetchMangaDetails(Context context,String serviceFeed, String mangaUrlOrId, MangaCallback callback) {
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
            case "DemonicScans":
                DemonicScansFeedService.getMangaDetailsDemonicScans(mangaUrlOrId, new DemonicScansFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[fetchMangaDetails:DemonicScans] Error fetching manga URL " + mangaUrlOrId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "ManhuaFast":
                ManhuaFastFeedService.getMangaDetailsManhuaFast(mangaUrlOrId, new ManhuausFeedService.MangaCallback() {
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }


                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[fetchMangaDetails:ManhuaFast] Error fetching manga URL " + mangaUrlOrId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "FlameComics":
                FlameComicsFeedService.getMangaDetailsFlameComics(mangaUrlOrId, new FlameComicsFeedService.MangaCallback() {
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[fetchMangaDetails:FlameComics] Error fetching manga URL " + mangaUrlOrId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Rizzfables":
                RizzfablesFeedService.getMangaDetailsRizzfables(mangaUrlOrId, new RizzfablesFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[fetchMangaDetails:Rizzfables] Error fetching manga URL " + mangaUrlOrId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Mgeko":
                MgekoFeedService.getMangaDetailsMgeko(mangaUrlOrId, new MgekoFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[fetchMangaDetails:Mgeko] Error fetching manga URL " + mangaUrlOrId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Comix":
                ComixFeedService.getMangaDetailsComix(context,mangaUrlOrId, new ComixFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[fetchMangaDetails:COmix] Error fetching manga URL " + mangaUrlOrId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
                case "VortexScans":
                    VortexScansFeedService.getMangaDetailsVortexScans(mangaUrlOrId, new VortexScansFeedService.MangaCallback() {
                        @Override
                        public void onSuccess(MangaItemModel manga) {
                            callback.onSuccess(manga);
                        }
                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "[fetchMangaDetails:VortexScans] Error fetching manga URL " + mangaUrlOrId + ": " + errorMessage);
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

    public static void mangaGetDescription(Context context,String serviceFeed, String mangaId, String mangaUrl, DescriptionCallback callback) {
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
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);

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
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
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
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + message);

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
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "DemonicScans":
                DemonicScansFeedService.getMangaDetailsDemonicScans(mangaUrl, new DemonicScansFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[mangaGetDescription:DemonicScans] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "ManhuaFast":
                ManhuaFastFeedService.getMangaDetailsManhuaFast(mangaUrl, new ManhuausFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());

                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[mangaGetDescription:ManhuaFast] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "FlameComics":
                FlameComicsFeedService.getMangaDetailsFlameComics(mangaUrl, new FlameComicsFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[mangaGetDescription:FlameComics] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Rizzfables":
                RizzfablesFeedService.getMangaDetailsRizzfables(mangaUrl, new RizzfablesFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[mangaGetDescription:Rizzfables] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Mgeko":
                MgekoFeedService.getMangaDetailsMgeko(mangaUrl, new MgekoFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[mangaGetDescription:Mgeko] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Comix":
                ComixFeedService.getMangaDetailsComix(context, mangaUrl, new ComixFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[mangaGetDescription:Comix] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "VortexScans":
                VortexScansFeedService.getMangaDetailsVortexScans(mangaUrl, new VortexScansFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga.getDescription());
                    }
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[mangaGetDescription:VortexScans] Error fetching description for manga URL " + mangaUrl + ": " + errorMessage);
                        Log.e(TAG, "[MangaSourceFeed]: " + serviceFeed + " -> " + mangaUrl + ": " + errorMessage);
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
            case "DemonicScans":
                return mangaUrl;
            case "ManhuaFast":
                return mangaUrl;
            case "FlameComics":
                return mangaUrl;
            case "Rizzfables":
                return mangaUrl;
            case "Mgeko":
                return mangaUrl;
            case "Comix":
                return mangaUrl;
            case "VortexScans":
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
            case "DemonicScans":
                return chapterUrl;
            case "ManhuaFast":
                return chapterUrl;
            case "FlameComics":
                return chapterUrl;
            case "Rizzfables":
                return chapterUrl;
            case "Mgeko":
                return chapterUrl;
            case "Comix":
                return chapterUrl;
            case "VortexScans":
                return chapterUrl;
            default:
                Log.e(TAG, "[getChapterIdOrChapterUrl] Unknown source:getChapterIdOrUrl " + source);
                return "";
        }
    }

    public static void fetchChapterListController(Context context, String serviceFeed, String mangaUrlOrId, int offset, int limit, String descAsc, ChapterListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchChapterListController] Callback is null for serviceFeed: " + serviceFeed);
            return;
        }

        switch (serviceFeed) {
            case "MangaDex":
                MangaDexChaptersService.fetchAllChapters(mangaUrlOrId, descAsc, offset, limit, new MangaDexChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        Handler main = new Handler(Looper.getMainLooper());
                        main.post(() -> callback.onSuccess(chapters));
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
            case "DemonicScans":
                DemonicScansChaptersService.getChaptersDemonicScans(mangaUrlOrId, new DemonicScansChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:DemonicScans] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "ManhuaFast":
                ManhuaFastChaptersService.getChaptersManhuaFast(context, mangaUrlOrId, new ManhuaFastChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:ManhuaFast] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "FlameComics":
                FlameComicsChaptersService.getChaptersFlameComics(mangaUrlOrId, new FlameComicsChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:FlameComics] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "Rizzfables":
                RizzfablesChaptersService.getChaptersRizzfables(mangaUrlOrId, new RizzfablesChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:Rizzfables] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "Mgeko":
                MgekoChaptersService.getChaptersMgeko(mangaUrlOrId, new MgekoChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:Mgeko] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "Comix":
                ComixChapterListService service = new ComixChapterListService();

                service.getChapterList(context,mangaUrlOrId, new ComixChapterListService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:Comix] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "VortexScans":
                VortexScansChaptersService.getChaptersVortexScans(mangaUrlOrId, new VortexScansChaptersService.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        if ("asc".equalsIgnoreCase(descAsc)) Collections.reverse(chapters);
                        callback.onSuccess(chapters);
                    }
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[fetchChapterListController:VortexScans] Error fetching chapters for manga URL " + mangaUrlOrId + ": " + message);
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

    public static void getMangaItem(Context context, String source, String mangaUrlId, MangaCallback callback) {
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
            case "DemonicScans":
                DemonicScansFeedService.getMangaDetailsDemonicScans(mangaUrlId, new DemonicScansFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:DemonicScans] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "ManhuaFast":
                ManhuaFastFeedService.getMangaDetailsManhuaFast(mangaUrlId, new ManhuausFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:ManhuaFast] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "FlameComics":
                FlameComicsFeedService.getMangaDetailsFlameComics(mangaUrlId, new FlameComicsFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:FlameComics] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Rizzfables":
                RizzfablesFeedService.getMangaDetailsRizzfables(mangaUrlId, new RizzfablesFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:Rizzfables] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Mgeko":
                MgekoFeedService.getMangaDetailsMgeko(mangaUrlId, new MgekoFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:Mgeko] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "Comix":
                ComixFeedService.getMangaDetailsComix(context,mangaUrlId, new ComixFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:Comix] Error for manga URL " + mangaUrlId + ": " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
                break;
            case "VortexScans":
                VortexScansFeedService.getMangaDetailsVortexScans(mangaUrlId, new VortexScansFeedService.MangaCallback() {
                    @Override
                    public void onSuccess(MangaItemModel manga) {
                        callback.onSuccess(manga);
                    }
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "[getMangaItem:VortexScans] Error for manga URL " + mangaUrlId + ": " + errorMessage);
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
                        Handler main = new Handler(Looper.getMainLooper());
                        main.post(() -> callback.onSuccess(pages));
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
            case "DemonicScans":
                DemonicScansChaptersService.getChapterDemonicScans(chapterUrlId, new DemonicScansChaptersService.ChapterCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {

                        callback.onSuccess(chapter);
                    }


                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:DemonicScans] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "ManhuaFast":
                ManhuaFastChaptersService.getChapterMangaManhuaFast(chapterUrlId, new ManhuaFastChaptersService.ChapterCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {
                        callback.onSuccess(chapter);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:ManhuaFast] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "FlameComics":
                FlameComicsChaptersService.getChapterFlameComics(chapterUrlId, new FlameComicsChaptersService.ChapterCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {
                        callback.onSuccess(chapter);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:FlameComics] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "Rizzfables":
                RizzfablesChaptersService.getChapterRizzfables(chapterUrlId, new RizzfablesChaptersService.ChapterCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {
                        callback.onSuccess(chapter);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:Rizzfables] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "Mgeko":
                MgekoChaptersService.getChapterMgeko(chapterUrlId, new MgekoChaptersService.ChapterCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {
                        callback.onSuccess(chapter);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:Mgeko] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "Comix":
                ComixChapterPagesService service = new ComixChapterPagesService();

                service.getChapterPages(context,chapterUrlId, new ComixChapterPagesService.PagesCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {
                        callback.onSuccess(chapter);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:Comix] Error fetching pages for chapter " + chapterUrlId + ": " + message);
                        callback.onError(message);
                    }
                });
                break;
            case "VortexScans":
                VortexScansChaptersService.getChapterVortexScans(chapterUrlId, new VortexScansChaptersService.ChapterCallback() {
                    @Override
                    public void onSuccess(List<String> chapter) {
                        callback.onSuccess(chapter);
                    }
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "[getChapterPages:VortexScans] Error fetching pages for chapter " + chapterUrlId + ": " + message);
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
        List<String> sources = List.of("AsuraScans", "Manhuaus", "ManhuaPlus", "DemonicScans", "ManhuaFast", "FlameComics", "Rizzfables", "Mgeko","Comix","VortexScans");
        List<MangaItemModel> allResults = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completed = new AtomicInteger(0);
        int totalSources = sources.size();

        for (String source : sources) {
            fetchSearchMangas(query, source, new ServiceController.MangaListCallback() {
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
                        Log.e("SearchAllSources", "[" + source + "] " + message);
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

            // Optional: failsafe timeout in case fetchSearchMangas never calls callback
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (completed.get() < totalSources) {
                    completed.incrementAndGet();
                    Log.e("SearchAllSources", "[" + source + "] timed out");
                    if (completed.get() == totalSources) {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> {
                            if (allResults.isEmpty()) callback.onError("No results found.");
                            else callback.onSuccess(new ArrayList<>(allResults));
                        });
                    }
                }
            }, 3000); // 5 seconds timeout
        }


    }

    public static void fetchSearchMangas(String query, String source, MangaListCallback callback) {
        if (callback == null) {
            Log.e(TAG, "[fetchSearchMangas] Callback is null for source: " + source);
            return;
        }

        switch (source) {
            case "MangaDex":
                MangaDexSearchService.searchManga(query, 0, 50, new MangaDexSearchService.MangaListCallback() {
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
                AsuraScansSearchService.search(query, new AsuraScansSearchService.MangaListCallback() {
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
                ManhuausSearchService.search(query, new ManhuausSearchService.MangaListCallback() {
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
                ManhuaPlusSearchService.search(query, new ManhuaPlusSearchService.MangaListCallBack() {
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
            case "DemonicScans":
                DemonicScansSearchService.search(query, new DemonicScansSearchService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:DemonicScans] Error searching query '" + query + "': " + error);
                    }
                });
                break;
            case "ManhuaFast":
                ManhuaFastSearchService.search(query, new ManhuaFastSearchService.MangaListCallBack() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:ManhuaFast] Error searching query '" + query + "': " + error);
                    }
                });
                break;
            case "FlameComics":
                FlameComicsSearchService.search(query, new FlameComicsSearchService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:FlameComics] Error searching query '" + query + "': " + error);
                    }
                });
                break;
            case "Rizzfables":
                RizzfablesSearchService.search(query, new RizzfablesSearchService.MangaListCallBack() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:Rizzfables] Error searching query '" + query + "': " + error);
                    }
                });
                break;
            case "Mgeko":
                MgekoSearchService.search(query, new MgekoSearchService.MangaListCallBack() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:Mgeko] Error searching query '" + query + "': " + error);
                    }
                });
                break;
            case "Comix":
                ComixSearchService.search(query, new ComixSearchService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:Comix] Error searching query '" + query + "': " + error);
                    }
                });
                break;
            case "VortexScans":
                VortexScansSearchService.search(query, new VortexScansSearchService.MangaListCallback() {
                    @Override
                    public void onSuccess(List<MangaItemModel> results) {
                        callback.onSuccess(results);
                    }
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "[fetchSearchMangas:VortexScans] Error searching query '" + query + "': " + error);
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

    public interface LastChapterTitleCallback {
        void onSucces(String lastChapterTitle);

        void onError(String errorMessage);
    }
}
