package com.example.mangav5.ServicesAsuraScans;

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

                    Element titleElement = doc.selectFirst("div.text-center.sm\\:text-left > span");
                    Element coverElement = doc.selectFirst("div.relative.col-span-full.sm\\:col-span-3 img");
                    Element description = doc.selectFirst("div.col-span-12.sm\\:col-span-9 > span");

                    MangaItemModel manga = new MangaItemModel(
                            mangaId,
                            titleElement != null ? titleElement.text() : "No title",
                            description != null ? description.text() : "",
                            coverElement != null ? coverElement.attr("src") : "",
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

                Elements mangaEntries = doc.select("div.w-full.p-1.pt-1.pb-3");

                for (Element entry : mangaEntries) {
                    Element titleLink = entry.selectFirst("span.text-\\[15px\\] a");
                    Element img = entry.selectFirst("img");
                    Element chapter = entry.selectFirst("div.flex.text-sm a[href]");

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
                                chapter != null ? chapter.text() : "",
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
