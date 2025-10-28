package com.example.mangav5.ServicesAsuraScans;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsuraScraperTask {

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);
        void onError(String errorMessage);
    }

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);
        void onError(String message);
    }

    public interface PagesCallback {
        void onSuccess(List<String> chapters);
        void onError(String message);
    }

    private static final String TAG = "AsuraScraper";

    public static void getMangaInfoAsuraScans(String mangaUrl,MangaCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // 1. Fetch HTML
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                // 2. Get Info
                String[] parts = mangaUrl.split("-");

                String mangaId = parts[parts.length - 1];  // take last part
                Element titleElement = doc.selectFirst("div.text-center.sm\\:text-left > span");
                Element coverElement = doc.selectFirst("div.relative.col-span-full.sm\\:col-span-3 img");
                Element description = doc.selectFirst("div.col-span-12.sm\\:col-span-9 > span");




                MangaItemModel manga = new MangaItemModel(mangaId, titleElement.text(), description.text(), coverElement.attr("src"), false,mangaUrl,"");
                callback.onSuccess(manga);

            } catch (IOException e) {
                Log.e(TAG, "Error scraping Asura: ", e);
            }
        });
    }

    public static void getMangaChaptersAsuraScans(String mangaUrl,ChapterListCallback callback){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // 1. Fetch HTML
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                Element chapterContainer = doc.selectFirst(
                        "div.overflow-y-auto.scrollbar-thumb-themecolor.space-y-2\\.5"
                );



                // 4. Return to main thread
                mainHandler.post(() -> {
                    List<ChapterModel> chapters = new ArrayList<>();
                    if (chapterContainer != null) {
                        Elements chapterLinks = chapterContainer.select("a[href]");
                        for (Element link : chapterLinks) {
                            Elements h3s = link.select("h3");
                            String[] parts = link.absUrl("href").split("/");       // split by '/'


                            String chapterTitle = h3s.size() > 1 ? h3s.get(0).text() : "First h3";
                            String dateUploaded = h3s.size() > 1 ? h3s.get(1).text() : "Second h3";
                            String chapterUrl = link.absUrl("href");  // full URL
                            String chapterId = chapterTitle.replace(" ", "") + dateUploaded.replace(" ", "");
                            String chapterNumber = parts[parts.length - 1];  // take last part


                            ChapterModel chapter = new ChapterModel(chapterId, chapterTitle, chapterNumber, chapterUrl);
                            chapters.add(chapter);
                        }
                    }

                    callback.onSuccess(chapters);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error scraping Asura: ", e);
            }
        });

    }

    public static void getAsuraScansMangaFeed(int pageNumber, MangaListCallback callback) {
        String mangaUrl = "https://asuracomic.net/page/" + pageNumber;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();

            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                Elements mangaEntries = doc.select("div.w-full.p-1.pt-1.pb-3");

                for (Element entry : mangaEntries) {
                    Element titleLink = entry.selectFirst("span.text-\\[15px\\] a");
                    Element img = entry.selectFirst("img"); // get cover image
                    Element chapter = entry.selectFirst("div.flex.text-sm a[href]");
                    Log.e("AsuraScraper", "Chapter Title:" + chapter.text());
                    if (titleLink != null) {
                        String href = "https://asuracomic.net" + titleLink.attr("href");
                        String title = titleLink.text();
                        String coverUrl = img != null ? img.absUrl("src") : "";
                        String[] parts = href.split("-");
                        String mangaId = parts[parts.length - 1];

                        MangaItemModel manga = new MangaItemModel(
                                mangaId,
                                title,
                                "",
                                coverUrl,
                                false,
                                href,
                                chapter.text()
                        );
                        //Show last chapter
                        mangaList.add(manga);
                    }
                }

                // ✅ Return the list to main thread
                mainHandler.post(() -> callback.onSuccess(mangaList));

            } catch (IOException e) {
                Log.e(TAG, "Error scraping Asura feed: ", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
