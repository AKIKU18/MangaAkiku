package com.example.mangav5.ServiceManhuaFast;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.ServiceManhuas.ManhuausChaptersService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManhuaFastChaptersService {
    private static final int TIMEOUT_MS = 60000; // 60 seconds
    private static final int MAX_RETRIES = 3;

    public static String generateChapterId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        String id = siteName + "-" + hash;
        return id;
    }
    public static void getChapterMangaManhuaFast(String chapterUrl, ManhuaFastChaptersService.ChapterCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    Document doc = Jsoup.connect(chapterUrl)
                            .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                            .timeout(TIMEOUT_MS)
                            .get();

                    // Select all chapter images
                    Elements images = doc.select("div.reading-content img.wp-manga-chapter-img");

                    List<String> imageUrls = new ArrayList<>();
                    for (Element img : images) {
                        String url = img.attr("data-src").trim(); // get data-src
                        if (!url.isEmpty()) {
                            imageUrls.add(url);
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(imageUrls));
                    return; // success, exit loop

                } catch (IOException e) {
                    attempts++;
                    if (attempts >= MAX_RETRIES) {
                        final String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        mainHandler.post(() -> callback.onError(msg));
                    }
                }
            }
        });
    }

    public static void getChaptersManhuaFast(Context context, String ajaxUrl, ChapterListCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {

        WebView webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
            // Add JS interface
            webView.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void processHTML(String html) {
                    try {
                        List<ChapterModel> chapters = new ArrayList<>();
                        Document doc = Jsoup.parse(html);
                        Elements chapterLinks = doc.select("li.wp-manga-chapter > a");

                        for (Element link : chapterLinks) {
                            String chapterTitle = link.text().trim();
                            String[] parts = chapterTitle.split(" ");
                            String chapterNumber = parts.length > 1 ? parts[1] : "0"; // safe fallback
                            String chapterId = generateChapterId(ajaxUrl, chapterTitle);
                            String chapterUrl = link.attr("href");
                            String source = "ManhuaFast";

                            ChapterModel chapter = new ChapterModel(chapterId, chapterTitle, chapterNumber, chapterUrl, source);
                            chapters.add(chapter);
                        }

                        mainHandler.post(() -> callback.onSuccess(chapters));

                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError(e.getMessage()));
                    }
                }
            }, "HtmlHandler");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    // Inject JS to get HTML content
                    webView.evaluateJavascript(
                            "(function() { HtmlHandler.processHTML(document.documentElement.outerHTML); })();",
                            null
                    );
                }
            });

            webView.loadUrl(ajaxUrl);
        });
    }

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);

        void onError(String message);
    }

    public interface ChapterCallback {
        void onSuccess(List<String> chapter);

        void onError(String message);
    }
}

