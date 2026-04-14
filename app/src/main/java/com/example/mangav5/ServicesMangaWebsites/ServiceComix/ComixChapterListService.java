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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComixChapterListService {

    private static final String TAG = "ComixChapterList";
    private static final String JS_BRIDGE_NAME = "AndroidBridge";

    private static final int INITIAL_DELAY_MS = 1500;
    private static final int PAGE_DELAY_MS = 1500;
    private static final int MAX_PAGE_TURNS = 50;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ChapterModel> chapterMap = new LinkedHashMap<>();

    private boolean finished = false;
    private boolean started = false;
    private int pageTurns = 0;
    private int stableCountHits = 0;
    private int lastChapterCount = 0;

    private int requestedOffset = 0;
    private int requestedLimit = 20;

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public void getChapterList(Context context,
                               String mangaUrl,
                               int offset,
                               int limit,
                               ChapterListCallback callback) {

        if (mangaUrl == null || mangaUrl.trim().isEmpty()) {
            postError(callback, "Manga URL is empty");
            return;
        }

        String cleanUrl = mangaUrl.split("\\?")[0].trim();

        chapterMap.clear();
        finished = false;
        started = false;
        pageTurns = 0;
        stableCountHits = 0;
        lastChapterCount = 0;

        requestedOffset = Math.max(0, offset);
        requestedLimit = Math.max(1, limit);

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

                    if (finished || started) return;
                    started = true;

                    handler.postDelayed(() -> scrapeCurrentPage(view), INITIAL_DELAY_MS);
                }
            });

            webView.loadUrl(cleanUrl);
        });
    }

    private void scrapeCurrentPage(WebView view) {
        if (finished) return;

        String js =
                "(function(){" +
                        "  try {" +
                        "    var out = [];" +
                        "    var links = document.querySelectorAll('ul.chap-list li a.title');" +

                        "    links.forEach(function(a) {" +
                        "      var href = a.getAttribute('href') || '';" +
                        "      var title = (a.innerText || a.textContent || '').trim();" +
                        "      var fullUrl = href;" +

                        "      if (href && href.indexOf('//') === 0) {" +
                        "        fullUrl = location.protocol + href;" +
                        "      } else if (href && href.indexOf('/') === 0) {" +
                        "        fullUrl = location.origin + href;" +
                        "      }" +

                        "      out.push({title:title,url:fullUrl});" +
                        "    });" +

                        "    var nextButton = null;" +
                        "    var nextCandidates = document.querySelectorAll('ul.pagination a.page-link, nav.navigation a.page-link');" +
                        "    nextCandidates.forEach(function(a) {" +
                        "      var txt = (a.textContent || '').trim();" +
                        "      var html = (a.innerHTML || '').toLowerCase();" +
                        "      if (!nextButton && (txt === 'Next' || html.indexOf('angle-right') !== -1 || html.indexOf('arrow-right') !== -1)) {" +
                        "        nextButton = a;" +
                        "      }" +
                        "    });" +

                        "    var hasNext = false;" +
                        "    if (nextButton) {" +
                        "      var li = nextButton.closest('li');" +
                        "      hasNext = !(li && li.classList.contains('disabled'));" +
                        "    }" +

                        "    if (window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".processPage) {" +
                        "      window." + JS_BRIDGE_NAME + ".processPage(JSON.stringify({chapters:out,hasNext:hasNext}));" +
                        "    }" +
                        "  } catch (e) {" +
                        "    if (window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError) {" +
                        "      window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "    }" +
                        "  }" +
                        "})();";

        view.evaluateJavascript(js, null);
    }

    private void goNextPage(WebView view) {
        if (finished) return;

        Log.d(TAG, "Going to next page");

        String js =
                "(function(){" +
                        "  try {" +
                        "    var nextButton = null;" +
                        "    var nextCandidates = document.querySelectorAll('ul.pagination a.page-link, nav.navigation a.page-link');" +
                        "    nextCandidates.forEach(function(a) {" +
                        "      var txt = (a.textContent || '').trim();" +
                        "      var html = (a.innerHTML || '').toLowerCase();" +
                        "      if (!nextButton && (txt === 'Next' || html.indexOf('angle-right') !== -1 || html.indexOf('arrow-right') !== -1)) {" +
                        "        nextButton = a;" +
                        "      }" +
                        "    });" +

                        "    if (nextButton) {" +
                        "      nextButton.click();" +
                        "      if (window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".pageChanged) {" +
                        "        window." + JS_BRIDGE_NAME + ".pageChanged('next');" +
                        "      }" +
                        "    } else {" +
                        "      if (window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".noNextPage) {" +
                        "        window." + JS_BRIDGE_NAME + ".noNextPage();" +
                        "      }" +
                        "    }" +
                        "  } catch (e) {" +
                        "    if (window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError) {" +
                        "      window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "    }" +
                        "  }" +
                        "})();";

        view.evaluateJavascript(js, null);
    }

    private String extractChapterNumber(String title, String url) {
        try {
            Matcher titleMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(title);
            if (titleMatcher.find()) {
                return titleMatcher.group(1).trim();
            }

            Matcher urlMatcher = Pattern.compile("chapter-(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE).matcher(url);
            if (urlMatcher.find()) {
                return urlMatcher.group(1).trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "extractChapterNumber error: " + e.getMessage(), e);
        }
        return "";
    }

    public static String generateChapterId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        return siteName + "-" + hash;
    }

    private void postError(ChapterListCallback callback, String message) {
        handler.post(() -> callback.onError(message));
    }

    private void postSuccess(ChapterListCallback callback, List<ChapterModel> chapters) {
        handler.post(() -> callback.onSuccess(chapters));
    }

    private void finishWithSlice(ChapterListCallback callback) {
        finished = true;

        List<ChapterModel> all = new ArrayList<>(chapterMap.values());

        if (requestedOffset >= all.size()) {
            postSuccess(callback, new ArrayList<>());
            return;
        }

        int end = Math.min(requestedOffset + requestedLimit, all.size());
        List<ChapterModel> sliced = new ArrayList<>(all.subList(requestedOffset, end));

        postSuccess(callback, sliced);
    }

    private class JsBridge {
        private final WebView webView;
        private final ChapterListCallback callback;

        JsBridge(WebView webView, ChapterListCallback callback) {
            this.webView = webView;
            this.callback = callback;
        }

        @JavascriptInterface
        public void processPage(String json) {
            handler.post(() -> {
                if (finished) return;
                if (json == null || json.trim().isEmpty()) return;

                try {
                    JSONObject root = new JSONObject(json);
                    JSONArray arr = root.optJSONArray("chapters");
                    boolean hasNext = root.optBoolean("hasNext", false);

                    if (arr == null) {
                        finishWithSlice(callback);
                        return;
                    }

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);

                        String title = obj.optString("title", "").trim();
                        String url = obj.optString("url", "").trim();

                        if (title.isEmpty() || url.isEmpty()) continue;

                        String chapterNumber = extractChapterNumber(title, url);
                        if (chapterNumber.isEmpty()) continue;

                        if (chapterMap.containsKey(chapterNumber)) continue;

                        String chapterId = generateChapterId(url, title);

                        ChapterModel chapter = new ChapterModel(chapterId, title, chapterNumber, url, "Comix");
                        chapter.setChapterId(chapterId);
                        chapter.setTitle(title);
                        chapter.setNumber(chapterNumber);
                        chapter.setChapterUrl(url);
                        chapter.setSource("Comix");

                        chapterMap.put(chapterNumber, chapter);
                    }

                    Log.d(TAG, "Unique chapters so far: " + chapterMap.size());

                    // Stop as soon as we have enough for this page request.
                    int neededCount = requestedOffset + requestedLimit;
                    if (chapterMap.size() >= neededCount) {
                        finishWithSlice(callback);
                        return;
                    }

                    if (!hasNext) {
                        finishWithSlice(callback);
                        return;
                    }

                    if (chapterMap.size() == lastChapterCount) {
                        stableCountHits++;
                    } else {
                        stableCountHits = 0;
                    }

                    lastChapterCount = chapterMap.size();

                    if (stableCountHits >= 2) {
                        finishWithSlice(callback);
                        return;
                    }

                    pageTurns++;
                    if (pageTurns >= MAX_PAGE_TURNS) {
                        finishWithSlice(callback);
                        return;
                    }

                    handler.postDelayed(() -> goNextPage(webView), PAGE_DELAY_MS);

                } catch (Exception e) {
                    finished = true;
                    Log.e(TAG, "Failed to parse JS JSON", e);
                    postError(callback, "Failed to parse JS JSON: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void pageChanged(String ignored) {
            handler.postDelayed(() -> {
                if (!finished) {
                    scrapeCurrentPage(webView);
                }
            }, PAGE_DELAY_MS);
        }

        @JavascriptInterface
        public void noNextPage() {
            handler.post(() -> {
                if (!finished) {
                    finishWithSlice(callback);
                }
            });
        }

        @JavascriptInterface
        public void onJsError(String error) {
            Log.e(TAG, "JS Error: " + error);
        }
    }
}