package com.example.mangav5.ServicesMangaWebsites.ServiceMgeko;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ScriptHelper.GenerateMangaIDHex;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusSearchService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MgekoSearchService {
    private static final String TAG = "MgekoSearchService";
    private static final int TIMEOUT_MS = 15000;

    public static void search(String query, MangaListCallBack callback) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String searchUrl = "https://www.mgeko.cc/search/?search=" + encodedQuery;

                Log.d(TAG, "Searching: " + searchUrl);

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Android App)")
                        .timeout(TIMEOUT_MS)
                        .get();

                List<MangaItemModel> results = new ArrayList<>();

                // ✅ Each manga card
                Elements items = doc.select("li.novel-item");

                for (Element item : items) {
                    try {
                        Element link = item.selectFirst("a[href]");
                        if (link == null) continue;

                        // ✅ Title
                        Element titleEl = item.selectFirst("h4.novel-title");
                        String title = titleEl != null ? titleEl.text().trim() : "";

                        // ✅ Manga URL
                        String mangaUrl = "https://www.mgeko.cc" + link.attr("href");

                        // ✅ Manga ID
                        String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);

                        // Cover
                        Element img = item.selectFirst(".novel-cover img");

                        String cover = "";
                        if (img != null) {
                            if (img.hasAttr("data-src")) {
                                cover = img.attr("data-src").trim();
                            } else if (img.hasAttr("data-original")) {
                                cover = img.attr("data-original").trim();
                            } else {
                                cover = img.attr("src").trim(); // fallback
                            }

                            if (!cover.startsWith("http")) {
                                cover = "https://www.mgeko.cc" + cover;
                            }
                        }

                        // ✅ DESCRIPTION (FULL, NOT TRUNCATED)
                        Element descEl = item.selectFirst("div.summary");
                        String description = descEl != null
                                ? descEl.attr("title").trim()
                                : "";

                        // ✅ Last chapter (optional)
                        Element chapterEl = item.selectFirst(".novel-stats strong");
                        String lastChapter = chapterEl != null
                                ? chapterEl.text().trim()
                                : "";

                        MangaItemModel manga = new MangaItemModel();
                        manga.setTitle(title);
                        manga.setMangaUrl(mangaUrl);
                        manga.setCoverImageUrl(cover);
                        manga.setDescription(description);
                        manga.setLastChapter(lastChapter);
                        manga.setSource("Mgeko");
                        manga.setMangaId(mangaId);

                        results.add(manga);

                    } catch (Exception inner) {
                        Log.e(TAG, "Parse error: " + inner.getMessage());
                    }
                }

                Handler mainHandler = new Handler(Looper.getMainLooper());
                if (results.isEmpty()) {
                    mainHandler.post(() ->
                            callback.onError("No results found"));
                } else {
                    mainHandler.post(() ->
                            callback.onSuccess(results));
                }

            } catch (Exception e) {
                Log.e(TAG, "Search failed", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e.getMessage()));
            }
        });
    }

    public interface MangaListCallBack {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}
