package com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ScriptHelper.GenerateMangaIDHex;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AsuraScansSearchService {

    private static final String TAG = "AsuraSearch";
    private static final String BASE_API = "https://api.asurascans.com/api/series";
    private static final String BASE_SITE = "https://asurascans.com";

    public static void search(String query, MangaListCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8.toString());

                String url = BASE_API
                        + "?search=" + encodedQuery
                        + "&sort=latest"
                        + "&order=desc"
                        + "&limit=20"
                        + "&offset=0";

                Log.d(TAG, "Searching API URL: " + url);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        .addHeader("Accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    String msg = "HTTP " + response.code();
                    Log.e(TAG, "Search failed: " + msg);
                    mainHandler.post(() -> callback.onError(msg));
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Raw JSON: " + body);

                JSONObject root = new JSONObject(body);
                JSONArray data = root.optJSONArray("data");

                List<MangaItemModel> results = new ArrayList<>();

                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);

                        String title = item.optString("title", "Unknown");
                        String description = item.optString("description", "");
                        String coverUrl = item.optString("cover", "");

                        String publicUrl = item.optString("public_url", "");
                        String mangaUrl = publicUrl.startsWith("http")
                                ? publicUrl
                                : BASE_SITE + publicUrl;

                        String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);

                        String lastChapter = "";
                        JSONArray latestChapters = item.optJSONArray("latest_chapters");
                        if (latestChapters != null && latestChapters.length() > 0) {
                            JSONObject latest = latestChapters.getJSONObject(0);

                            if (latest.has("number")) {
                                lastChapter = "Chapter " + latest.optString("number", "");
                            } else if (latest.has("slug")) {
                                lastChapter = latest.optString("slug", "");
                            }
                        }

                        MangaItemModel manga = new MangaItemModel(
                                mangaId,
                                title,
                                description,
                                coverUrl,
                                false,
                                mangaUrl,
                                lastChapter,
                                "AsuraScans"
                        );

                        results.add(manga);

                        Log.d(TAG, "Found manga -> id=" + mangaId
                                + " | title=" + title
                                + " | url=" + mangaUrl
                                + " | cover=" + coverUrl
                                + " | lastChapter=" + lastChapter);
                    }
                }

                if (results.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No results found"));
                } else {
                    mainHandler.post(() -> callback.onSuccess(results));
                }

            } catch (Exception e) {
                Log.e(TAG, "Search failed", e);
                mainHandler.post(() -> callback.onError("Failed to fetch results: " + e.getMessage()));
            }
        }).start();
    }

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}