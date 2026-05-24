package com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ScriptHelper.GenerateMangaIDHex;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausSearchService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class FlameComicsSearchService {
    private static final String TAG = "FlameComicsSearch";
    private static final int TIMEOUT_MS = 60000; // 60 seconds

    public static void search(String query, FlameComicsSearchService.MangaListCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String jsonUrl = "https://flamecomics.xyz/_next/data/VZEdwcuZVY5GW40mJJ_Nk/browse.json";

                Log.d(TAG, "Fetching FlameComics JSON");

                Document doc = Jsoup.connect(jsonUrl)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                String jsonString = doc.body().text();
                JSONObject jsonObject = new JSONObject(jsonString);

                JSONObject pageProps = jsonObject.getJSONObject("pageProps");
                JSONArray blocks = pageProps.getJSONArray("series");

                List<MangaItemModel> tempResults = new ArrayList<>();

                String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
                Set<String> urls = new HashSet<>();
                for (int i = 0; i < blocks.length(); i++) {

                    JSONObject series = blocks.getJSONObject(i);

                    String title = series.optString("title", "");
                    if (!title.toLowerCase().contains(normalizedQuery)) continue;

                    int seriesId = series.optInt("series_id", 0);
                    String mangaUrl = "https://flamecomics.xyz/series/" + seriesId;
                    if (seriesId <= 0) continue; // skip invalid series
                    Document imageCoverDoc = Jsoup.connect(mangaUrl)
                            .ignoreContentType(true)
                            .userAgent("Mozilla/5.0")
                            .timeout(15000)
                            .get();

                    String imageCover = "https://flamecomics.xyz" + imageCoverDoc.selectFirst("img.SeriesPage_cover__cEjW-").attr("src");
                    // evita duplicaturi
                    if (urls.contains(mangaUrl)) continue;
                    urls.add(mangaUrl);

                    // adaugi în listă
                    MangaItemModel item = new MangaItemModel();
                    item.setTitle(title);
                    item.setMangaId(GenerateMangaIDHex.generateUuidHex(mangaUrl));
                    item.setMangaUrl(mangaUrl);
                    item.setDescription("");
                    item.setCoverImageUrl(imageCover);
                    item.setSource("FlameComics");
                    tempResults.add(item);

                }


                if (tempResults.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("No results found."));
                    return;
                }

                // Return results on main thread
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onSuccess(tempResults));

            } catch (Exception e) {
                Log.e(TAG, "Search failed: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Unexpected error: " + e.getMessage()));
            }
        });
    }
    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}
