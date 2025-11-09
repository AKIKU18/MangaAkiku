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
import java.util.Locale;

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
                String baseUrl = "https://asuracomic.net/";

                Elements mangas = doc.select("div.grid.grid-cols-2.sm\\:grid-cols-2.md\\:grid-cols-5.gap-3.p-4 a");

                String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);

                for (Element manga : mangas) {
                    String mangaUrl = baseUrl + manga.attr("href");

                    String href = manga.attr("href");
                    String mangaId = href.substring(href.lastIndexOf("-") + 1);

                    Element titleEl = manga.selectFirst("span.block.text-\\[13\\.3px\\].font-bold");
                    String title = titleEl != null ? titleEl.text() : "Unknown";

                    // ✅ Only include manga whose title closely matches the query
                    String normalizedTitle = title.toLowerCase(Locale.ROOT);
                    if (!normalizedTitle.contains(normalizedQuery) &&
                            !normalizedQuery.contains(normalizedTitle) &&
                            !normalizedTitle.replaceAll("\\s+", "").contains(normalizedQuery.replaceAll("\\s+", ""))) {
                        continue; // skip loosely related ones
                    }

                    Element imgEl = manga.selectFirst("img");
                    String coverImageUrl = imgEl != null ? imgEl.attr("src") : "";

                    Element chapterEl = manga.selectFirst("span.text-\\[13px\\].text-\\[\\#999\\]");
                    String lastChapter = chapterEl != null ? chapterEl.text() : "";

                    boolean isBookmarked = false;
                    String description = "";
                    String source = "AsuraScans";

                    results.add(new MangaItemModel(
                            mangaId, title, description, coverImageUrl, isBookmarked, mangaUrl, lastChapter, source
                    ));
                }

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

    public interface SearchCallback {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}
