package com.example.mangav5.ServiceMaster;

import android.util.Log;


import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.demonicScansTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DataTimedLoader {
    private static final String TAG = "DataTimedLoader";
    public static List<IMangaSource> allSources = List.of(
            new demonicScansTest()
    );
    public static void loadAndSaveAllManga(int offsetOrPage,IMangaSource.MangaListCallback finalCallback) {
        Log.d(TAG, "Starting to load all sources");

        List<MangaItemModel> allMangas = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);

        for (IMangaSource source : allSources) {
            source.getMangaFeed(offsetOrPage, new IMangaSource.MangaListCallback() {

                @Override
                public void onSuccess(List<MangaItemModel> mangas) {

                    allMangas.addAll(mangas);

                    if (completed.incrementAndGet() == allSources.size()) {
                        finalCallback.onSuccess(allMangas);
                    }
                }

                @Override
                public void onError(String message) {

                    if (completed.incrementAndGet() == allSources.size()) {
                        finalCallback.onSuccess(allMangas);
                    }
                }
            });
        }
    }

    /**
     * Defines a common contract for all manga source services.
     */
    public interface IMangaSource {
        void getMangaFeed(int page, MangaListCallback callback);

        String getSourceName();

        void getMangaDetails(String source, String mangaUrlId, ServiceController.MangaCallback callback);

        interface MangaListCallback {
            void onSuccess(List<MangaItemModel> mangas);

            void onError(String message);
        }

        interface MangaCallback {
            void onSuccess(MangaItemModel manga);

            void onError(String errorMessage);
        }
    }


    public interface IMangaChapters {
        void getChapterList(String source, String mangaUrlId, ChapterListCallback callback);

        interface ChapterListCallback {
            void onSuccess(List<ChapterModel> chapters);

            void onError(String message);
        }

        interface ChapterPagesCallback {
            void onSuccess(List<String> pages);

            void onError(String message);
        }
    }

    public interface IMangaSearch {
        void searchManga(String query, MangaSearchCallback callback);

        interface MangaSearchCallback {
            void onSuccess(List<MangaItemModel> mangas);

            void onError(String message);
        }

    }


}

