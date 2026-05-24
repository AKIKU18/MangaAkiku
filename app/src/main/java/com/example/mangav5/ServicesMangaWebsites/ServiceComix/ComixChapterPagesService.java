package com.example.mangav5.ServicesMangaWebsites.ServiceComix;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.mangav5.Network.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ComixChapterPagesService {

    private static final String TAG = "ComixPagesService";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final OkHttpClient client = NetworkHelper.getOkHttpClient();

    public interface PagesCallback {
        void onSuccess(List<String> pages);
        void onError(String message);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void getChapterPages(Context context, String chapterUrl, PagesCallback callback) {

        // Try API first
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // chapterUrl example: https://comix.to/title/hashId-slug/chapterId-chapterSlug
                String[] parts = chapterUrl.split("/");
                String lastPart = parts[parts.length - 1];
                String chapterId = lastPart.contains("-") ? lastPart.split("-")[0] : lastPart;

                // Try v2 API
                String apiUrl = "https://comix.to/api/v2/chapter/" + chapterId;
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .header("User-Agent", NetworkHelper.USER_AGENT)
                        .header("Accept", "application/json")
                        .header("Referer", chapterUrl)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful() || response.code() == 404) {
                    // Try v1 API fallback
                    apiUrl = "https://comix.to/api/v1/chapter/" + chapterId;
                    request = new Request.Builder()
                            .url(apiUrl)
                            .header("User-Agent", NetworkHelper.USER_AGENT)
                            .header("Accept", "application/json")
                            .header("Referer", chapterUrl)
                            .build();
                    response = client.newCall(request).execute();
                }

                if (response.isSuccessful() && response.body() != null) {
                    JSONObject root = new JSONObject(response.body().string());
                    JSONObject result = root.optJSONObject("result");
                    if (result == null) result = root.optJSONObject("data");
                    
                    if (result != null) {
                        JSONArray images = result.optJSONArray("images");
                        if (images != null) {
                            List<String> pages = new ArrayList<>();
                            for (int i = 0; i < images.length(); i++) {
                                pages.add(images.getString(i));
                            }
                            handler.post(() -> callback.onSuccess(pages));
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "API call failed, falling back to WebView: " + e.getMessage());
            }

            // Fallback to WebView
            handler.post(() -> startWebViewScraper(context, chapterUrl, callback));
        });
    }

    private void startWebViewScraper(Context context, String chapterUrl, PagesCallback callback) {
        handler.post(() -> {
            WebView webView = new WebView(context);
            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setUserAgentString(NetworkHelper.USER_AGENT);

            webView.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void onData(String json) {
                    handler.post(() -> {
                        try {
                            JSONObject obj = new JSONObject(json);
                            if (obj.has("error")) {
                                callback.onError(obj.getString("error"));
                                webView.destroy();
                                return;
                            }
                            int total = obj.getInt("total");
                            String firstUrl = obj.getString("firstUrl");
                            List<String> pages = generatePages(firstUrl, total);
                            callback.onSuccess(pages);
                            webView.destroy();
                        } catch (Exception e) {
                            callback.onError(e.getMessage());
                            webView.destroy();
                        }
                    });
                }
            }, "AndroidBridge");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    handler.postDelayed(() -> {
                        String js =
                                "(function() {" +
                                        "try {" +
                                        "let total = 0;" +
                                        "document.querySelectorAll('.rpage-page__counter').forEach(e => {" +
                                        "  let txt = e.innerText;" +
                                        "  let m = txt.match(/\\/\\s*(\\d+)/);" +
                                        "  if (m) total = Math.max(total, parseInt(m[1]));" +
                                        "});" +
                                        "if (!total) total = document.querySelectorAll('.rpage-page').length;" +
                                        "let img = document.querySelector('.rpage-page img');" +
                                        "let firstUrl = img ? (img.src || img.getAttribute('data-src')) : '';" +
                                        "window.AndroidBridge.onData(JSON.stringify({total: total, firstUrl: firstUrl}));" +
                                        "} catch(e) {" +
                                        "window.AndroidBridge.onData(JSON.stringify({error: e.toString()}));" +
                                        "}" +
                                        "})();";
                        view.evaluateJavascript(js, null);
                    }, 2500);
                }
            });
            webView.loadUrl(chapterUrl);
        });
    }

    private List<String> generatePages(String firstUrl, int total) {
        List<String> pages = new ArrayList<>();
        try {
            int lastSlash = firstUrl.lastIndexOf("/");
            int lastDot = firstUrl.lastIndexOf(".");
            String basePath = firstUrl.substring(0, lastSlash + 1);
            String extension = firstUrl.substring(lastDot);
            for (int i = 1; i <= total; i++) {
                String num = (i < 10) ? "0" + i : String.valueOf(i);
                pages.add(basePath + num + extension);
            }
        } catch (Exception e) {
            pages.add(firstUrl);
        }
        return pages;
    }
}
