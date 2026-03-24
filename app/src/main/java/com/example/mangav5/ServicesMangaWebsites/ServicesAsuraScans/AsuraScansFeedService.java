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

                    Element coverElement = doc.selectFirst("img[alt]");

                    Element container = doc.selectFirst("div.max-w-\\[1285px\\]");
                    Log.e("ContainerDesc",container.toString());


                    String title = titleElement != null ? titleElement.text().trim() : "No title";
                    String coverUrl = coverElement != null ? coverElement.absUrl("src") : "";

                    MangaItemModel manga = new MangaItemModel(
                            mangaId,
                            title,
                            "description",
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
                        "div.overflow-y-auto.scrollbar-thumb-themecolor.space-y-2\\.5"
                );

                List<ChapterModel> chapters = new ArrayList<>();
                if (chapterContainer != null) {
                    Elements chapterLinks = chapterContainer.select("a[href]");
                    for (Element link : chapterLinks) {
                        Elements h3s = link.select("h3");
                        String[] parts = link.absUrl("href").split("/");
                        String chapterTitle = h3s.size() > 0 ? h3s.get(0).text() : "No title";
                        String dateUploaded = h3s.size() > 1 ? h3s.get(1).text() : "";
                        String chapterUrl = link.absUrl("href");
                        String chapterId = chapterTitle.replace(" ", "") + dateUploaded.replace(" ", "");
                        String chapterNumber = parts[parts.length - 1];

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

                Elements mangaEntries = doc.select("div.grid.grid-cols-12");
                for (Element entry : mangaEntries) {

                    // Main manga link (title)
                    Element titleLink = entry.selectFirst("a[href^=/comics/]");

                    // Cover image
                    Element img = entry.selectFirst("img");

                    // Latest chapter link
                    Element chapter = entry.selectFirst("a[href*=/chapter/]");

                    if (titleLink != null) {

                        String href = "https://asuracomic.net" + titleLink.attr("href");
                        String title = entry.selectFirst("img").attr("alt");

                        String coverUrl = img != null ? img.absUrl("src") : "";

                        // Extract ID (last part after "-")
                        String[] parts = href.split("-");
                        String mangaId = parts[parts.length - 1];

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

                mainHandler.post(() -> callback.onSuccess(mangaList));

            } catch (IOException e) {
                Log.e(TAG, "Error scraping Asura feed: ", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
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
