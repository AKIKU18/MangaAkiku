package com.example.mangav5.ServicesMangaWebsites.ServiceComix;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ComixSearchService {
    private static final String TAG = "ComixSearch";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    public static void search(String query, MangaListCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String trimmedQuery = query == null ? "" : query.trim();

                if (trimmedQuery.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Search query is empty."));
                    return;
                }

                String encodedQuery = URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8.toString());

                String searchUrl = "https://comix.to/api/v2/manga" +
                        "?order[relevance]=desc" +
                        "&keyword=" + encodedQuery +
                        "&genres_mode=and";

                Log.d(TAG, "Searching: " + searchUrl);

                Request request = new Request.Builder()
                        .url(searchUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Referer", "https://comix.to/")
                        .header("Origin", "https://comix.to")
                        .header("Accept-Language", "en-GB,en;q=0.9")
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "HTTP code: " + response.code());
                Log.d(TAG, "Body: " + body);

                if (!response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Comix blocked the request. HTTP " + response.code()));
                    return;
                }

                List<MangaItemModel> results = new ArrayList<>();

                JSONObject root = new JSONObject(body);
                JSONArray itemsArray = null;

                if (root.has("result")) {
                    JSONObject resultObj = root.optJSONObject("result");
                    if (resultObj != null) {
                        itemsArray = resultObj.optJSONArray("items");
                    }
                }

                if (itemsArray == null && root.has("data")) {
                    itemsArray = root.optJSONArray("data");
                }

                if (itemsArray == null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("No results found."));
                    return;
                }

                for (int i = 0; i < itemsArray.length(); i++) {
                    try {
                        JSONObject item = itemsArray.getJSONObject(i);

                        String hashId = item.optString("hash_id", item.optString("id", ""));
                        String slug = item.optString("slug", "");
                        String title = item.optString("title", "");
                        String description = item.optString("synopsis", item.optString("description", ""));
                        String lastChapter = item.optString("latest_chapter", "");

                        // FILTRU STRICT:
                        // toate cuvintele din query trebuie sa existe in title
                        if (!matchesSearchWords(trimmedQuery, title)) {
                            Log.d(TAG, "Skipped (title mismatch): " + title);
                            continue;
                        }

                        String cover = "";
                        JSONObject poster = item.optJSONObject("poster");
                        if (poster != null) {
                            cover = poster.optString("large",
                                    poster.optString("medium",
                                            poster.optString("small", "")));
                        }

                        if (cover.isEmpty()) {
                            cover = item.optString("cover", "");
                        }

                        String mangaUrl = "";
                        if (!hashId.isEmpty() && !slug.isEmpty()) {
                            mangaUrl = "https://comix.to/title/" + hashId + "-" + slug;
                        } else if (!slug.isEmpty()) {
                            mangaUrl = "https://comix.to/title/" + slug;
                        }

                        if (!title.isEmpty() && !mangaUrl.isEmpty()) {
                            results.add(new MangaItemModel(
                                    generateUuidHex(mangaUrl),
                                    title,
                                    description,
                                    cover,
                                    false,
                                    mangaUrl,
                                    lastChapter,
                                    "Comix"
                            ));
                        }

                    } catch (Exception innerEx) {
                        Log.e(TAG, "Error parsing item: " + innerEx.getMessage(), innerEx);
                    }
                }

                Handler mainHandler = new Handler(Looper.getMainLooper());
                if (results.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No matching results found."));
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

    private static boolean matchesSearchWords(String query, String title) {
        String normalizedQuery = normalizeText(query);
        String normalizedTitle = normalizeText(title);

        String[] words = normalizedQuery.split("\\s+");
        for (String word : words) {
            if (word.isEmpty()) continue;

            // ignora cuvinte foarte scurte daca vrei
            if (word.length() < 2) continue;

            if (!normalizedTitle.contains(word)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeText(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
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