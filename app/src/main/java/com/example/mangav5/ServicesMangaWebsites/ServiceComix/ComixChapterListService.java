package com.example.mangav5.ServicesMangaWebsites.ServiceComix;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Network.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ComixChapterListService {

    private static final String TAG = "ComixChapterList";
    private static final String JS_BRIDGE_NAME = "AndroidBridge";
    private static final OkHttpClient client = NetworkHelper.getOkHttpClient();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ChapterModel> chapterMap = new LinkedHashMap<>();
    private boolean finished = false;

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public void getChapterList(Context context, String mangaUrl, ChapterListCallback callback) {

        if (mangaUrl == null || mangaUrl.trim().isEmpty()) {
            callback.onError("Manga URL is empty");
            return;
        }

        // Try API first
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String slugPart = mangaUrl.substring(mangaUrl.lastIndexOf("/") + 1);
                String hashId = slugPart.contains("-") ? slugPart.split("-")[0] : slugPart;

                // Try v2 API
                String apiUrl = "https://comix.to/api/v2/manga/" + hashId + "/chapters?limit=500";
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .header("User-Agent", NetworkHelper.USER_AGENT)
                        .header("Accept", "application/json")
                        .header("Referer", mangaUrl)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful() || response.code() == 404) {
                    // Try v1 API fallback
                    apiUrl = "https://comix.to/api/v1/manga/" + hashId + "/chapters?limit=500";
                    request = new Request.Builder()
                            .url(apiUrl)
                            .header("User-Agent", NetworkHelper.USER_AGENT)
                            .header("Accept", "application/json")
                            .header("Referer", mangaUrl)
                            .build();
                    response = client.newCall(request).execute();
                }

                if (response.isSuccessful() && response.body() != null) {
                    JSONObject root = new JSONObject(response.body().string());
                    JSONObject result = root.optJSONObject("result");
                    if (result == null) result = root.optJSONObject("data");
                    
                    if (result != null) {
                        JSONArray items = result.optJSONArray("items");
                        if (items == null) items = root.optJSONArray("items");
                        
                        if (items != null) {
                            List<ChapterModel> chapters = new ArrayList<>();
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                String title = item.optString("title", "").trim();
                                String chapterId = item.optString("hash_id", item.optString("id", ""));
                                String chapterSlug = item.optString("slug", "");
                                String chapterNumber = item.optString("number", "");
                                
                                if (title.isEmpty()) title = "Chapter " + chapterNumber;
                                
                                String chapterUrl = mangaUrl + "/" + chapterId + "-" + chapterSlug;

                                chapters.add(new ChapterModel(
                                        chapterId, title, chapterNumber, chapterUrl, "Comix"
                                ));
                            }
                            handler.post(() -> callback.onSuccess(chapters));
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "API call failed, falling back to WebView: " + e.getMessage());
            }

            // Fallback to WebView
            handler.post(() -> startWebViewScraper(context, mangaUrl, callback));
        });
    }

    private void startWebViewScraper(Context context, String mangaUrl, ChapterListCallback callback) {
        String cleanUrl = mangaUrl.split("\\?")[0].trim();
        WebView webView = new WebView(context);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setUserAgentString(NetworkHelper.USER_AGENT);

        webView.addJavascriptInterface(new JsBridge(webView, callback), JS_BRIDGE_NAME);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (finished) return;
                handler.postDelayed(() -> scrapeAllChapters(view), 3000);
            }
        });
        webView.loadUrl(cleanUrl);
    }

    private void scrapeAllChapters(WebView webView) {
        String js =
                "(async function() {" +
                        "let collected = [];" +
                        "let seen = new Set();" +
                        "function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }" +
                        "function scrape() {" +
                        "  document.querySelectorAll('li.mchap-item').forEach(row => {" +
                        "    let a = row.querySelector('a.mchap-row__primary');" +
                        "    if (!a) return;" +
                        "    let href = a.href || '';" +
                        "    if (!href) return;" +
                        "    if (href.startsWith('/')) href = location.origin + href;" +
                        "    let ch = row.querySelector('.mchap-row__ch');" +
                        "    let titleExtra = row.querySelector('.mchap-row__title');" +
                        "    let title = (ch ? ch.innerText : '') + (titleExtra ? ' ' + titleExtra.innerText : '');" +
                        "    if (!seen.has(href)) {" +
                        "      seen.add(href);" +
                        "      collected.push({ title: title.trim(), url: href });" +
                        "    }" +
                        "  });" +
                        "}" +
                        "scrape();" +
                        "while (true) {" +
                        "  let next = document.querySelector('a[rel=next]') || document.querySelector('a.next');" +
                        "  if (!next) break;" +
                        "  let old = location.href;" +
                        "  next.click();" +
                        "  await sleep(2500);" +
                        "  let i = 0;" +
                        "  while (location.href === old && i < 20) { await sleep(500); i++; }" +
                        "  await sleep(1500);" +
                        "  scrape();" +
                        "}" +
                        "window." + JS_BRIDGE_NAME + ".onChaptersLoaded(JSON.stringify(collected));" +
                        "})();";
        webView.evaluateJavascript(js, null);
    }

    private String extractChapterNumber(String title, String url) {
        try {
            Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(title);
            if (m.find()) return m.group(1);
            m = Pattern.compile("chapter[- ]?(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE).matcher(url);
            if (m.find()) return m.group(1);
        } catch (Exception e) {
            Log.e(TAG, "extractChapterNumber error", e);
        }
        return "";
    }

    public static String generateChapterId(String url, String title) {
        String site = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        return site + "-" + (url + title).hashCode();
    }

    private class JsBridge {
        private final WebView webView;
        private final ChapterListCallback callback;

        JsBridge(WebView webView, ChapterListCallback callback) {
            this.webView = webView;
            this.callback = callback;
        }

        @JavascriptInterface
        public void onChaptersLoaded(String json) {
            handler.post(() -> {
                if (finished) return;
                try {
                    finished = true;
                    JSONArray arr = new JSONArray(json);
                    List<ChapterModel> chapters = new ArrayList<>();
                    chapterMap.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String title = obj.optString("title", "").trim();
                        String url = obj.optString("url", "").trim();
                        if (title.isEmpty() || url.isEmpty()) continue;
                        if (chapterMap.containsKey(url)) continue;

                        String number = extractChapterNumber(title, url);
                        String chapterId = generateChapterId(url, title);
                        ChapterModel chapter = new ChapterModel(chapterId, title, number, url, "Comix");
                        chapterMap.put(url, chapter);
                    }
                    chapters.addAll(chapterMap.values());
                    callback.onSuccess(chapters);
                    webView.destroy();
                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                    callback.onError(e.getMessage());
                    webView.destroy();
                }
            });
        }

        @JavascriptInterface
        public void onError(String error) {
            handler.post(() -> {
                if (finished) return;
                finished = true;
                callback.onError(error);
                webView.destroy();
            });
        }
    }
}
