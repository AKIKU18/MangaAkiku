package com.example.mangav5.ServiceMaster;

import android.content.Context;
import android.util.Log;


import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.demonicScansTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DataTimedLoader {
    public long timestamp;

    /**
     * Defines a common contract for all manga source services.
     */
    public interface IMangaSource {
        interface MangaListCallback {
            void onSuccess(List<MangaItemModel> mangas);
            void onError(String message);
        }
        void getMangaFeed( int page, MangaListCallback callback);

        String getSourceName();

        interface  MangaCallback{
            void onSuccess(MangaItemModel manga);
            void onError(String errorMessage);
        }

        void getMangaDetails(String source, String mangaUrlId, ServiceController.MangaCallback callback);
    }

    public interface IMangaChapters{
        interface ChapterListCallback{
            void onSuccess(List<ChapterModel> chapters);
            void onError(String message);
        }
        void getChapterList(String source, String mangaUrlId, ChapterListCallback callback);

        interface ChapterPagesCallback{
            void onSuccess(List<String> pages);
            void onError(String message);
        }
    }

    public interface IMangaSearch{
        interface MangaSearchCallback{
            void onSuccess(List<MangaItemModel> mangas);
            void onError(String message);
        }

        void searchManga(String query, MangaSearchCallback callback);

    }



    private static final String TAG = "DataTimedLoader";

    public static void loadAndSaveAllManga(
            int offsetOrPage,
            IMangaSource.MangaListCallback finalCallback
    ) {

        Log.d(TAG, "Starting to load all sources");

        List<IMangaSource> allSources = List.of(
                new demonicScansTest()
        );

        List<MangaItemModel> allMangas = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);

        for (IMangaSource source : allSources) {
            source.getMangaFeed( offsetOrPage, new IMangaSource.MangaListCallback() {

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





}

