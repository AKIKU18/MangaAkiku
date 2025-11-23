package com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausChaptersService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlameComicsChaptersService {
    private static final int TIMEOUT_MS = 60000; // 60 seconds
    private static final int MAX_RETRIES = 1;

    public static String generateChapterId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        String id = siteName + "-" + hash;
        return id;
    }


    public static void getChaptersFlameComics(String mangaUrl, ChapterListCallback callback) {
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

                    // Extrage ID-ul manga (ultima parte după ultimul '/')
                    String[] parts = mangaUrl.split("/");
                    String mangaCode = parts[parts.length - 1];

                    // Construiește URL-ul JSON
                    String jsonUrl = "https://flamecomics.xyz/_next/data/VZEdwcuZVY5GW40mJJ_Nk/series/" + mangaCode + ".json?id=" + mangaCode;

                    Log.d("FlameComics", "JSON URL: " + jsonUrl);

                    Document doc = Jsoup.connect(jsonUrl)
                            .ignoreContentType(true)
                            .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                            .timeout(TIMEOUT_MS)
                            .get();

                    String jsonString = doc.body().text();


                    List<ChapterModel> chapters = new ArrayList<>();

                    JSONObject jsonObject = new JSONObject(jsonString);

                    JSONObject pageProps = jsonObject.getJSONObject("pageProps");
                    JSONObject series = pageProps.getJSONObject("series"); // for manga info
                    JSONArray chaptersArray = pageProps.getJSONArray("chapters"); // correct

                    for (int i = 0; i < chaptersArray.length(); i++) {
                        JSONObject ch = chaptersArray.getJSONObject(i);
                        String chapterId = ch.getString("token");
                        String chapterUrl = "https://flamecomics.xyz/series/" + series.getString("series_id") + "/" + ch.getString("token");

                        String title = ch.optString("title", "No title");

                        String chapterStr = ch.optString("chapter", "0"); // "10.00"
                        int chapterInt = (int) Double.parseDouble(chapterStr); // 10
                        String number = String.valueOf(chapterInt);          // "10"
                        if(title.isEmpty() || title != null){
                            title = "Chapter " + number;
                        }

                        Log.e("title", title);
                        // Creează ChapterModel și adaugă în listă
                        ChapterModel chapter = new ChapterModel(
                                chapterId,
                                title,
                                number,
                                chapterUrl,
                                "FlameComics"
                        );
                        chapters.add(chapter);
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


    public static void getChapterFlameComics(String chapterUrl, FlameComicsChaptersService.ChapterCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        //https://flamecomics.xyz/_next/data/VZEdwcuZVY5GW40mJJ_Nk/series/150/b5aad0ebf452dbdf.json?id=150&token=b5aad0ebf452dbdf API chapter images links
        executor.execute(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    Document doc = Jsoup.connect(chapterUrl)
                            .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                            .timeout(TIMEOUT_MS)
                            .get();

                    Elements imgElements = doc.select("div.m_6d731127.mantine-Stack-root img");
                    List<String> imageUrls = new ArrayList<>();
                    for (Element img : imgElements) {
                        String url = img.attr("src").trim(); // get data-src
                        if (!url.isEmpty() && !url.contains("https://cdn.flamecomics.xyz/assets")) {
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
