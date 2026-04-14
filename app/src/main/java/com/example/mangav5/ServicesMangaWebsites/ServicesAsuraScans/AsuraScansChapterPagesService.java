package com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans;

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

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AsuraScansChapterPagesService {

    private static final String TAG = "AsuraPages";
    private static final String JS_BRIDGE_NAME = "AndroidBridge";
    private static final int MAX_CHECKS = 8;
    private static final int CHECK_DELAY_MS = 1200;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> processedUrls = new LinkedHashSet<>();

    private boolean successSent = false;
    private boolean finished = false;

    public interface PagesCallback {
        void onSuccess(List<String> pages);
        void onError(String message);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public void GetChapterPages(Context context, String chapterUrl, PagesCallback callback) {
        if (chapterUrl == null || chapterUrl.trim().isEmpty()) {
            callback.onError("Chapter URL is empty");
            return;
        }

        processedUrls.clear();
        successSent = false;
        finished = false;

        WebView webView = new WebView(context);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadsImagesAutomatically(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setUserAgentString(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/122.0.0.0 Safari/537.36"
        );

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new JsBridge(callback), JS_BRIDGE_NAME);

        webView.setWebViewClient(new WebViewClient() {
            private int attempts = 0;

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page finished: " + url);
                handler.postDelayed(() -> inspectPage(view, callback, attempts), CHECK_DELAY_MS);
            }

            private void inspectPage(WebView view, PagesCallback callback, int currentAttempts) {
                if (finished) return;

                String js =
                        "(function(){" +
                                "  try {" +
                                "    var bodyText = document.body ? document.body.innerText : '';" +
                                "    var hasPassword = !!document.querySelector('input[type=\"password\"]');" +
                                "    var isLogin = hasPassword || /log in to continue|sign in to read this chapter|sign in|log in/i.test(bodyText);" +
                                "    return JSON.stringify({" +
                                "      isLoginPage: isLogin," +
                                "      title: document.title || ''," +
                                "      imgCount: document.querySelectorAll('img').length" +
                                "    });" +
                                "  } catch (e) {" +
                                "    return JSON.stringify({" +
                                "      isLoginPage:false," +
                                "      error:String(e)" +
                                "    });" +
                                "  }" +
                                "})();";

                view.evaluateJavascript(js, result -> {
                    Log.d(TAG, "Inspect result: " + result);

                    if (finished) return;

                    if (result != null && result.contains("\"isLoginPage\":true")) {
                        finished = true;
                        callback.onError("Chapter requires login on AsuraScans");
                        return;
                    }

                    collectPages(view, callback);
                });
            }

            private void collectPages(WebView view, PagesCallback callback) {
                if (finished) return;

                attempts++;
                injectCollectorScript(view);

                if (attempts < MAX_CHECKS && !successSent) {
                    handler.postDelayed(() -> collectPages(view, callback), CHECK_DELAY_MS);
                } else if (!successSent && processedUrls.isEmpty()) {
                    finished = true;
                    callback.onError("No chapter pages found");
                }
            }
        });

        webView.loadUrl(chapterUrl);
    }

    private void injectCollectorScript(WebView view) {
        String js =
                "(function(){" +
                        "  try {" +
                        "    function normalize(url) {" +
                        "      if (!url) return '';" +
                        "      url = String(url).trim();" +
                        "      if (!url) return '';" +
                        "      if (url.indexOf('//') === 0) return location.protocol + url;" +
                        "      if (url.indexOf('/') === 0) return location.origin + url;" +
                        "      return url;" +
                        "    }" +

                        "    function pickFromSrcset(srcset) {" +
                        "      if (!srcset) return '';" +
                        "      var parts = srcset.split(',');" +
                        "      var last = parts[parts.length - 1].trim();" +
                        "      return last.split(' ')[0].trim();" +
                        "    }" +

                        "    var out = [];" +
                        "    var imgs = document.querySelectorAll('img');" +

                        "    imgs.forEach(function(img) {" +
                        "      var alt = (img.getAttribute('alt') || '').trim();" +
                        "      var cls = (img.getAttribute('class') || '').trim();" +

                        "      var src =" +
                        "        img.getAttribute('src') || " +
                        "        img.getAttribute('data-src') || " +
                        "        img.getAttribute('data-lazy-src') || " +
                        "        img.getAttribute('data-image') || " +
                        "        pickFromSrcset(img.getAttribute('srcset')) || " +
                        "        pickFromSrcset(img.getAttribute('data-srcset')) || " +
                        "        img.src || '';" +

                        "      src = normalize(src);" +

                        "      var looksLikePage =" +
                        "        /Page\\s+\\d+/i.test(alt) || " +
                        "        src.indexOf('/storage/media/') !== -1 || " +
                        "        src.indexOf('/storage/comics/') !== -1;" +

                        "      var looksLikeJunk =" +
                        "        /logo|icon|avatar|cover|thumb|thumbnail|banner/i.test(src) || " +
                        "        /logo|icon|avatar|cover|thumb|thumbnail|banner/i.test(alt) || " +
                        "        /logo|icon|avatar|cover|thumb|thumbnail|banner/i.test(cls);" +

                        "      if (src && looksLikePage && !looksLikeJunk) {" +
                        "        out.push(src);" +
                        "      }" +
                        "    });" +

                        "    if (window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".process) {" +
                        "      window." + JS_BRIDGE_NAME + ".process(JSON.stringify(out));" +
                        "    }" +
                        "  } catch (e) {" +
                        "    if (window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError) {" +
                        "      window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "    }" +
                        "  }" +
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
                if (finished) return;
                if (json == null || json.trim().length() < 2) return;

                try {
                    JSONArray arr = new JSONArray(json);

                    for (int i = 0; i < arr.length(); i++) {
                        String src = arr.optString(i, "").trim();
                        if (!src.isEmpty()) {
                            processedUrls.add(src);
                        }
                    }

                    Log.d(TAG, "Pages found: " + processedUrls.size());
                    for (String page : processedUrls) {
                        Log.d(TAG, "Page: " + page);
                    }

                    if (!processedUrls.isEmpty() && !successSent) {
                        successSent = true;
                        finished = true;
                        callback.onSuccess(new ArrayList<>(processedUrls));
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse JS JSON", e);
                    finished = true;
                    callback.onError("Failed to parse JS JSON: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void onJsError(String error) {
            Log.e(TAG, "JS error: " + error);
        }
    }
}