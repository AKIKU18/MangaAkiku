package com.example.mangav5.ServicesAsuraScans;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AsuraScansChapterPagesService {

    private static final String TAG = "ImagesScraper";
    private static final String JS_BRIDGE_NAME = "AndroidBridge";
    private static final int MAX_JS_CHECKS = 5;        // reduced from 10
    private static final int JS_CHECK_DELAY_MS = 1000; // increased delay
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> processedUrls = new HashSet<>();

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public void GetChapterPages(Context context, String chapterUrl, PagesCallback callback) {
        if (chapterUrl == null || chapterUrl.isEmpty()) {
            callback.onError("Chapter URL is empty");
            return;
        }

        Log.d(TAG, "Loading chapter URL: " + chapterUrl);

        WebView webView = new WebView(context);

        // ⚠️ Avoid GPU crash
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadsImagesAutomatically(false); // only load images via JS
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new JsBridge(callback), JS_BRIDGE_NAME);

        webView.setWebViewClient(new WebViewClient() {
            private int jsAttempts = 0;

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page finished loading: " + url);
                handler.postDelayed(() -> checkAndInject(view), JS_CHECK_DELAY_MS);
            }

            private void checkAndInject(WebView view) {
                jsAttempts++;
                Log.d(TAG, "JS injection attempt " + jsAttempts);
                injectCollectorScript(view);

                if (jsAttempts < MAX_JS_CHECKS) {
                    handler.postDelayed(() -> checkAndInject(view), JS_CHECK_DELAY_MS);
                } else {
                    Log.d(TAG, "Max JS attempts reached");
                }
            }
        });

        webView.loadUrl(chapterUrl);
    }

    private void injectCollectorScript(WebView view) {
        String js =
                "(function(){" +
                        "try{ " +
                        "  var arr=[];" +
                        "  document.querySelectorAll('img').forEach(function(el){ " +
                        "    var src = el.getAttribute('src') || el.getAttribute('data-src') || el.src;" +
                        "    if(src) arr.push(src);" +
                        "  });" +
                        "  var norm = arr.map(function(u){ " +
                        "    if(!u) return u;" +
                        "    if(u.indexOf('//')===0) return location.protocol + u;" +
                        "    if(u.indexOf('/')===0) return location.origin + u;" +
                        "    return u;" +
                        "  });" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".process){ " +
                        "    window." + JS_BRIDGE_NAME + ".process(JSON.stringify(norm)); " +
                        "  }" +
                        "}catch(e){console.error(e);} " +
                        "})();";
        view.evaluateJavascript(js, null);
    }

    public interface PagesCallback {
        void onSuccess(List<String> pages);
        void onError(String message);
    }

    private class JsBridge {
        private final PagesCallback callback;

        JsBridge(PagesCallback callback) {
            this.callback = callback;
        }

        @JavascriptInterface
        public void process(String json) {
            handler.post(() -> {
                if (json == null || json.length() < 2) {
                    Log.d(TAG, "JS returned empty JSON");
                    return;
                }

                try {
                    List<String> pages = new ArrayList<>();
                    JSONArray arr = new JSONArray(json);
                    Log.d(TAG, "JS returned " + arr.length() + " images");

                    for (int i = 0; i < arr.length(); i++) {
                        String src = arr.optString(i, "").trim();
                        if (!processedUrls.contains(src)) {
                            processedUrls.add(src);
                            if (src.contains("/storage/media/") || src.contains("/storage/comics/")) {
                                pages.add(src);
                                Log.d(TAG, "Matched comic image: " + src);
                            } else {
                                Log.d(TAG, "Skipped image: " + src);
                            }
                        }
                    }

                    if (!pages.isEmpty()) {
                        callback.onSuccess(pages);
                    } else {
                        Log.d(TAG, "No comic images matched yet, waiting...");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse JS JSON: " + e.getMessage());
                    callback.onError("Failed to parse JS JSON: " + e.getMessage());
                }
            });
        }
    }
}
