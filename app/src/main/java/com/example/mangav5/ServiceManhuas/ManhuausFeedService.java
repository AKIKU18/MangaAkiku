package com.example.mangav5.ServiceManhuas;

import android.os.Handler;
import android.os.Looper;

import com.example.mangav5.Models.MangaItemModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManhuausFeedService {
    private static final int TIMEOUT_MS = 60000; // 60 seconds
    private static final int MAX_RETRIES = 3;

    public static void getMangaFeedManhuaus(MangaListCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    Document doc = Jsoup.connect("https://manhuaus.com")
                            .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                            .timeout(TIMEOUT_MS)
                            .get();

                    List<MangaItemModel> manga = new ArrayList<>();




                    mainHandler.post(() -> callback.onSuccess(manga));
                    return; // success, exit loop

                } catch (IOException e) {
                    attempts++;
                    if (attempts >= MAX_RETRIES) {
                        final String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        mainHandler.post(() -> callback.onError(msg));
                    }
                }
            }
        });
    }

    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);

        void onError(String errorMessage);
    }

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);

        void onError(String message);
    }
}
