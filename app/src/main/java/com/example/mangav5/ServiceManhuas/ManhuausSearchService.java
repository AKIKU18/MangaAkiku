package com.example.mangav5.ServiceManhuas;

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

public class ManhuausSearchService {
    private static final String TAG = "ManhuausSearch";

    public static void search(String query, SearchCallback callback) {
        new Thread(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String searchUrl = "https://manhuaus.com/?s=" + encodedQuery + "&post_type=wp-manga&op=&author=&artist=&release=&adult=";


                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(15000)
                        .get();

                List<MangaItemModel> results = new ArrayList<>();

                Elements items = doc.select("div.c-tabs-item__content"); // each manga result

                for (Element item : items) {
                    try {
                        // Title & URL
                        Element titleA = item.selectFirst(".post-title a");
                        String title = titleA != null ? titleA.text().trim() : "";
                        String url = titleA != null ? titleA.attr("href").trim() : "";

                        // Post ID from URL (last part of URL)
                        String mangaId =item.attr("data-post").trim();


                        // Cover image
                        Element img = item.selectFirst(".tab-thumb img");
                        String cover = "";
                        if (img != null) {
                            if (img.hasAttr("data-src") && !img.attr("data-src").isEmpty()) {
                                cover = img.attr("data-src").trim();
                            } else if (img.hasAttr("src")) {
                                cover = img.attr("src").trim();
                            }
                        }

                        // Last chapter
                        Element chapterA = item.selectFirst(".meta-item.latest-chap a");
                        String lastChapter = chapterA != null ? chapterA.text().trim() : "";

                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(mangaId);
                        m.setTitle(title);
                        m.setMangaUrl(url);
                        m.setCoverImageUrl(cover);
                        m.setLastChapter(lastChapter);
                        m.setDescription(""); // optional
                        m.setSource("Manhuaus");
                        results.add(m);
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed parsing single search item: " + ex.getMessage(), ex);
                    }
                }


                if (results.isEmpty()) {
                    callback.onError("No results found");
                } else {
                    callback.onSuccess(results);
                }

            } catch (Exception e) {
                Log.e(TAG, "Search failed: " + e.getMessage(), e);
                callback.onError("Failed to fetch results: " + e.getMessage());
            }
        }).start();
    }

    private static String firstUrlFromSrcset(String srcset) {
        if (srcset == null || srcset.isEmpty()) return "";
        String[] parts = srcset.split(",");
        if (parts.length == 0) return "";
        String first = parts[0].trim();
        String[] tokens = first.split("\\s+");
        return tokens.length > 0 ? tokens[0] : "";
    }

    public interface SearchCallback {
        void onSuccess(List<MangaItemModel> results);
        void onError(String error);
    }
}
