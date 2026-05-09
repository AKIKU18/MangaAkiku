package com.example.mangav5.ServicesMangaWebsites.VortexScans;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VortexScansChaptersService {

    private static final int TIMEOUT_MS = 60000;
    private static final int MAX_RETRIES = 1;

    public static void getChapterVortexScans(String chapterUrl,
                                       ChapterCallback callback) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            int attempts = 0;

            while (attempts < MAX_RETRIES) {

                try {

                    Document doc = Jsoup.connect(chapterUrl)
                            .userAgent("Mozilla/5.0 (Android App)")
                            .timeout(TIMEOUT_MS)
                            .get();

                    List<String> pages = new ArrayList<>();

                    // ALL IMAGES
                    Elements imgs = doc.select(
                            "section[itemprop=articleBody] figure.image-container img"
                    );

                    for (Element img : imgs) {
                        String url = img.attr("src");

                        if (url != null && !url.isEmpty()) {
                            Log.e("VortexScansChaptersService", "Found image: " + url);
                            pages.add(url);
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(pages));
                    return;

                } catch (Exception e) {

                    attempts++;

                    if (attempts >= MAX_RETRIES) {
                        String msg = e.getMessage() != null
                                ? e.getMessage()
                                : "Unknown error";

                        mainHandler.post(() -> callback.onError(msg));
                    }
                }
            }

            executor.shutdown();
        });
    }

    public static void getChaptersVortexScans(String mangaUrl,
                                      ChapterListCallback callback) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            try {

                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(60000)
                        .get();

                List<ChapterModel> chapters = new ArrayList<>();

                Elements chapterElements = doc.select(
                        "div.mt-4.space-y-2 a[href*='chapter']"
                );

                // Get the last chapter and extract the number
                Element lastChapterElement = chapterElements.first();

                String raw = lastChapterElement.text();

                // Extract only number after "Chapter"
                String numb = raw.replaceAll("(?i).*chapter\\s*", "")   // remove everything before number
                        .replaceAll("[^0-9]", "")             // keep only digits
                        .trim();
                int lastChapter = Integer.parseInt(numb);
                for(int i=lastChapter; i > 0; i--){

                    String cleanTitle = "Chapter " + i;
                    String chapterUrl = mangaUrl + "/chapter-" + i;
                    String chapterId = generateChapterId(chapterUrl, cleanTitle);
                    String chapterNumber = i + "";

                    Log.e("VortexScansChaptersService", "Found chapter: " + chapterUrl);
                    chapters.add(new ChapterModel(
                            chapterId,
                            cleanTitle,
                            chapterNumber,
                            chapterUrl,
                            "VortexScans"
                    ));
                }

                mainHandler.post(() -> callback.onSuccess(chapters));

            } catch (Exception e) {

                mainHandler.post(() ->
                        callback.onError(e.getMessage() != null
                                ? e.getMessage()
                                : "Unknown error")
                );
            }

            executor.shutdown();
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