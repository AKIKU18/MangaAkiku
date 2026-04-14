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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ComixChapterPagesService {

    private static final String TAG = "ComixChapterPages";
    private static final String JS_BRIDGE_NAME = "AndroidBridge";

    private static final int INITIAL_DELAY_MS = 1800;
    private static final int STEP_DELAY_MS = 1200;
    private static final int MAX_PAGE_VISITS = 250;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> pageSet = new LinkedHashSet<>();

    private boolean started = false;
    private boolean finished = false;

    private int expectedPages = 0;
    private int currentPageIndex = 0;
    private int stableHits = 0;
    private int lastPageCount = 0;

    public interface PagesCallback {
        void onSuccess(List<String> pages);
        void onError(String message);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public void getChapterPages(Context context, String chapterUrl, PagesCallback callback) {
        if (chapterUrl == null || chapterUrl.trim().isEmpty()) {
            postError(callback, "Chapter URL is empty");
            return;
        }

        String cleanUrl = chapterUrl.split("\\?")[0].trim();

        pageSet.clear();
        started = false;
        finished = false;
        expectedPages = 0;
        currentPageIndex = 0;
        stableHits = 0;
        lastPageCount = 0;

        handler.post(() -> {
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

            webView.addJavascriptInterface(new JsBridge(webView, callback), JS_BRIDGE_NAME);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "Page finished: " + url);

                    if (started || finished) return;
                    started = true;

                    handler.postDelayed(() -> inspectAndStart(view, callback), INITIAL_DELAY_MS);
                }
            });

            webView.loadUrl(cleanUrl);
        });
    }

    private void inspectAndStart(WebView view, PagesCallback callback) {
        if (finished) return;

        String js =
                "(function(){" +
                        "try{" +
                        "  var text = document.body ? document.body.innerText : '';" +
                        "  var isBlocked = /cloudflare|just a moment|verify you are human/i.test(text);" +
                        "  var viewer = document.querySelector('.read-viewer');" +
                        "  var pageImgs = document.querySelectorAll('.read-viewer .page img').length;" +
                        "  var pageBoxes = document.querySelectorAll('.read-viewer .page').length;" +
                        "  var expected = 0;" +
                        "  var progress = document.querySelector('.progress-line');" +
                        "  if(progress){" +
                        "    var divs = progress.querySelectorAll('div');" +
                        "    if(divs.length >= 2){" +
                        "      var n = parseInt((divs[1].textContent || '').trim(), 10);" +
                        "      if(!isNaN(n)) expected = n;" +
                        "    }" +
                        "  }" +
                        "  if(!expected && pageBoxes > 0) expected = pageBoxes;" +
                        "  return JSON.stringify({" +
                        "    isBlocked:isBlocked," +
                        "    hasViewer:!!viewer," +
                        "    pageImgs:pageImgs," +
                        "    pageBoxes:pageBoxes," +
                        "    expectedPages:expected" +
                        "  });" +
                        "}catch(e){" +
                        "  return JSON.stringify({isBlocked:false,hasViewer:false,pageImgs:0,pageBoxes:0,expectedPages:0,error:String(e)});" +
                        "}" +
                        "})();";

        view.evaluateJavascript(js, result -> {
            if (finished) return;

            try {
                String cleaned = unescapeJsResult(result);
                JSONObject obj = new JSONObject(cleaned);

                Log.d(TAG, "Inspect result: " + cleaned);

                if (obj.optBoolean("isBlocked", false)) {
                    finished = true;
                    postError(callback, "Comix blocked the chapter page");
                    return;
                }

                expectedPages = obj.optInt("expectedPages", 0);
                Log.d(TAG, "Expected pages: " + expectedPages);

                collectAndAdvance(webViewRef(view), callback);

            } catch (Exception e) {
                finished = true;
                postError(callback, "Failed to inspect chapter page: " + e.getMessage());
            }
        });
    }

    private WebView webViewRef(WebView view) {
        return view;
    }

    private void collectAndAdvance(WebView view, PagesCallback callback) {
        if (finished) return;
        injectCollectorScript(view);
        handler.postDelayed(() -> advanceToNextPageBox(view), STEP_DELAY_MS);
    }

    private void advanceToNextPageBox(WebView view) {
        if (finished) return;

        String js =
                "(function(){" +
                        "try{" +
                        "  var pages = document.querySelectorAll('.read-viewer .page');" +
                        "  var total = pages.length;" +
                        "  var idx = " + currentPageIndex + ";" +
                        "  if(idx < pages.length){" +
                        "    pages[idx].scrollIntoView({behavior:'instant', block:'center'});" +
                        "  }" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".afterAdvance){" +
                        "    window." + JS_BRIDGE_NAME + ".afterAdvance(JSON.stringify({index:idx,total:total}));" +
                        "  }" +
                        "}catch(e){" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError){" +
                        "    window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "  }" +
                        "}" +
                        "})();";

        view.evaluateJavascript(js, null);
    }

    private void injectCollectorScript(WebView view) {
        String js =
                "(function(){" +
                        "try{" +
                        "  function normalize(url){" +
                        "    if(!url) return '';" +
                        "    url = String(url).trim();" +
                        "    if(!url) return '';" +
                        "    if(url.indexOf('//') === 0) return location.protocol + url;" +
                        "    if(url.indexOf('/') === 0) return location.origin + url;" +
                        "    return url;" +
                        "  }" +
                        "  function pickFromSrcset(srcset){" +
                        "    if(!srcset) return '';" +
                        "    var parts = srcset.split(',');" +
                        "    var last = parts[parts.length - 1].trim();" +
                        "    return last.split(' ')[0].trim();" +
                        "  }" +
                        "  var out = [];" +
                        "  var imgs = document.querySelectorAll('.read-viewer .page img, .viewer-wrapper .page img, .read-viewer img.fit-w');" +
                        "  imgs.forEach(function(img){" +
                        "    var src =" +
                        "      img.getAttribute('src') || " +
                        "      img.getAttribute('data-src') || " +
                        "      img.getAttribute('data-lazy-src') || " +
                        "      img.getAttribute('data-image') || " +
                        "      pickFromSrcset(img.getAttribute('srcset')) || " +
                        "      pickFromSrcset(img.getAttribute('data-srcset')) || " +
                        "      img.src || '';" +
                        "    src = normalize(src);" +
                        "    var cls = (img.getAttribute('class') || '').trim();" +
                        "    var parentCls = img.parentElement ? (img.parentElement.getAttribute('class') || '') : '';" +
                        "    var looksLikeReaderImage =" +
                        "      src.indexOf('wowpic') !== -1 || " +
                        "      src.indexOf('.webp') !== -1 || " +
                        "      src.indexOf('.jpg') !== -1 || " +
                        "      src.indexOf('.jpeg') !== -1 || " +
                        "      src.indexOf('.png') !== -1;" +
                        "    var looksLikeReaderDom =" +
                        "      /fit-w/i.test(cls) || /page/i.test(parentCls);" +
                        "    if(src && looksLikeReaderImage && looksLikeReaderDom){" +
                        "      out.push(src);" +
                        "    }" +
                        "  });" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".process){" +
                        "    window." + JS_BRIDGE_NAME + ".process(JSON.stringify(out));" +
                        "  }" +
                        "}catch(e){" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError){" +
                        "    window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "  }" +
                        "}" +
                        "})();";

        view.evaluateJavascript(js, null);
    }

    private void postError(PagesCallback callback, String message) {
        handler.post(() -> callback.onError(message));
    }

    private void postSuccess(PagesCallback callback, List<String> pages) {
        handler.post(() -> callback.onSuccess(pages));
    }

    private String unescapeJsResult(String result) {
        if (result == null) return "{}";
        String cleaned = result;
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
            cleaned = cleaned.replace("\\\\", "\\").replace("\\\"", "\"");
        }
        return cleaned;
    }

    private class JsBridge {
        private final WebView webView;
        private final PagesCallback callback;

        JsBridge(WebView webView, PagesCallback callback) {
            this.webView = webView;
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
                            pageSet.add(src);
                        }
                    }

                    Log.d(TAG, "Pages found so far: " + pageSet.size());
                    for (String page : pageSet) {
                        Log.d(TAG, "Page: " + page);
                    }

                    if (expectedPages > 0 && pageSet.size() >= expectedPages) {
                        finished = true;
                        postSuccess(callback, new ArrayList<>(pageSet));
                    }

                } catch (Exception e) {
                    finished = true;
                    Log.e(TAG, "Failed to parse JS JSON", e);
                    postError(callback, "Failed to parse JS JSON: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void afterAdvance(String json) {
            handler.postDelayed(() -> {
                if (finished) return;

                currentPageIndex++;

                if (pageSet.size() == lastPageCount) {
                    stableHits++;
                } else {
                    stableHits = 0;
                }

                lastPageCount = pageSet.size();

                Log.d(TAG, "Advance index: " + currentPageIndex +
                        " | pages=" + pageSet.size() +
                        " | stableHits=" + stableHits +
                        " | expected=" + expectedPages);

                if (expectedPages > 0 && pageSet.size() >= expectedPages) {
                    finished = true;
                    postSuccess(callback, new ArrayList<>(pageSet));
                    return;
                }

                if (stableHits >= 8) {
                    finished = true;
                    if (!pageSet.isEmpty()) {
                        postSuccess(callback, new ArrayList<>(pageSet));
                    } else {
                        postError(callback, "No chapter pages found");
                    }
                    return;
                }

                if (currentPageIndex >= MAX_PAGE_VISITS) {
                    finished = true;
                    if (!pageSet.isEmpty()) {
                        postSuccess(callback, new ArrayList<>(pageSet));
                    } else {
                        postError(callback, "No chapter pages found");
                    }
                    return;
                }

                collectAndAdvance(webView, callback);

            }, STEP_DELAY_MS);
        }

        @JavascriptInterface
        public void onJsError(String error) {
            Log.e(TAG, "JS Error: " + error);
        }
    }
}