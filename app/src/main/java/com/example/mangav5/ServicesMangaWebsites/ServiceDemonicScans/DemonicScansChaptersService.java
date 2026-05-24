package com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans;

import android.os.Handler;
import android.os.Looper;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.ScriptHelper.GenerateMangaIDHex;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class DemonicScansChaptersService {
    private static final String TAG = "DemonicScansService";
    public static void getChaptersDemonicScans(String mangaUrl, DemonicScansChaptersService.ChapterListCallback callback) {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                        .get();


                List<ChapterModel> chapters = new ArrayList<>();
                Elements containerChapters = doc.select("#chapters-list > li > a");
                for (Element containerChapter : containerChapters) {
                    String chapterUrl = "https://demonicscans.org" + containerChapter.attr("href");
                    // Using GenerateMangaIDHex for chapter IDs too for consistency if desired, 
                    // or keep it if it was using the same recipe.
                    String chapterId = GenerateMangaIDHex.generateUuidHex(chapterUrl);
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
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
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

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    public interface ChapterCallback {
        void onSuccess(List<String> chapterImages);
        void onError(String message);
    }
}
