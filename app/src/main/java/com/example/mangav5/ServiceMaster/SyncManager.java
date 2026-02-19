package com.example.mangav5.ServiceMaster;

import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;

import java.util.List;

public class SyncManager {

    public static void SyncLibrary(ServiceController.MangaListCallback callback) {
        ServiceController.fetchMangaListController("DemonicScans", 0, 100, new ServiceController.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) {
                Log.d("SyncManager", "SyncLibrary: " + mangas.size());
                callback.onSuccess(mangas);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}

