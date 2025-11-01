package com.example.mangav5.ServicesAsuraScans;

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

public class AsuraScansSearchService {
    private static final String TAG = "AsuraSearch";

    public static void search(String query, SearchCallback callback) {
        new Thread(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String searchUrl = "https://asuracomic.net/series?page=1&name=" + encodedQuery;

                Log.d(TAG, "Searching URL: " + searchUrl);

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(15000)
                        .get();

                List<MangaItemModel> results = new ArrayList<>();
                // Each manga is inside this grid container
                String baseUrl = "https://asuracomic.net/";

                // Select each manga block (anchor <a> inside the grid)
                Elements mangas = doc.select("div.grid.grid-cols-2.sm\\:grid-cols-2.md\\:grid-cols-5.gap-3.p-4 a");

                for (Element manga : mangas) {
                    // ✅ Manga URL (relative -> full)
                    String mangaUrl = baseUrl + manga.attr("href");

                    // ✅ Manga ID (last part of href, e.g., "nano-machine-42c424da")
                    String href = manga.attr("href");
                    String mangaId = href.substring(href.lastIndexOf("-") + 1);

                    // ✅ Title
                    Element titleEl = manga.selectFirst("span.block.text-\\[13\\.3px\\].font-bold");
                    String title = titleEl != null ? titleEl.text() : "Unknown";

                    // ✅ Description (none available in search results → keep blank)
                    String description = "";

                    // ✅ Cover Image
                    Element imgEl = manga.selectFirst("img");
                    String coverImageUrl = imgEl != null ? imgEl.attr("src") : "";

                    // ✅ Last Chapter (e.g., "Chapter 284")
                    Element chapterEl = manga.selectFirst("span.text-\\[13px\\].text-\\[\\#999\\]");
                    String lastChapter = chapterEl != null ? chapterEl.text() : "";

                    // ✅ Bookmark status (default false)
                    boolean isBookmarked = false;

                    // ✅ Source name
                    String source = "AsuraScans";

                    results.add(new MangaItemModel(mangaId, title, description, coverImageUrl, isBookmarked, mangaUrl, lastChapter,source));
                }


                callback.onSuccess(results);

                if (results.isEmpty()) {
                    callback.onError("No results found");
                } else {
                    callback.onSuccess(results);
                }

            } catch (Exception e) {
                Log.e(TAG, "Search failed: " + e.getMessage(), e);
                callback.onError("Failed to fetch results: " + e.getMessage());
            }
        }).start();
    }

    public static interface SearchCallback {
        void onSuccess(List<MangaItemModel> results);

        void onError(String error);
    }
}
