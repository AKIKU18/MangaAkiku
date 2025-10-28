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

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AsuraScansChapterPages {
    private static final String TAG = "ImagesScraper";
    private static final String JS_BRIDGE_NAME = "AndroidBridge";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> processedUrls = new HashSet<>();

    public interface PagesCallback {
        void onSuccess(List<String> pages);
        void onError(String message);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void GetChapterPages(Context context, String chapterUrl, PagesCallback callback) {
        if (chapterUrl == null || chapterUrl.isEmpty()) {
            callback.onError("Chapter URL is empty");
            return;
        }

        // Create a hidden WebView (not added to layout)
        WebView webView = new WebView(context);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadsImagesAutomatically(true);
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new JsBridge(callback), JS_BRIDGE_NAME);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Inject JS after page load
                handler.postDelayed(() -> injectCollectorScript(view), 500);
            }
        });

        webView.loadUrl(chapterUrl);
    }

    private void injectCollectorScript(WebView view) {
        String js =
                "(function(){" +
                        "  function collectAndSend(){ " +
                        "    try{ " +
                        "      var arr = [];" +
                        "      var imgs = document.querySelectorAll('img');" +
                        "      imgs.forEach(function(el){ " +
                        "        var src = el.getAttribute('src') || el.getAttribute('data-src') || el.src;" +
                        "        if(src) arr.push(src);" +
                        "      });" +
                        "      var norm = arr.map(function(u){ " +
                        "        if(!u) return u;" +
                        "        if(u.indexOf('//')===0) return location.protocol + u;" +
                        "        if(u.indexOf('/')===0) return location.origin + u;" +
                        "        return u;" +
                        "      });" +
                        "      var unique = Array.from(new Set(norm.filter(function(x){ return !!x; }))); " +
                        "      if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".process){ " +
                        "        window." + JS_BRIDGE_NAME + ".process(JSON.stringify(unique)); " +
                        "      }" +
                        "    }catch(e){} " +
                        "  }" +
                        "  collectAndSend();" +
                        "  setTimeout(collectAndSend, 500);" +
                        "  setTimeout(collectAndSend, 1200);" +
                        "})();";
        view.evaluateJavascript(js, null);
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
                    callback.onError("No images found");
                    return;
                }

                try {
                    List<String> pages = new ArrayList<>();
                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        String src = arr.optString(i, "").trim();
                        if (src.contains("gg.asuracomic.net/storage/media") &&
                                src.matches(".*/\\d{2}-optimized\\.webp$") &&
                                !processedUrls.contains(src)) {
                            processedUrls.add(src);
                            pages.add(src);
                            Log.i(TAG, "COMIC_IMG_URL: " + src);
                        }
                    }

                    if (pages.isEmpty()) {
                        callback.onError("No comic images matched");
                    } else {
                        callback.onSuccess(pages);
                    }

                } catch (Exception e) {
                    callback.onError("Failed to parse JS JSON: " + e.getMessage());
                }
            });
        }
    }
}
