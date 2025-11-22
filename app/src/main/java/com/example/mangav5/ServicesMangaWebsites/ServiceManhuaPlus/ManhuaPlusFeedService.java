package com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

public class ManhuaPlusFeedService {

    private static final String TAG = "ManhuaPlusFeedService";
    private static final int TIMEOUT_MS = 60_000;

    /**
     * Fetch homepage feed from ManhuaPlus
     */
    public static void getMangaFeedManhuaPlus(int offsetOrPage,MangaListCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        String websiteManga = "https://manhuaplus.org/all-manga/";
        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(websiteManga + offsetOrPage + "/")
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(TIMEOUT_MS)
                        .get();

                // Each manga block in homepage
                Elements items = doc.select("div.grid.gtc-f141a.gg-20.p-13.mh-77vh > div"); // Update selector if site changes

                for (Element item : items) {
                    try {
                        // Title and URL
                        Element titleA = item.selectFirst("a");
                        String title = titleA != null ? titleA.attr("title").trim() : "";
                        String url = titleA != null ? titleA.attr("href").trim() : "";

                        // Manga ID from URL
                        String mangaId = url.replace("https://manhuaplus.org/manga/", "").trim();

                        // Cover image
                        Element img = item.selectFirst("img");
                        String cover = "";
                        if (img != null) {
                            if (img.hasAttr("data-src") && !img.attr("data-src").isEmpty()) {
                                cover = "https://manhuaplus.org/" +  img.attr("data-src").trim();
                            } else if (img.hasAttr("src")) {
                                cover ="https://manhuaplus.org/" +  img.attr("src").trim() ;
                            }
                        }

                        // Last chapter (optional, from text if available)
                        Element lastChapterEl = item.selectFirst(".chapters a"); // Update if needed
                        String lastChapter = lastChapterEl != null ? lastChapterEl.text().trim() : "";

                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(mangaId);
                        m.setTitle(title);
                        m.setCoverImageUrl(cover);
                        m.setMangaUrl(url);
                        m.setDescription("");
                        m.setLastChapter(lastChapter);
                        m.setSource("ManhuaPlus");

                        mangaList.add(m);

                    } catch (Exception innerEx) {
                        Log.e(TAG, "Failed parsing single item: " + innerEx.getMessage(), innerEx);
                    }
                }

                mainHandler.post(() -> callback.onSuccess(mangaList));

            } catch (IOException e) {
                final String err = e.getMessage() != null ? e.getMessage() : "IO Error";
                Log.e(TAG, "Error fetching ManhuaPlus homepage: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            } catch (Exception e) {
                final String err = e.getMessage() != null ? e.getMessage() : "Unknown Error";
                Log.e(TAG, "Unexpected error: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            }
        });
    }

    /**
     * Fetch manga details page from ManhuaPlus
     */
    public static void getMangaDetailsManhuaPlus(String mangaUrl, MangaCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(TIMEOUT_MS)
                        .get();

                // Manga ID from URL
                String mangaId = mangaUrl.replace("https://manhuaplus.org/manga/", "").trim();

                // Title
                Element titleEl = doc.selectFirst("div.fs-15.s1.p-20.bc-fff.r2 > header > h1");
                String title = titleEl != null ? titleEl.text() : "No title";

                // Description
                Document docDescription = Jsoup.connect("https://manhuaplus.top/manga/"+mangaId)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(TIMEOUT_MS)
                        .get();

                Element descEl = docDescription.selectFirst("#item-detail > div.detail-content > p"); // Update selector if needed
                String desc = descEl != null ? descEl.text().trim() : "";

                // Cover image
                Element coverEl = doc.selectFirst("div.a1 > figure");
                String cover = "";
                cover = "https://manhuaplus.org"+ coverEl.selectFirst("img").attr("src");

                MangaItemModel manga = new MangaItemModel(
                        mangaId,
                        title,
                        desc,
                        cover,
                        false,
                        mangaUrl,
                        "",
                        "ManhuaPlus"
                );

                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "IO Error"));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown Error"));
            }
        });
    }

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);
        void onError(String message);
    }

    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);
        void onError(String errorMessage);
    }
}
