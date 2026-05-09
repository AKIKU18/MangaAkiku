package com.example.mangav5.ServicesMangaWebsites.VortexScans;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans.AsuraScansFeedService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VortexScansFeedService {
    private static final String TAG = "VortexScans";
    private static final int TIMEOUT_MS = 60000; // 60 seconds
    private static final int MAX_RETRIES = 3;

    public static void getMangaDetailsVortexScans(String mangaUrl,
                                               VortexScansFeedService.MangaCallback callback) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            try {

                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(15000)
                        .get();

                // TITLE
                String titleElement = doc.selectFirst(
                        "div.flex.min-w-0.flex-1.flex-col.gap-3.px-2.py-4.sm\\:px-3 h1").text();

                String title = titleElement != null
                        ? titleElement
                        : "Unknown";

                // COVER
                String coverElement = doc.selectFirst("div.relative astro-island div img").attr("src");

                String coverUrl = "";

                if (coverElement != null) {
                    coverUrl = coverElement;
                }

                // DESCRIPTION
                String descriptionText = "";

                Element descriptionElement =
                        doc.selectFirst("div.flex.flex-col.gap-1.text-foreground");

                if (descriptionElement != null) {
                    descriptionText = descriptionElement.text().trim();
                }

                Element chapter = doc.selectFirst("div.mt-4.space-y-2 a");

                String lastChapter = "";

                if (chapter != null) {
                    // get full text
                    String text = chapter.text();

                    // remove "New" if present
                    lastChapter = text.replace("New", "").trim();
                }


                // MANGA ID
                String mangaId = generateMangaId(mangaUrl, title);

                MangaItemModel manga = new MangaItemModel(
                        mangaId,
                        title,
                        descriptionText,
                        coverUrl,
                        false,
                        mangaUrl,
                        "",
                        "VortexScans"
                );

                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (Exception e) {

                Log.e(TAG, "Error loading manga info", e);

                mainHandler.post(() ->
                        callback.onError(
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Unknown error"
                        )
                );
            }

            executor.shutdown();
        });
    }

    public static void getMangaFeedVortexScans(int pageNumber, VortexScansFeedService.MangaListCallback callback) {
        String siteUrl = "https://vortexscans.org/?page=" + pageNumber;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();

            try {
                Document doc = Jsoup.connect(siteUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                        .timeout(15000)
                        .get();

                Log.d(TAG, "Requested URL: " + siteUrl);

                Elements mangaEntries = doc.select("div.grid > div.h-full");
                for (Element entry : mangaEntries) {
                    try {

                        Element mangaLink = entry.selectFirst("a[href^=/series/]");

                        if (mangaLink == null) continue;

                        String mangaTitle = mangaLink.attr("title");

                        String mangaUrl =
                                "https://vortexscans.org" + mangaLink.attr("href");

                        Element imageElement = entry.selectFirst("img");

                        String imageCover = "";

                        if (imageElement != null) {
                            imageCover = imageElement.attr("src");
                        }

                        String mangaId = generateMangaId(mangaUrl, mangaTitle);


                        MangaItemModel manga = new MangaItemModel(
                                mangaId,
                                mangaTitle,
                                "",
                                imageCover,
                                false,
                                mangaUrl,
                                "",
                                "VortexScans"
                        );

                        mangaList.add(manga);

                    } catch (Exception inner) {
                        Log.e(TAG, "Error parsing series card", inner);
                    }
                }

                mainHandler.post(() -> callback.onSuccess(mangaList));

            } catch (IOException e) {
                Log.e(TAG, "Error scraping Asura feed: ", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            } finally {
                executor.shutdown();
            }
        });
    }

    public static String generateMangaId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        String id = siteName + "-" + hash;
        return id;
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
