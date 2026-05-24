package com.example.mangav5.ScriptHelper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServicesAsuraScans.AsuraScansFeedService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ScriptTester {

    private static final String TAG = "AsuraWebView";
    private static final long PAGE_WAIT_MS = 4000;

    public static void getMangaInfoAsuraScans(
            Context context,
            FrameLayout webViewContainer,
            String mangaUrl,
            AsuraScansFeedService.MangaCallback callback
    ) {
        String TAG = "AsuraWebView";
        Handler mainHandler = new Handler(Looper.getMainLooper());

        mainHandler.post(() -> {
            Log.e(TAG, "[1] Creating WebView");

            WebView webView = new WebView(context);
            webViewContainer.removeAllViews();
            webViewContainer.addView(webView);

            Log.e(TAG, "[2] WebView added to layout");

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadsImagesAutomatically(true);

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.e(TAG, "[JS] " + consoleMessage.message());
                    return true;
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.e(TAG, "[3] onPageFinished: " + url);

                    view.postDelayed(() -> {
                        Log.e(TAG, "[4] Getting HTML now");

                        view.evaluateJavascript(
                                "(function(){ return document.documentElement.outerHTML; })();",
                                value -> {
                                    Log.e(TAG, "[5] HTML callback triggered");

                                    if (value == null) {
                                        Log.e(TAG, "[ERROR] value is null");
                                        callback.onError("HTML is null");
                                        webViewContainer.removeAllViews();
                                        return;
                                    }

                                    Log.e(TAG, "[6] RAW value length: " + value.length());

                                    try {
                                        String html = value;

                                        if (html.startsWith("\"") && html.endsWith("\"")) {
                                            html = html.substring(1, html.length() - 1);
                                        }

                                        html = html
                                                .replace("\\u003C", "<")
                                                .replace("\\u003E", ">")
                                                .replace("\\u0026", "&")
                                                .replace("\\n", "\n")
                                                .replace("\\t", "\t")
                                                .replace("\\\"", "\"")
                                                .replace("\\/", "/")
                                                .replace("\\\\", "\\");

                                        Log.e(TAG, "[7] Decoded html length: " + html.length());
                                        Log.e(TAG, "[8] HTML preview: " + html.substring(0, Math.min(500, html.length())));

                                        Document doc = Jsoup.parse(html, mangaUrl);

                                        Element titleEl = doc.selectFirst("h1");
                                        Element descEl = doc.selectFirst("#description-text p, #description-text");
                                        Element imgEl = doc.selectFirst("article img[alt], img[alt]");
                                        Element chapterEl = doc.selectFirst("a[href*=/chapter/]");

                                        String title = titleEl != null ? titleEl.text().trim() : "";
                                        String description = descEl != null ? descEl.text().trim() : "";
                                        String coverUrl = imgEl != null ? imgEl.absUrl("src") : "";
                                        String lastChapter = chapterEl != null ? chapterEl.text().trim() : "";

                                        String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);

                                        MangaItemModel manga = new MangaItemModel(
                                                mangaId,
                                                title,
                                                description,
                                                coverUrl,
                                                false,
                                                mangaUrl,
                                                lastChapter,
                                                "AsuraScans"
                                        );

                                        callback.onSuccess(manga);
                                        webViewContainer.removeAllViews();

                                    } catch (Exception e) {
                                        Log.e(TAG, "[ERROR] parsing failed", e);
                                        callback.onError(e.getMessage());
                                        webViewContainer.removeAllViews();
                                    }
                                }
                        );
                    }, 4000);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    Log.e(TAG, "[ERROR] onReceivedError: " + error);
                    callback.onError(String.valueOf(error));
                    webViewContainer.removeAllViews();
                }
            });

            Log.e(TAG, "[0] loadUrl");
            webView.loadUrl(mangaUrl);
        });
    }
}