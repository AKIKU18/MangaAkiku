package com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RizzfablesChaptersService {
    private static final int TIMEOUT_MS = 60000; // 60 seconds
    private static final int MAX_RETRIES = 1;

    public static void getChaptersRizzfables(String mangaUrl, RizzfablesChaptersService.ChapterListCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
                    /*String chapterId,
                    String title,
                    String number,
                    String chapterUrl,
                    String source
                            */
        executor.execute(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    Document doc = Jsoup.connect(mangaUrl)
                            .ignoreContentType(true)
                            .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                            .timeout(TIMEOUT_MS)
                            .get();

                    List<ChapterModel> chapters = new ArrayList<>();

                    Elements seriePage = doc.select("#chapterlist > ul > li");

                    for(Element chapter : seriePage){
                        String chapterTitle = chapter.select(".chapternum").text();
                        String chapterUrl = chapter.select("a").attr("href");
                        String chapterId = generateChapterId(chapterUrl,chapterTitle);
                        String chapterNumber = chapterTitle.replaceAll("[^0-9.]", "");

                        ChapterModel chapterModel = new ChapterModel(chapterId, chapterTitle, chapterNumber, chapterUrl, "Rizzfables");
                        chapters.add(chapterModel);
                    }

                    mainHandler.post(() -> callback.onSuccess(chapters));
                    return;

                } catch (Exception e) {
                    attempts++;
                    if (attempts >= MAX_RETRIES) {
                        final String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        mainHandler.post(() -> callback.onError(msg));
                    }
                }
            }
        });
    }

    public static void getChapterRizzfables(String chapterUrl, RizzfablesChaptersService.ChapterCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    Document doc = Jsoup.connect(chapterUrl)
                            .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                            .timeout(TIMEOUT_MS)
                            .get();

                    Elements imgElements = doc.select("#post-69013 > div.entry-content.entry-content-single.maincontent > div.rdminimal img");
                    List<String> imageUrls = new ArrayList<>();
                    for (Element img : imgElements) {
                        String url = img.attr("src").trim(); // get data-src
                        imageUrls.add(url);
                    }
                    mainHandler.post(() -> callback.onSuccess(imageUrls));
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


    public static String generateChapterId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        String id = siteName + "-" + hash;
        return id;
    }

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);

        void onError(String message);
    }

    public interface ChapterCallback {
        void onSuccess(List<String> chapter);

        void onError(String message);
    }
}
