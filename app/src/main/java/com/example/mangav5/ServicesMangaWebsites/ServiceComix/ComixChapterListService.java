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

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Map<String, ChapterModel> chapterMap = new LinkedHashMap<>();

    private boolean finished = false;

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public void getChapterList(Context context,
                               String mangaUrl,
                               ChapterListCallback callback) {

        if (mangaUrl == null || mangaUrl.trim().isEmpty()) {
            callback.onError("Manga URL is empty");
            return;
        }

        String cleanUrl = mangaUrl.split("\\?")[0].trim();

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

            webView.addJavascriptInterface(
                    new JsBridge(webView, callback),
                    JS_BRIDGE_NAME
            );

            webView.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    if (finished) return;

                    Log.d(TAG, "Page loaded: " + url);

                    handler.postDelayed(() -> scrapeAllChapters(view), 2500);
                }
            });

            webView.loadUrl(cleanUrl);
        });
    }

    private void scrapeAllChapters(WebView webView) {

        String js =
                "(async function() {" +

                        "let collected = [];" +
                        "let seen = new Set();" +

                        "function sleep(ms) {" +
                        "  return new Promise(r => setTimeout(r, ms));" +
                        "}" +

                        "function scrape() {" +

                        "document.querySelectorAll('li.mchap-item').forEach(row => {" +

                        "  let a = row.querySelector('a.mchap-row__primary');" +
                        "  if (!a) return;" +

                        "  let href = a.href || '';" +
                        "  if (!href) return;" +

                        "  if (href.startsWith('/')) href = location.origin + href;" +

                        "  let ch = row.querySelector('.mchap-row__ch');" +
                        "  let titleExtra = row.querySelector('.mchap-row__title');" +
                        "  let group = row.querySelector('.mchap-row__group span');" +
                        "  let time = row.querySelector('.mchap-row__time');" +

                        "  let title = (ch ? ch.innerText : '') + " +
                        "               (titleExtra ? ' ' + titleExtra.innerText : '');" +

                        "  if (!seen.has(href)) {" +
                        "    seen.add(href);" +
                        "    collected.push({" +
                        "      title: title.trim()," +
                        "      url: href," +
                        "      group: group ? group.innerText.trim() : ''," +
                        "      uploaded: time ? time.innerText.trim() : ''" +
                        "    });" +
                        "  }" +

                        "});" +
                        "}" +

                        "scrape();" +

                        "while (true) {" +

                        "let next = document.querySelector('a[rel=next]') || document.querySelector('a.next');" +

                        "if (!next) break;" +

                        "let old = location.href;" +
                        "next.click();" +

                        "await sleep(2500);" +

                        "let i = 0;" +
                        "while (location.href === old && i < 20) {" +
                        "  await sleep(500);" +
                        "  i++;" +
                        "}" +

                        "await sleep(1500);" +

                        "scrape();" +

                        "}" +

                        "window." + JS_BRIDGE_NAME + ".onChaptersLoaded(JSON.stringify(collected));" +

                        "})();";

        webView.evaluateJavascript(js, null);
    }

    private String extractChapterNumber(String title, String url) {

        try {

            Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(title);
            if (m.find()) return m.group(1);

            m = Pattern.compile("chapter[- ]?(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE)
                    .matcher(url);
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
                        String group = obj.optString("group", "").trim();
                        String uploaded = obj.optString("uploaded", "").trim();

                        if (title.isEmpty() || url.isEmpty()) continue;

                        if (chapterMap.containsKey(url)) continue;

                        String number = extractChapterNumber(title, url);
                        String chapterId = generateChapterId(url, title);

                        ChapterModel chapter = new ChapterModel(
                                chapterId,
                                title,
                                number,
                                url,
                                "Comix"
                        );

                        chapter.setChapterId(chapterId);
                        chapter.setTitle(title);
                        chapter.setNumber(number);
                        chapter.setChapterUrl(url);
                        chapter.setSource("Comix");

                        chapterMap.put(url, chapter);
                    }

                    chapters.addAll(chapterMap.values());

                    Log.d(TAG, "TOTAL CHAPTERS: " + chapters.size());

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

                Log.e(TAG, "JS ERROR: " + error);

                callback.onError(error);

                webView.destroy();
            });
        }
    }
}