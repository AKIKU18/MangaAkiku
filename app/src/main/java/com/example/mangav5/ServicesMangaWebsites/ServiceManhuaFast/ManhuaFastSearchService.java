package com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausFeedService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class ManhuaFastSearchService {
    private static final String TAG = "ManhuaFast Search";

    public static void search(String query, MangaListCallBack callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String searchUrl = "https://manhuafast.net/?s=" + encodedQuery + "&post_type=wp-manga&op=&author=&artist=&release=&adult=";

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                List<MangaItemModel> results = new ArrayList<>();
                Elements items = doc.select("div.c-tabs-item__content");

                List<MangaItemModel> tempResults = new ArrayList<>();

                for (Element item : items) {
                    try {
                        Element titleA = item.selectFirst(".post-title a");
                        String title = titleA != null ? titleA.text().trim() : "";
                        String url = titleA != null ? titleA.attr("href").trim() : "";
                        String mangaId = generateUuidHex(url);
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
                        m.setMangaId(mangaId);
                        m.setTitle(title);
                        m.setMangaUrl(url);
                        m.setCoverImageUrl(cover);
                        m.setLastChapter(lastChapter);
                        m.setDescription("");
                        m.setSource("ManhuaFast");
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
                    ManhuaFastFeedService.getMangaDetailsManhuaFast(manga.getMangaUrl(), new ManhuausFeedService.MangaCallback() {
                        @Override
                        public void onSuccess(MangaItemModel manga) {
                            manga.setMangaId(manga.getMangaId());
                            latch.countDown();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to fetch ID for " + manga.getTitle() + ": " + errorMessage);
                            latch.countDown();
                        }
                    });
                }

                // Wait for all threads to finish
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

    public static String extractSlug(String url) {
        String[] parts = url.replaceAll("/+$", "").split("/");
        return parts[parts.length - 1];
    }

    public static String normalizeSlug(String slug) {
        return slug.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public static String generateUuidHex(String url) {
        String slug = normalizeSlug(extractSlug(url));
        UUID uuid = UUID.nameUUIDFromBytes(slug.getBytes(StandardCharsets.UTF_8));
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        return Long.toHexString(msb) + Long.toHexString(lsb);
    }

    public interface MangaListCallBack {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}
