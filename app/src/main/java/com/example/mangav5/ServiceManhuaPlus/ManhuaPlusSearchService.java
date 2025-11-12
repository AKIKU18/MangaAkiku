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
import java.util.Locale;
import java.util.concurrent.Executors;

public class ManhuaPlusSearchService {
    private static final String TAG = "ManhuaPlusSearchService";

    public static void search(String query, SearchCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String searchUrl = "https://manhuaplus.org/search?keyword=" + encodedQuery;
                Log.d(TAG, "🔍 Searching: " + searchUrl);

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                List<MangaItemModel> results = new ArrayList<>();
                Elements items = doc.select("div.grid.gtc-f141a.gg-20.p-13.mh-77vh > div");

                String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);

                for (Element item : items) {
                    try {
                        Element titleLink = item.selectFirst("a[title]");
                        String title = titleLink != null ? titleLink.attr("title").trim() : "";
                        String url = titleLink != null ? titleLink.attr("href").trim() : "";
                        if (title.isEmpty() || url.isEmpty()) continue;

                        // ✅ Filter: only keep results that closely match the query
                        String normalizedTitle = title.toLowerCase(Locale.ROOT);
                        if (!normalizedTitle.contains(normalizedQuery)
                                && !normalizedQuery.contains(normalizedTitle)
                                && !normalizedTitle.replaceAll("\\s+", "")
                                .contains(normalizedQuery.replaceAll("\\s+", ""))) {
                            continue; // skip unrelated
                        }

                        // ✅ Manga ID (last part of URL)
                        String mangaId = url.replace("https://manhuaplus.org/manga/", "").trim();

                        // ✅ Cover image
                        Element img = item.selectFirst("img");
                        String cover = "";
                        if (img != null) {
                            if (img.hasAttr("data-src") && !img.attr("data-src").isEmpty()) {
                                String src = img.attr("data-src").trim();
                                cover = src.startsWith("http") ? src : "https://manhuaplus.org" + src;
                            } else if (img.hasAttr("src")) {
                                cover = img.attr("src").trim();
                            }
                        }

                        // ✅ Last chapter
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
                        m.setMangaId(mangaId);

                        results.add(m);


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

    public interface SearchCallback {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}
