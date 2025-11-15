package com.example.mangav5.ServiceDemonicScans;

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
import java.util.UUID;
import java.util.concurrent.Executors;

public class DemonicScansSearchService {
    private static final String TAG = "DemonicScans";

    public static void search(String query, DemonicScansSearchService.MangaListCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String searchUrl = "https://demonicscans.org/search.php?manga=" + encodedQuery;
                Log.d(TAG, "🔍 Searching: " + searchUrl);

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                List<MangaItemModel> results = new ArrayList<>();

                for (Element a : doc.select("a")) {
                    try {
                        String url = "https://demonicscans.org" + a.attr("href");

                        Element img = a.selectFirst("img.search-thumb");
                        String cover = img != null ? img.attr("src") : "";
                        Element titleDiv = a.selectFirst("div.flex.flex-col > div:first-child");
                        String title = titleDiv != null ? titleDiv.text() : "";
                        Document lastChapterDoc = Jsoup.connect(url)
                                .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                                .timeout(15000)
                                .get();

                        String lastChapter = lastChapterDoc.selectFirst("#chapters-list > li:nth-child(1) > a").text();


                        if (!title.isEmpty() && !url.isEmpty()) {
                            results.add(new MangaItemModel(
                                    generateUuidHex(url),
                                    title,
                                    "",        // description empty
                                    cover,
                                    false,
                                    url,
                                    lastChapter,
                                    "DemonicScans"
                            ));
                        }


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

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);

        void onError(String message);
    }
}
