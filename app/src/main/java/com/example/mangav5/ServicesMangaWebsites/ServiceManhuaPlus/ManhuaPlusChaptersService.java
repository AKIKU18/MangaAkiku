package com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.mangav5.Models.ChapterModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManhuaPlusChaptersService {
    private static final String TAG = "ManhuaPlusService";

    public static String generateChapterId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        return siteName + "-" + hash;
    }

    public static void getChaptersManhuaPlus(String mangaUrl, ChapterListCallback callback) {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(60000)
                        .get();

                List<ChapterModel> chapters = new ArrayList<>();
                Elements containerChapters = doc.select("ul#myUL li.chapter");

                for (Element containerChapter : containerChapters) {
                    String chapterTitle = containerChapter.selectFirst("a").text();
                    String chapterUrl = containerChapter.selectFirst("a").attr("href");
                    String chapterNumber = chapterTitle.split(" ").length > 1 ? chapterTitle.split(" ")[1] : "0";
                    String chapterId = generateChapterId(chapterUrl, chapterTitle);

                    chapters.add(new ChapterModel(chapterId, chapterTitle, chapterNumber, chapterUrl, "ManhuaPlus"));
                }

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(chapters));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    /**
     * Fetch chapter images using WebView + JS
     */
    public static void getChapterMangaManhuaPlus(Context context, String chapterUrl, ChapterCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        WebView webView = new WebView(context);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Android App; +https://myapp.example)");

        // JS Interface to return the image URLs
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void processHTML(String json) {
                mainHandler.post(() -> {
                    if (json == null || json.isEmpty()) {
                        callback.onError("No images found in JS");
                        return;
                    }
                    List<String> images = new ArrayList<>(Arrays.asList(json.split(",")));
                    callback.onSuccess(images);
                });
            }
        }, "HTMLOUT");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Run JS to extract images from #chapterContent
                String js = "javascript:(function() {" +
                        "var imgs = document.querySelectorAll('#chapterContent .separator img');" +
                        "var srcs = [];" +
                        "for(var i=0;i<imgs.length;i++){" +
                        "  var s = imgs[i].getAttribute('src') || imgs[i].getAttribute('data-src');" +
                        "  if(s) srcs.push(s);" +
                        "}" +
                        "window.HTMLOUT.processHTML(srcs.join(','));" +
                        "})()";
                view.evaluateJavascript(js, null);
            }
        });

        webView.loadUrl(chapterUrl);
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
