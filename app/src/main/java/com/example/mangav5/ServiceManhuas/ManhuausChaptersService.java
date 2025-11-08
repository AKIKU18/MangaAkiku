package com.example.mangav5.ServiceManhuas;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManhuausChaptersService {
    private static final int TIMEOUT_MS = 60000; // 60 seconds
    private static final int MAX_RETRIES = 3;

    public static String generateChapterId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        String id = siteName + "-" + hash;
        return id;
    }


    public static void getChaptersManhuaus(String mangaUrl, ChapterListCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    Document doc = Jsoup.connect(mangaUrl)
                            .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                            .timeout(TIMEOUT_MS)
                            .get();

                    /*String chapterId,
                    String title,
                    String number,
                    String chapterUrl,
                    String source
                            */
                    List<ChapterModel> chapters = new ArrayList<>();
                    Elements chapterLinks = doc.select("li.wp-manga-chapter a[href]");

                    for (Element link : chapterLinks) {
                        String chapterTitle = link.text().trim();
                        String chapterNumber = "0";
                        Matcher matcher = Pattern.compile("(\\d+)").matcher(chapterTitle);
                        if (matcher.find()) {
                            chapterNumber = matcher.group(1);
                        }
                        String chapterId = generateChapterId(mangaUrl, chapterTitle);
                        String chapterUrl = link.attr("href");
                        String source = "ManhuaFast";

                        ChapterModel chapter = new ChapterModel(chapterId, chapterTitle, chapterNumber, chapterUrl, source);
                        chapters.add(chapter);
                    }


                    mainHandler.post(() -> callback.onSuccess(chapters));
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

    public static void getChapterMangaManhuaus(String chapterUrl, ChapterCallback callback) {
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

                    Elements imgElements = doc.select("div.reading-content img.wp-manga-chapter-img");

                    List<String> imageUrls = new ArrayList<>();
                    for (Element img : imgElements) {
                        String url = img.attr("data-src").trim(); // get data-src
                        if (!url.isEmpty()) {
                            imageUrls.add(url);
                        }
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

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);

        void onError(String message);
    }

    public interface ChapterCallback {
        void onSuccess(List<String> chapter);

        void onError(String message);
    }


}
