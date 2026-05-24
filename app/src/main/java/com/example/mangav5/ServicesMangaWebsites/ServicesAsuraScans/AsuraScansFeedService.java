package com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Network.NetworkHelper;
import com.example.mangav5.ScriptHelper.GenerateMangaIDHex;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsuraScansFeedService {

    private static final String TAG = "AsuraScraper";
    private static final int TIMEOUT_MS = 60000; // 60 seconds
    private static final int MAX_RETRIES = 3;

    public static void getMangaInfoAsuraScans(String mangaUrl, MangaCallback callback) {
        if (mangaUrl.contains("api.mangadex.org")) {
            Log.e(TAG, "❌ Wrong URL: Jsoup cannot parse JSON from " + mangaUrl);
            callback.onError("Invalid URL for AsuraScansFeedService");
            return;
        }


        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    Document doc = NetworkHelper.getJsoupConnection(mangaUrl).get();


                    String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);

                    Element titleElement = doc.selectFirst("h1");

                    Element coverElement = doc.selectFirst(".z-0 img");
                    Element description = doc.selectFirst("#description-text");
                    String title = titleElement != null ? titleElement.text().trim() : "No title";
                    String coverUrl = coverElement != null ? coverElement.absUrl("src") : "";
                    String descriptionText = description != null ? description.text().trim() : "";


                    MangaItemModel manga = new MangaItemModel(
                            mangaId,
                            title,
                            descriptionText,
                            coverUrl,
                            false,
                            mangaUrl,
                            "",
                            "AsuraScans"
                    );

                    mainHandler.post(() -> callback.onSuccess(manga));

                    return; // success, exit loop
                } catch (IOException e) {
                    attempts++;
                    Log.e(TAG, "Attempt " + attempts + " failed for " + mangaUrl, e);

                    if (attempts >= MAX_RETRIES) {
                        final String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        mainHandler.post(() -> callback.onError(msg));
                    }
                }
            }
        });
    }

    public static void getMangaChaptersAsuraScans(String mangaUrl, ChapterListCallback callback) {
        if (mangaUrl.contains("api.mangadex.org")) {
            Log.e(TAG, "❌ Wrong URL: Jsoup cannot parse JSON from " + mangaUrl);
            callback.onError("Invalid URL for AsuraScansFeedService");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Document doc = NetworkHelper.getJsoupConnection(mangaUrl).get();

                Element chapterContainer = doc.selectFirst(
                        "div.divide-y:nth-child(1)"
                );


                List<ChapterModel> chapters = new ArrayList<>();
                if (chapterContainer != null) {
                    Elements chapterLinks = chapterContainer.select("a[href]");

                    for (Element link : chapterLinks) {
                        String url = link.absUrl("href");
                        String chapterNumber = url.substring(url.lastIndexOf("/") + 1);
                        Element titleElement = link.selectFirst("span.font-medium");
                        String chapterTitle = titleElement != null ? titleElement.text() : "Unknown";
                        String chapterId = generateChapterId(url, chapterTitle);
                        String chapterUrl =link.absUrl("href");

                        ChapterModel chapter = new ChapterModel(chapterId, chapterTitle, chapterNumber, chapterUrl, "AsuraScans");
                        chapters.add(chapter);
                    }
                }

                mainHandler.post(() -> callback.onSuccess(chapters));

            } catch (IOException e) {
                Log.e(TAG, "Error scraping Asura chapters: ", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void getAsuraScansMangaFeed(int pageNumber, MangaListCallback callback) {
        String mangaUrl = "https://asurascans.com/browse?page=" + pageNumber;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();

            try {
                Document doc = NetworkHelper.getJsoupConnection(mangaUrl).get();

                Elements mangaEntries = doc.select("#series-grid .series-card");

                for (Element entry : mangaEntries) {
                    try {
                        Element titleLink = entry.selectFirst("a[href^=/comics/]");
                        Element img = entry.selectFirst("img");
                        Element titleElement = entry.selectFirst("h3");

                        if (titleLink == null || titleElement == null) continue;

                        String href = titleLink.absUrl("href");
                        if (href.isEmpty()) {
                            href = "https://asurascans.com" + titleLink.attr("href");
                        }

                        String title = titleElement.text().trim();
                        if (title.isEmpty() && img != null) {
                            title = img.attr("alt").trim();
                        }

                        if (title.isEmpty() || href.isEmpty()) continue;

                        String coverUrl = "";
                        if (img != null) {
                            coverUrl = img.absUrl("src");
                            if (coverUrl.isEmpty()) {
                                coverUrl = img.absUrl("data-src");
                            }
                        }

                        String mangaId = GenerateMangaIDHex.generateUuidHex(href);

                        // chapters text
                        String chapterText = "";
                        Elements infoSpans = entry.select("div.p-3 div.flex.items-center.gap-2.mt-2 span");
                        if (infoSpans.size() > 0) {
                            chapterText = infoSpans.get(0).text().trim(); // ex: 85 Chapters
                        }

                        MangaItemModel manga = new MangaItemModel(
                                mangaId,
                                title,
                                "",
                                coverUrl,
                                false,
                                href,
                                chapterText,
                                "AsuraScans"
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


    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);

        void onError(String errorMessage);
    }

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);

        void onError(String message);
    }
}
