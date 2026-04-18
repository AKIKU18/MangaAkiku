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

    private static final int INITIAL_DELAY_MS = 1800;
    private static final int PAGE_DELAY_MS = 1400;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // dedup dupa chapter number + normalized title
    private final Map<String, ChapterModel> chapterMapByKey = new LinkedHashMap<>();

    private boolean finished = false;
    private boolean started = false;

    private int totalPaginationPages = 1;
    private int targetPage = 1;

    private String lastPageSignature = "";

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public void getChapterList(Context context,
                               String mangaUrl,
                               ChapterListCallback callback) {

        if (mangaUrl == null || mangaUrl.trim().isEmpty()) {
            postError(callback, "Manga URL is empty");
            return;
        }

        String cleanUrl = mangaUrl.split("\\?")[0].trim();

        chapterMapByKey.clear();
        finished = false;
        started = false;
        totalPaginationPages = 1;
        targetPage = 1;
        lastPageSignature = "";

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

                    handler.postDelayed(() -> inspectPageCountFromSummary(view), INITIAL_DELAY_MS);
                }
            });

            webView.loadUrl(cleanUrl);
        });
    }

    private void inspectPageCountFromSummary(WebView view) {
        if (finished) return;

        String js =
                "(function(){" +
                        "try{" +
                        "  var info = document.querySelector('div.mt-3.text-center.text-body-secondary.small');" +
                        "  var rawText = info ? (info.innerText || info.textContent || '').trim() : '';" +
                        "  var bolds = info ? info.querySelectorAll('b') : [];" +
                        "  var start = 1;" +
                        "  var end = 20;" +
                        "  var totalItems = 0;" +

                        "  if (bolds.length >= 3) {" +
                        "    var s = parseInt((bolds[0].textContent || '').trim(), 10);" +
                        "    var e = parseInt((bolds[1].textContent || '').trim(), 10);" +
                        "    var t = parseInt((bolds[2].textContent || '').trim(), 10);" +
                        "    if (!isNaN(s)) start = s;" +
                        "    if (!isNaN(e)) end = e;" +
                        "    if (!isNaN(t)) totalItems = t;" +
                        "  }" +

                        "  var perPage = 20;" +
                        "  if (end >= start) {" +
                        "    perPage = (end - start) + 1;" +
                        "  }" +
                        "  if (perPage <= 0) perPage = 20;" +

                        "  var totalPages = 1;" +
                        "  if (totalItems > 0 && perPage > 0) {" +
                        "    totalPages = Math.ceil(totalItems / perPage);" +
                        "  }" +

                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onPageCountInfo){" +
                        "    window." + JS_BRIDGE_NAME + ".onPageCountInfo(JSON.stringify({" +
                        "      rawText: rawText," +
                        "      start: start," +
                        "      end: end," +
                        "      perPage: perPage," +
                        "      totalItems: totalItems," +
                        "      totalPages: totalPages" +
                        "    }));" +
                        "  }" +
                        "}catch(e){" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError){" +
                        "    window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "  }" +
                        "}" +
                        "})();";

        view.evaluateJavascript(js, null);
    }

    private void scrapeCurrentPage(WebView view) {
        if (finished) return;

        String js =
                "(function(){" +
                        "try{" +
                        "  var out = [];" +
                        "  var links = document.querySelectorAll('ul.chap-list li a.title');" +
                        "  links.forEach(function(a){" +
                        "    var href = a.getAttribute('href') || '';" +
                        "    var title = (a.innerText || a.textContent || '').trim();" +
                        "    var fullUrl = href;" +
                        "    if(href && href.indexOf('//') === 0){" +
                        "      fullUrl = location.protocol + href;" +
                        "    } else if(href && href.indexOf('/') === 0){" +
                        "      fullUrl = location.origin + href;" +
                        "    }" +
                        "    out.push({title:title,url:fullUrl});" +
                        "  });" +
                        "  var sig = out.map(function(x){ return x.url; }).join('|');" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".processPage){" +
                        "    window." + JS_BRIDGE_NAME + ".processPage(JSON.stringify({chapters:out,signature:sig}));" +
                        "  }" +
                        "}catch(e){" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError){" +
                        "    window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "  }" +
                        "}" +
                        "})();";

        view.evaluateJavascript(js, null);
    }

    private void goToPage(WebView view, int pageNumber) {
        if (finished) return;

        Log.d(TAG, "Going explicitly to page: " + pageNumber);

        String js =
                "(function(){" +
                        "try{" +
                        "  var clicked = false;" +
                        "  var candidates = document.querySelectorAll('ul.pagination a.page-link, nav.navigation a.page-link');" +
                        "  candidates.forEach(function(a){" +
                        "    if(clicked) return;" +
                        "    var txt = (a.textContent || '').trim();" +
                        "    var n = parseInt(txt, 10);" +
                        "    if(!isNaN(n) && n === " + pageNumber + "){" +
                        "      clicked = true;" +
                        "      a.click();" +
                        "    }" +
                        "  });" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".pageNavigationStarted){" +
                        "    window." + JS_BRIDGE_NAME + ".pageNavigationStarted(String(clicked));" +
                        "  }" +
                        "}catch(e){" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError){" +
                        "    window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "  }" +
                        "}" +
                        "})();";

        view.evaluateJavascript(js, null);
    }

    private void waitUntilPageChangedAndScrape(WebView view, int expectedPage, int retryCount) {
        if (finished) return;

        if (retryCount > 15) {
            Log.d(TAG, "Wait retries exceeded for page " + expectedPage + ", scraping anyway");
            scrapeCurrentPage(view);
            return;
        }

        String js =
                "(function(){" +
                        "try{" +
                        "  var links = document.querySelectorAll('ul.chap-list li a.title');" +
                        "  var sig = Array.from(links).map(function(a){" +
                        "    var href = a.getAttribute('href') || '';" +
                        "    if(href && href.indexOf('//') === 0) return location.protocol + href;" +
                        "    if(href && href.indexOf('/') === 0) return location.origin + href;" +
                        "    return href;" +
                        "  }).join('|');" +
                        "  var activePage = 1;" +
                        "  var active = document.querySelector(" +
                        "    'ul.pagination li.active a.page-link, ul.pagination li.active span.page-link, " +
                        "     nav.navigation li.active a.page-link, nav.navigation li.active span.page-link'" +
                        "  );" +
                        "  if(active){" +
                        "    var n = parseInt((active.textContent || '').trim(), 10);" +
                        "    if(!isNaN(n)) activePage = n;" +
                        "  }" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onPageStateCheck){" +
                        "    window." + JS_BRIDGE_NAME + ".onPageStateCheck(JSON.stringify({" +
                        "      signature:sig," +
                        "      activePage:activePage," +
                        "      retryCount:" + retryCount +
                        "    }));" +
                        "  }" +
                        "}catch(e){" +
                        "  if(window." + JS_BRIDGE_NAME + " && window." + JS_BRIDGE_NAME + ".onJsError){" +
                        "    window." + JS_BRIDGE_NAME + ".onJsError(String(e));" +
                        "  }" +
                        "}" +
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

    private String normalizeChapterTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9\\s.-]", "")
                .trim();
    }

    private String buildChapterKey(String title, String url) {
        String normalizedTitle = normalizeChapterTitle(title);
        String chapterNumber = extractChapterNumber(title, url);

        if (chapterNumber == null) chapterNumber = "";
        chapterNumber = chapterNumber.trim();

        if (!chapterNumber.isEmpty()) {
            return chapterNumber + "|" + normalizedTitle;
        }

        return normalizedTitle;
    }

    public static String generateChapterId(String url, String title) {
        String siteName = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
        String hash = String.valueOf((url + title).hashCode());
        return siteName + "-" + hash;
    }

    private void finishWithAll(ChapterListCallback callback) {
        finished = true;
        List<ChapterModel> all = new ArrayList<>(chapterMapByKey.values());
        Log.d(TAG, "Finish with full list. Total unique chapters: " + all.size());
        postSuccess(callback, all);
    }

    private void postError(ChapterListCallback callback, String message) {
        handler.post(() -> callback.onError(message));
    }

    private void postSuccess(ChapterListCallback callback, List<ChapterModel> chapters) {
        handler.post(() -> callback.onSuccess(chapters));
    }

    private class JsBridge {
        private final WebView webView;
        private final ChapterListCallback callback;

        JsBridge(WebView webView, ChapterListCallback callback) {
            this.webView = webView;
            this.callback = callback;
        }

        @JavascriptInterface
        public void onPageCountInfo(String json) {
            handler.post(() -> {
                if (finished) return;

                try {
                    JSONObject obj = new JSONObject(json);

                    String rawText = obj.optString("rawText", "");
                    int perPage = obj.optInt("perPage", 20);
                    int totalItems = obj.optInt("totalItems", 0);
                    totalPaginationPages = obj.optInt("totalPages", 1);

                    if (perPage <= 0) perPage = 20;
                    if (totalPaginationPages <= 0) totalPaginationPages = 1;

                    Log.d(TAG, "Summary text: " + rawText);
                    Log.d(TAG, "perPage=" + perPage + " totalItems=" + totalItems + " totalPages=" + totalPaginationPages);

                    targetPage = 1;
                    scrapeCurrentPage(webView);

                } catch (Exception e) {
                    finished = true;
                    Log.e(TAG, "Failed to parse page count info", e);
                    postError(callback, "Failed to parse page count info: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void processPage(String json) {
            handler.post(() -> {
                if (finished) return;
                if (json == null || json.trim().isEmpty()) return;

                try {
                    JSONObject root = new JSONObject(json);
                    JSONArray arr = root.optJSONArray("chapters");
                    String signature = root.optString("signature", "");

                    if (arr == null) {
                        finishWithAll(callback);
                        return;
                    }

                    int addedThisPage = 0;
                    int duplicatesThisPage = 0;

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        Log.d(TAG, "obj: " + obj.toString());

                        String title = obj.optString("title", "").trim();
                        String url = obj.optString("url", "").trim();

                        if (title.isEmpty() || url.isEmpty()) continue;

                        String key = buildChapterKey(title, url);
                        if (key.isEmpty()) continue;

                        if (chapterMapByKey.containsKey(key)) {
                            duplicatesThisPage++;
                            Log.d(TAG, "Duplicate skipped: " + key + " -> " + title);
                            continue;
                        }

                        String chapterNumber = extractChapterNumber(title, url);
                        String chapterId = generateChapterId(url, title);

                        ChapterModel chapter = new ChapterModel(
                                chapterId,
                                title,
                                chapterNumber,
                                url,
                                "Comix"
                        );
                        chapter.setChapterId(chapterId);
                        chapter.setTitle(title);
                        chapter.setNumber(chapterNumber);
                        chapter.setChapterUrl(url);
                        chapter.setSource("Comix");

                        chapterMapByKey.put(key, chapter);
                        addedThisPage++;
                    }

                    lastPageSignature = signature;

                    Log.d(TAG, "Scraped page " + targetPage + "/" + totalPaginationPages);
                    Log.d(TAG, "Added this page: " + addedThisPage);
                    Log.d(TAG, "Duplicates this page: " + duplicatesThisPage);
                    Log.d(TAG, "Unique chapters total: " + chapterMapByKey.size());

                    if (targetPage >= totalPaginationPages) {
                        Log.d(TAG, "All calculated pages visited, finishing");
                        finishWithAll(callback);
                        return;
                    }

                    targetPage++;
                    handler.postDelayed(() -> goToPage(webView, targetPage), PAGE_DELAY_MS);

                } catch (Exception e) {
                    finished = true;
                    Log.e(TAG, "Failed to parse JS JSON", e);
                    postError(callback, "Failed to parse JS JSON: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void pageNavigationStarted(String clicked) {
            handler.postDelayed(() -> {
                if (finished) return;

                boolean didClick = "true".equalsIgnoreCase(clicked);
                if (!didClick) {
                    Log.d(TAG, "Could not click target page " + targetPage + ", finishing");
                    finishWithAll(callback);
                    return;
                }

                waitUntilPageChangedAndScrape(webView, targetPage, 0);
            }, PAGE_DELAY_MS);
        }

        @JavascriptInterface
        public void onPageStateCheck(String json) {
            handler.post(() -> {
                if (finished) return;

                try {
                    JSONObject obj = new JSONObject(json);
                    String signature = obj.optString("signature", "");
                    int activePage = obj.optInt("activePage", 1);
                    int retryCount = obj.optInt("retryCount", 0);

                    Log.d(TAG, "Page state -> activePage=" + activePage + " targetPage=" + targetPage + " retry=" + retryCount);

                    if (activePage == targetPage && signature != null && !signature.isEmpty() && !signature.equals(lastPageSignature)) {
                        scrapeCurrentPage(webView);
                        return;
                    }

                    handler.postDelayed(() ->
                                    waitUntilPageChangedAndScrape(webView, targetPage, retryCount + 1),
                            PAGE_DELAY_MS);

                } catch (Exception e) {
                    finished = true;
                    Log.e(TAG, "Failed to parse page state", e);
                    postError(callback, "Failed to parse page state: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void onJsError(String error) {
            Log.e(TAG, "JS Error: " + error);
        }
    }
}