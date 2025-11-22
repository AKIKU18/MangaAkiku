package com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans;

import android.os.Handler;
import android.os.Looper;

import com.example.mangav5.Models.ChapterModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DemonicScansChaptersService {
    private static final String TAG = "DemonicScansService";
    public static void getChaptersDemonicScans(String mangaUrl, DemonicScansChaptersService.ChapterListCallback callback) {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(60000)
                        .get();

                List<ChapterModel> chapters = new ArrayList<>();
                Elements containerChapters = doc.select("#chapters-list > li > a");
                for (Element containerChapter : containerChapters) {
                    String chapterUrl = "https://demonicscans.org" + containerChapter.attr("href");
                    String chapterId = generateUuidHex(chapterUrl);
                    String chapterTitle = containerChapter.text();
                    String chapterNumber = chapterTitle.split(" ").length > 1 ? chapterTitle.split(" ")[1] : "0";

                    chapters.add(new ChapterModel(chapterId, chapterTitle, chapterNumber, chapterUrl, "DemonicScans"));
                }

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(chapters));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public static void getChapterDemonicScans(String chapterUrl, DemonicScansChaptersService.ChapterCallback callback) {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(chapterUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(60000)
                        .get();

                List<String> imageUrls = new ArrayList<>();

                Elements imgs = doc.select("img.imgholder");
                for (Element img : imgs) {
                    String src = img.attr("src").trim();
                    if (!src.isEmpty() && !src.equals("/img/free_ads.jpg")) {
                        imageUrls.add(src);
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(imageUrls));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
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

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    public interface ChapterCallback {
        void onSuccess(List<String> chapterImages);
        void onError(String message);
    }
}
