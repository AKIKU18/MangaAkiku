package com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.mangav5.Models.MangaItemModel;

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

public class DemonicScansFeedService {
    private static final String TAG = "DemonicScansFeedService";
    public static void getMangaFeedDemonicScans(int offsetPage,DemonicScansFeedService.MangaListCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        String websiteManga = "https://demonicscans.org/lastupdates.php?list=" + offsetPage;
        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(websiteManga)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Accept-Encoding", "gzip, deflate, br, zstd")
                        .header("Cache-Control", "max-age=0")
                        .header("Referer", "https://demonicscans.org/")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "same-origin")
                        .header("Sec-Fetch-User", "?1")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("sec-ch-ua", "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Opera\";v=\"124\"")
                        .header("sec-ch-ua-mobile", "?0")
                        .header("sec-ch-ua-platform", "\"Windows\"")
                        .cookie("cf_clearance", "CAMcc50dGwhPJlCQMp7eemEumBqKX5v6Kc110d8y0sw-1764098118-1.2.1.1-YQL0bswN.h0GgBmlvzzrioQ_PHja4hbRG4OkF8ntooyTTrULRlalhhHgBhsCRuWhgP9jVcORy2cn0SoNjzrCWilkkfBzHEDXu1rXOQKC7r42G3SF4AnbgpCrP5oNyM5Juzyi9IErBJbjinrGNNlNItJRmbHO_XiSxfHN7ZqU1.jS8Au.wY4CGokzWRHfs4RpJScLJijRsaj.HCi1SO7LU4Z8Z2bXSacFdE1oq31hcMM")
                        .cookie("_ga", "GA1.1.1334789206.1763192923")
                        .cookie("subdemon", "1")
                        .get();


                // Each manga block in homepage
                Elements items = doc.select("div.updates-element.border-box");
                for (Element item : items) {
                    try {
                        // Title and URL
                        Element divthumb = item.selectFirst("div.thumb");
                        String title = divthumb.selectFirst("a").attr("title");
                        String mangaUrl = "https://demonicscans.org/" + divthumb.selectFirst("a").attr("href");
                        String mangaId = generateUuidHex("https://demonicscans.org"+mangaUrl);
                        String coverImg = divthumb.selectFirst("img").attr("src");

                        Element chapterlink = item.selectFirst("div.flex.flex-row.chap-date.justify-space-between");
                        String lastChapter = "-";

                        if (chapterlink != null) {
                            Element ch = chapterlink.selectFirst("a");
                            if (ch != null) lastChapter = ch.text();
                        }

                        if(!title.equals("")){
                            MangaItemModel m = new MangaItemModel();

                            m.setMangaId(mangaId);
                            m.setTitle(title);
                            m.setCoverImageUrl(coverImg);
                            m.setMangaUrl(mangaUrl);
                            m.setDescription("");
                            m.setLastChapter(lastChapter);
                            m.setSource("DemonicScans");

                            mangaList.add(m);
                        }
                    } catch (Exception innerEx) {
                        Log.e(TAG, "Failed parsing single item: " + innerEx.getMessage(), innerEx);
                    }
                }
                mainHandler.post(() -> callback.onSuccess(mangaList));

            } catch (IOException e) {
                final String err = e.getMessage() != null ? e.getMessage() : "IO Error";
                Log.e(TAG, "Error fetching DemonicScans homepage: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            } catch (Exception e) {
                final String err = e.getMessage() != null ? e.getMessage() : "Unknown Error";
                Log.e(TAG, "Unexpected error: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            }
        });
    }

    public static void getMangaDetailsDemonicScans(String mangaUrl, DemonicScansFeedService.MangaCallback callback){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36")
                        .referrer("https://demonicscans.org/")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.5")
                        .header("Connection", "keep-alive")
                        .timeout(60_000)
                        .get();

                // Manga Details
                Element mangaPage = doc.selectFirst("#manga-page");

                Element img = mangaPage != null ? mangaPage.selectFirst("img") : null;
                Element descEle = doc.selectFirst("div.white-font");
                Element lastChapterEle = doc.selectFirst("#chapters-list > li:nth-child(1) > a");

                String mangaId = generateUuidHex(mangaUrl);
                String title = img != null ? img.attr("alt") : "No title";
                String coverImg = img != null ? img.attr("src") : "";
                String description = descEle !=null ? descEle.text(): "No description";
                String lastChapter = lastChapterEle != null ? lastChapterEle.text() : "No last chapter";
                MangaItemModel mangaItem = new MangaItemModel(
                        mangaId,
                        title,
                        description,
                        coverImg,
                        false,
                        mangaUrl,
                        lastChapter,
                        "DemonicScans"
                );



                mainHandler.post(() -> callback.onSuccess(mangaItem));

            } catch (IOException e) {
                final String err = e.getMessage() != null ? e.getMessage() : "IO Error";
                Log.e(TAG, "Error fetching DemonicScans homepage: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            } catch (Exception e) {
                final String err = e.getMessage() != null ? e.getMessage() : "Unknown Error";
                Log.e(TAG, "Unexpected error: " + err, e);
                mainHandler.post(() -> callback.onError(err));
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

    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);
        void onError(String errorMessage);
    }
}
