package com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
                    Document doc = Jsoup.connect(mangaUrl)
                            .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                            .timeout(TIMEOUT_MS)
                            .get();


                    String[] parts = mangaUrl.split("-");
                    String mangaId = parts[parts.length - 1];

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
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

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
        String mangaUrl = "https://asurascans.com/?page=" + pageNumber;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();

            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                Elements mangaEntries = doc.select("div.grid.grid-cols-12");
                for (Element entry : mangaEntries) {

                    // Main manga link (title)
                    Element titleLink = entry.selectFirst("a[href^=/comics/]");

                    // Cover image
                    Element img = entry.selectFirst("img");

                    // Latest chapter link
                    Element chapter = entry.selectFirst("a[href*=/chapter/]");

                    Elements grids = doc.select("div.grid.grid-cols-12");
                    Log.d(TAG, "Total grid containers: " + grids.size());

                    for (int i = 0; i < grids.size(); i++) {
                        Element grid = grids.get(i);
                        Elements links = grid.select("a[href^=/comics/]");
                        Log.d(TAG, "GRID #" + i + " comics links count = " + links.size());

                        for (int j = 0; j < Math.min(5, links.size()); j++) {
                            Log.d(TAG, "GRID #" + i + " link " + j + " = " + links.get(j).text() + " -> " + links.get(j).attr("href"));
                        }
                    }

                    if (titleLink != null) {

                        String href = "https://asurascans.com" + titleLink.attr("href");

                        // Extract title (alt attribute)
                        String title = entry.selectFirst("img").attr("alt");

                        String coverUrl = img != null ? img.absUrl("src") : "";

                        // Extract ID (last part after "-")
                        String[] parts = href.split("-");
                        String lastPart = href.substring(href.lastIndexOf("-") + 1);
                        String mangaId = lastPart;

                        // Extract chapter text (if available)
                        String chapterText = chapter != null ? chapter.text().trim() : "";

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


                    }
                }

                for (MangaItemModel mangas : mangaList){
                    Log.d(TAG, "onSuccess: " + "Page: " + pageNumber);
                    Log.d(TAG, "onSuccess: " + mangas.getTitle());

                }

                Log.d(TAG, "Requested URL: " + mangaUrl);
                Log.d(TAG, "Final URL: " + doc.location());
                Log.d(TAG, "HTML hash: " + doc.outerHtml().hashCode());

                mainHandler.post(() -> callback.onSuccess(mangaList));

            } catch (IOException e) {
                Log.e(TAG, "Error scraping Asura feed: ", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static String generateMangaId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        String id = siteName + "-" + hash;
        return id;
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
