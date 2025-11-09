package com.example.mangav5.ServiceManhuas;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class ManhuausSearchService {
    private static final String TAG = "ManhuausSearch";

    public static void search(String query, SearchCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String searchUrl = "https://manhuaus.com/?s=" + encodedQuery + "&post_type=wp-manga&op=&author=&artist=&release=&adult=";

                Log.d(TAG, "Searching Manhuaus: " + searchUrl);

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                List<MangaItemModel> results = new ArrayList<>();
                Elements items = doc.select("div.c-tabs-item__content");

                List<MangaItemModel> tempResults = new ArrayList<>();
                String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);

                for (Element item : items) {
                    try {
                        Element titleA = item.selectFirst(".post-title a");
                        String title = titleA != null ? titleA.text().trim() : "";
                        String url = titleA != null ? titleA.attr("href").trim() : "";

                        // Skip invalid items
                        if (title.isEmpty() || url.isEmpty()) continue;

                        // ✅ Filter: only titles that closely match the query
                        String normalizedTitle = title.toLowerCase(Locale.ROOT);
                        if (!normalizedTitle.contains(normalizedQuery)
                                && !normalizedQuery.contains(normalizedTitle)
                                && !normalizedTitle.replaceAll("\\s+", "")
                                .contains(normalizedQuery.replaceAll("\\s+", ""))) {
                            continue; // skip unrelated
                        }

                        Element img = item.selectFirst(".tab-thumb img");
                        String cover = "";
                        if (img != null) {
                            if (img.hasAttr("data-src") && !img.attr("data-src").isEmpty()) {
                                cover = img.attr("data-src").trim();
                            } else if (img.hasAttr("src")) {
                                cover = img.attr("src").trim();
                            }
                        }

                        Element chapterA = item.selectFirst(".meta-item.latest-chap a");
                        String lastChapter = chapterA != null ? chapterA.text().trim() : "";

                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(""); // will fill later
                        m.setTitle(title);
                        m.setMangaUrl(url);
                        m.setCoverImageUrl(cover);
                        m.setLastChapter(lastChapter);
                        m.setDescription("");
                        m.setSource("Manhuaus");

                        tempResults.add(m);

                    } catch (Exception ex) {
                        Log.e(TAG, "Failed parsing search item: " + ex.getMessage(), ex);
                    }
                }

                // If nothing found
                if (tempResults.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("No results found"));
                    return;
                }

                // Fetch IDs for each result in parallel
                CountDownLatch latch = new CountDownLatch(tempResults.size());

                for (MangaItemModel manga : tempResults) {
                    ManhuausFeedService.getMangaDetailsManhuaus(manga.getMangaUrl(), new ManhuausFeedService.MangaCallback() {
                        @Override
                        public void onSuccess(MangaItemModel detailedManga) {
                            manga.setMangaId(detailedManga.getMangaId());
                            latch.countDown();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to fetch ID for " + manga.getTitle() + ": " + error);
                            latch.countDown();
                        }
                    });
                }

                latch.await();
                results.addAll(tempResults);

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(results));

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
