package com.example.mangav5.ServiceManhuas;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
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

                Document doc = null;
                int attempts = 0;
                while (attempts < 3 && doc == null) {
                    try {
                        doc = Jsoup.connect(searchUrl)
                                .userAgent("Mozilla/5.0")
                                .referrer("https://www.google.com")
                                .timeout(15000)
                                .get();
                    } catch (IOException e) {
                        attempts++;
                        if (attempts == 3) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onError("Connection failed after 3 attempts: " + e.getMessage()));
                        }
                    }
                }

                Elements items = doc.select("div.c-tabs-item__content");
                if (items.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("No results found."));
                    return;
                }

                List<MangaItemModel> tempResults = new ArrayList<>();
                String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);

                for (Element item : items) {
                    try {
                        Element titleA = item.selectFirst(".post-title a");
                        if (titleA == null) continue;

                        String title = titleA.text().trim();
                        String url = titleA.attr("href").trim();
                        if (title.isEmpty() || url.isEmpty()) continue;

                        String normalizedTitle = title.toLowerCase(Locale.ROOT);
                        if (!normalizedTitle.contains(normalizedQuery)
                                && !normalizedQuery.contains(normalizedTitle)
                                && !normalizedTitle.replaceAll("\\s+", "")
                                .contains(normalizedQuery.replaceAll("\\s+", ""))) {
                            continue;
                        }

                        Element img = item.selectFirst(".tab-thumb img");
                        String cover = img != null
                                ? (img.hasAttr("data-src") ? img.attr("data-src").trim() : img.attr("src").trim())
                                : "";

                        Element chapterA = item.selectFirst(".meta-item.latest-chap a");
                        String lastChapter = chapterA != null ? chapterA.text().trim() : "";

                        MangaItemModel m = new MangaItemModel();
                        m.setTitle(title);
                        m.setMangaUrl(url);
                        m.setCoverImageUrl(cover);
                        m.setLastChapter(lastChapter);
                        m.setDescription("");
                        m.setSource("Manhuaus");
                        tempResults.add(m);

                    } catch (Exception ex) {
                        Log.e(TAG, "Parsing error: " + ex.getMessage(), ex);
                    }
                }

                if (tempResults.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("No results found."));
                    return;
                }

                CountDownLatch latch = new CountDownLatch(tempResults.size());

                for (MangaItemModel manga : tempResults) {
                    try {
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
                    } catch (Exception ex) {
                        Log.e(TAG, "Async fetch crash prevented: " + ex.getMessage(), ex);
                        latch.countDown();
                    }
                }

                // Wait max 20s to avoid freezing
                boolean finished = latch.await(20, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) Log.w(TAG, "Timeout waiting for detail fetches.");

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(tempResults));

            } catch (Exception e) {
                Log.e(TAG, "Search failed: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Unexpected error: " + e.getMessage()));
            }
        });
    }


    public interface SearchCallback {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}
