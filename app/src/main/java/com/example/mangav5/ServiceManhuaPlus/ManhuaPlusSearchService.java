package com.example.mangav5.ServiceManhuaPlus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ManhuaPlusSearchService {
    private static final String TAG = "ManhuaPlusSearchService";

    public static void search(String query, SearchCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String searchUrl = "https://manhuaplus.org/search?keyword=" + encodedQuery;
                Log.e(TAG, "🔍 Searching: " + searchUrl);

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                List<MangaItemModel> results = new ArrayList<>();

                // Select all individual manga blocks inside the grid
                Elements items = doc.select("div.grid.gtc-f141a.gg-20.p-13.mh-77vh > div");
                Log.e(TAG, "Found items: " + items.size());

                for (Element item : items) {
                    try {
                        // ✅ Get manga link & title
                        Element titleLink = item.selectFirst("a[title]");
                        String title = titleLink != null ? titleLink.attr("title").trim() : "";
                        String url = titleLink != null ? titleLink.attr("href").trim() : "";
                        String mangaId = url.replace("https://manhuaplus.org/manga/", "").trim();
                        // ✅ Get cover image
                        Element img = item.selectFirst("img");
                        String cover = "";
                        if (img != null) {
                            if (img.hasAttr("data-src") && !img.attr("data-src").isEmpty()) {
                                cover = "https://manhuaplus.org" + img.attr("data-src").trim();
                            } else if (img.hasAttr("src")) {
                                cover = img.attr("src").trim();
                            }
                        }

                        // ✅ Get latest chapter
                        Element chapterEl = item.selectFirst("a[href*=\"/chapter-\"]");
                        String lastChapter = chapterEl != null ? chapterEl.text().trim() : "";

                        // ✅ Build model
                        MangaItemModel m = new MangaItemModel();
                        m.setTitle(title);
                        m.setMangaUrl(url);
                        m.setCoverImageUrl(cover);
                        m.setLastChapter(lastChapter);
                        m.setDescription("");
                        m.setSource("ManhuaPlus");
                        m.setMangaId(mangaId); // can be filled later when opening details

                        results.add(m);

                        Log.e(TAG, "Parsed: " + title + " | " + lastChapter + " | " + cover + " | " + url + " | " + mangaId);
                    } catch (Exception innerEx) {
                        Log.e(TAG, "Error parsing item: " + innerEx.getMessage());
                    }
                }

                Handler mainHandler = new Handler(Looper.getMainLooper());
                if (results.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No results found."));
                } else {
                    mainHandler.post(() -> callback.onSuccess(results));
                }

            } catch (Exception e) {
                Log.e(TAG, "Search failed: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Failed to fetch results: " + e.getMessage()));
            }
        });
    }

    // Callback interface
    public interface SearchCallback {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}
