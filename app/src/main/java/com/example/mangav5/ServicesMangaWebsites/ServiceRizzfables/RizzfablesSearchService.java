package com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast.ManhuaFastFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaFast.ManhuaFastSearchService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausFeedService;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RizzfablesSearchService {

    private static final String TAG = "Rizzfables Search";
    private static final int TIMEOUT_MS = 60_000;

    public static void search(String query, MangaListCallBack callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // 1️⃣ Fetch the page
                Document doc = Jsoup.connect("https://rizzfables.com/series")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .get();

                // 2️⃣ Parse all manga
                Elements items = doc.select("div.listupd > div.bs > div.bsx");
                List<MangaItemModel> mangaList = new ArrayList<>();
                Map<String, MangaItemModel> mangaMap = new HashMap<>();

                for (Element item : items) {
                    try {
                        String title = item.selectFirst("a").attr("title");
                        String mangaUrl = item.selectFirst("a").attr("href");
                        String imageUrl = item.selectFirst("a > div.limit > img").attr("src");
                        String mangaId = generateUuidHex(mangaUrl);

                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(mangaId);
                        m.setTitle(title);
                        m.setMangaUrl(mangaUrl);
                        m.setCoverImageUrl(imageUrl);
                        m.setSource("Rizzfables");
                        mangaList.add(m);

                        // Normalize title and store in map for quick search
                        mangaMap.put(normalizeTitle(title), m);

                    } catch (Exception innerEx) {
                        Log.e(TAG, "Failed parsing item: " + innerEx.getMessage(), innerEx);
                    }
                }

                // 3️⃣ Partial search (case-insensitive, fast)
                String searchKey = normalizeTitle(query);
                List<MangaItemModel> searchResults = new ArrayList<>();

                for (Map.Entry<String, MangaItemModel> entry : mangaMap.entrySet()) {
                    if (entry.getKey().contains(searchKey)) {
                        searchResults.add(entry.getValue());
                    }
                }

                // 4️⃣ Return results on main thread
                mainHandler.post(() -> callback.onSuccess(searchResults));

            } catch (Exception e) {
                Log.e(TAG, "Search failed: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Failed to fetch results: " + e.getMessage()));
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

    private static String normalizeTitle(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]", "");
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
