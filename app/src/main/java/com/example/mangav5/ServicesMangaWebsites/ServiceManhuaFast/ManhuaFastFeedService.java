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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManhuaFastFeedService {
    private static final String TAG = "ManhuausFeedService";
    private static final int TIMEOUT_MS = 60_000;

    /**
     * Fetch the homepage and parse visible items only (no AJAX).
     */
    public static void getMangaFeedManhuaFast(int offsetPage,ManhuaFastFeedService.MangaListCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();
            try {
                Document doc = Jsoup.connect("https://manhuafast.com/page" + offsetPage)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(TIMEOUT_MS)
                        .get();

                // Select listing items visible in the homepage (selectors based on provided HTML)
                // Each block: .page-item-detail (contains .item-summary, .item-thumb, etc.)
                Elements items = doc.select("#loop-content .page-listing-item .page-item-detail, .page-item-detail");

                for (Element item : items) {
                    try {
                        // Title and URL
                        Element titleA = item.selectFirst(".post-title a, h3.h5 a, .post-title.font-title a");
                        String title = titleA != null ? titleA.text().trim() : "";
                        String url = titleA != null ? titleA.attr("href").trim() : "";

                        //GetMangaId
                        Element thumbDiv = item.selectFirst(".item-thumb");
                        String mangaId = generateUuidHex(url);


                        // Cover image
                        Element img = item.selectFirst("img");
                        String cover = "";
                        if (img != null) {
                            if (img.hasAttr("data-src") && !img.attr("data-src").isEmpty()) {
                                cover = img.attr("data-src").trim();
                            } else if (img.hasAttr("data-srcset") && !img.attr("data-srcset").isEmpty()) {
                                cover = firstUrlFromSrcset(img.attr("data-srcset"));
                            } else if (img.hasAttr("src")) {
                                cover = img.attr("src").trim();
                            }
                        }

                        // Last chapter (first chapter-item link)
                        Element firstChapterItem = item.selectFirst(".list-chapter .chapter-item a");
                        String lastChapter = firstChapterItem != null ? firstChapterItem.text().trim() : "";

                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(mangaId);  // ✅ same as search
                        m.setTitle(title);
                        m.setCoverImageUrl(cover);
                        m.setMangaUrl(url);
                        m.setDescription("");
                        m.setLastChapter(lastChapter);
                        m.setSource("ManhuaFast");

                        mangaList.add(m);


                    } catch (Exception innerEx) {
                        Log.e(TAG, "Failed parsing single item: " + innerEx.getMessage(), innerEx);
                    }
                }


                // Return results on main thread
                mainHandler.post(() -> callback.onSuccess(mangaList));
            } catch (IOException e) {
                final String err = e.getMessage() != null ? e.getMessage() : "IO Error";
                Log.e(TAG, "Error fetching ManhuaFast homepage: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            } catch (Exception e) {
                final String err = e.getMessage() != null ? e.getMessage() : "Unknown Error";
                Log.e(TAG, "Unexpected error: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            }
        });
    }

    // helper: pick first URL from srcset string "url1 175w, url2 110w"
    private static String firstUrlFromSrcset(String srcset) {
        if (srcset == null || srcset.isEmpty()) return "";
        String[] parts = srcset.split(",");
        if (parts.length == 0) return "";
        String first = parts[0].trim();
        String[] tokens = first.split("\\s+");
        return tokens.length > 0 ? tokens[0] : "";
    }

    public static void getMangaDetailsManhuaFast(String mangaUrl, ManhuausFeedService.MangaCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(60_000)
                        .get();

                // Manga ID from bookmark button
                Element bookmarkEl = doc.selectFirst(".add-bookmark .wp-manga-action-button");
                String mangaId = generateUuidHex(mangaUrl);
                // Title
                Element titleElement = doc.selectFirst(".summary_image a img"); // the image alt can be used
                String title = titleElement != null ? titleElement.attr("alt").trim() : "No title";

                // Description (optional, use summary if exists)
                Element description = doc.selectFirst("div.summary__content.show-more"); // empty fallback
                String descText = description != null ? description.text().trim() : "";

                // Cover image
                Element coverElement = doc.selectFirst(".summary_image img");
                String cover = "";
                if (coverElement != null) {
                    if (coverElement.hasAttr("data-src") && !coverElement.attr("data-src").isEmpty())
                        cover = coverElement.attr("data-src").trim();
                    else if (coverElement.hasAttr("src")) cover = coverElement.attr("src").trim();
                }


                // Build MangaItemModel exactly like your constructor
                MangaItemModel manga = new MangaItemModel(
                        mangaId,
                        title,
                        descText,
                        cover,
                        false,
                        mangaUrl,
                        "",
                        "ManhuaFast"
                );

                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "IO Error"));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown Error"));
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

    // callback interface
    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);

        void onError(String message);
    }


    // Callback interface
    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);

        void onError(String errorMessage);
    }
}
