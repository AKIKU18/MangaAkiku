package com.example.mangav5.ServicesMangaWebsites.VortexScans;

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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VortexScansSearchService {

    private static final String TAG = "VortexSearch";
    private static final String BASE_URL = "https://vortexscans.org";

    public static void search(String query, MangaListCallback callback) {

        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {

                String encodedQuery = URLEncoder.encode(
                        query == null ? "" : query.trim(),
                        StandardCharsets.UTF_8.toString()
                );

                String url = BASE_URL + "/series?searchTerm=" + encodedQuery;

                Log.d(TAG, "Search URL: " + url);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    mainHandler.post(() ->
                            callback.onError("HTTP " + response.code()));
                    return;
                }

                String html = response.body() != null
                        ? response.body().string()
                        : "";

                Document doc = Jsoup.parse(html);

                List<MangaItemModel> results = new ArrayList<>();

                // ✅ EACH MANGA CARD
                Elements cards = doc.select("div.relative.h-full.p-1");

                for (Element card : cards) {

                    Element mangaLink = card.selectFirst("a[href^=/series/][title]");
                    if (mangaLink == null) continue;

                    String title = mangaLink.attr("title").trim();
                    String href = mangaLink.attr("href");
                    String mangaUrl = BASE_URL + href;
                    // cover image
                    Element img = mangaLink.selectFirst("img");
                    String cover = img != null ? img.attr("src") : "";

                    // last chapter (safe)
                    String lastChapter = "";
                    Element chapter = card.selectFirst("a[href*='/chapter-']");
                    if (chapter != null) {
                        lastChapter = chapter.text(); // "Chapter 56"
                    }

                    String id = mangaUrl;

                    MangaItemModel manga = new MangaItemModel(
                            id,
                            title,
                            "",
                            cover,
                            false,
                            mangaUrl,
                            lastChapter,
                            "VortexScans"
                    );

                    results.add(manga);

                    Log.d(TAG, "Found: " + title + " | " + lastChapter);
                }

                if (results.isEmpty()) {
                    mainHandler.post(() ->
                            callback.onError("No results found"));
                } else {
                    mainHandler.post(() ->
                            callback.onSuccess(results));
                }

            } catch (Exception e) {
                Log.e(TAG, "Search error", e);
                mainHandler.post(() ->
                        callback.onError(e.getMessage()));
            }
        }).start();
    }

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}