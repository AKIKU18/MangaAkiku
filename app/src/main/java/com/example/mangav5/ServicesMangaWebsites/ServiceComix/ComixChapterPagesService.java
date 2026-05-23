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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ComixChapterPagesService {

    private static final String TAG = "ComixPagesService";
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface PagesCallback {
        void onSuccess(List<String> pages);
        void onError(String message);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void getChapterPages(Context context, String chapterUrl, PagesCallback callback) {

        handler.post(() -> {

            WebView webView = new WebView(context);

            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setUserAgentString(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            );

            webView.addJavascriptInterface(new Object() {

                @JavascriptInterface
                public void onData(String json) {
                    handler.post(() -> {
                        try {
                            JSONObject obj = new JSONObject(json);

                            int total = obj.getInt("total");
                            String firstUrl = obj.getString("firstUrl");

                            List<String> pages = generatePages(firstUrl, total);

                            callback.onSuccess(pages);

                            webView.destroy();

                        } catch (Exception e) {
                            callback.onError(e.getMessage());
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

                                        "// get 'X / Y' from loader counters" +
                                        "document.querySelectorAll('.rpage-page__counter').forEach(e => {" +
                                        "  let txt = e.innerText;" +
                                        "  let m = txt.match(/\\/\\s*(\\d+)/);" +
                                        "  if (m) total = Math.max(total, parseInt(m[1]));" +
                                        "});" +

                                        "// fallback page div count" +
                                        "if (!total) total = document.querySelectorAll('.rpage-page').length;" +

                                        "// get first real image" +
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

    // ---------------------------
    // URL GENERATOR LOGIC
    // ---------------------------

    private List<String> generatePages(String firstUrl, int total) {

        List<String> pages = new ArrayList<>();

        try {
            int lastSlash = firstUrl.lastIndexOf("/");
            int lastDot = firstUrl.lastIndexOf(".");

            String basePath = firstUrl.substring(0, lastSlash + 1);
            String extension = firstUrl.substring(lastDot);

            for (int i = 1; i <= total; i++) {

                String num;

                if (i < 10) num = "0" + i;
                else num = String.valueOf(i);

                String url = basePath + num + extension;
                pages.add(url);
            }

        } catch (Exception e) {
            pages.add(firstUrl); // fallback
        }

        return pages;
    }
}