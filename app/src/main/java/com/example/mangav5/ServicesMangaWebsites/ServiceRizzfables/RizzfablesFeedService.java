package com.example.mangav5.ServicesMangaWebsites.ServiceRizzfables;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusFeedService;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RizzfablesFeedService {
    private static final String TAG = "RizzfablesFeedService";
    private static final int TIMEOUT_MS = 60_000;

    /**
     * Fetch homepage feed from Rizzfables
     */
    public static void getMangaFeedRizzfables(RizzfablesFeedService.MangaListCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        String websiteManga = "https://rizzfables.com/series";
        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(websiteManga)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(TIMEOUT_MS)
                        .get();

                // Each manga block in homepage
                Elements items = doc.select("div.listupd > div.bs > div.bsx"); // Update selector if site changes

                for (Element item : items) {
                    try {
                        String title = item.selectFirst("a").attr("title");
                        String mangaUrl = item.selectFirst("a").attr("href");
                        String imageUrl = item.selectFirst("a > div.limit > img").attr("src");
                        String mangaId = generateUuidHex(mangaUrl);



                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(mangaId);
                        m.setDescription("");
                        m.setTitle(title);
                        m.setLastChapter("");
                        m.setSource("Rizzfables");
                        m.setMangaUrl(mangaUrl);
                        m.setCoverImageUrl(imageUrl);
                        mangaList.add(m);

                    } catch (Exception innerEx) {
                        Log.e(TAG, "Failed parsing single item: " + innerEx.getMessage(), innerEx);
                    }
                }

                mainHandler.post(() -> callback.onSuccess(mangaList));

            } catch (IOException e) {
                final String err = e.getMessage() != null ? e.getMessage() : "IO Error";
                Log.e(TAG, "Error fetching Rizzfables homepage: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            } catch (Exception e) {
                final String err = e.getMessage() != null ? e.getMessage() : "Unknown Error";
                Log.e(TAG, "Unexpected error: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            }
        });
    }

    /**
     * Fetch manga details page from ManhuaPlus
     */
    public static void getMangaDetailsRizzfables(String mangaUrl, RizzfablesFeedService.MangaCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(TIMEOUT_MS)
                        .get();

                String mangaId = generateUuidHex(mangaUrl);
                String title = doc.selectFirst("h1.entry-title").text().trim();
                String desc =  doc.selectFirst("#description-container").text();
                String imageUrl = doc.selectFirst("#post-69001 > div.main-info > div.info-left.desktop > div > div.thumb > img").attr("src");
                String lastChapter = doc.selectFirst("#chapterlist > ul > li > div.chbox > div.eph-num > a").attr("href");
                String description = extractDescription(doc.html());

                Log.e("description", description);

                MangaItemModel manga = new MangaItemModel(
                        mangaId,
                        title,
                        description,
                        imageUrl,
                        false,
                        mangaUrl,
                        lastChapter,
                        "Rizzfables"
                );

                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "IO Error"));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown Error"));
            }
        });
    }


    public static String extractDescription(String html) {
        // Regex to capture everything inside the quotes of: var description = "...";
        // Fixed: Added \\s* before the semicolon to stop exactly at the end of the string,
        // even if there are spaces like: "content" ;
        Pattern pattern = Pattern.compile("var description\\s*=\\s*\"(.*?)\"\\s*;", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            String description = matcher.group(1);

            if (description != null) {
                // Safety check: if it still grabbed too much, cut it off manually at the next quote
                if (description.contains("\";")) {
                    description = description.split("\";")[0];
                }

                // 1. Unescape JavaScript/Unicode characters
                String unescaped = description
                        .replace("\\r\\n", "\n")     // Handle escaped carriage return + newline
                        .replace("\\n", "\n")        // Handle simple escaped newlines
                        .replace("\\\"", "\"")       // Unescape double quotes
                        .replace("\\'", "'")         // Unescape single quotes
                        .replace("\\u2026", "...")   // Unicode ellipsis (...)
                        .replace("\\u2019", "'")     // Unicode right single quote (apostrophe)
                        .replace("\\u201c", "\"")    // Unicode left double quote
                        .replace("\\u201d", "\"")    // Unicode right double quote
                        .replace("\\/", "/")         // Unescape slashes
                        .trim();

                // 2. Parse with Jsoup to remove any embedded HTML tags (like <br>, <b>)
                return Jsoup.parse(unescaped).text();
            }
        }

        return "No description available";
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

    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);
        void onError(String errorMessage);
    }
}
