package com.example.mangav5.ServicesMangaWebsites.ServiceComix;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to extract chapter pages from Comix.to using a Pattern-Generation approach.
 * It detects the total pages and the first image URL, then generates the sequence.
 */
public class ComixChapterPagesService {

    private static final String TAG = "ComixPagesService";
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface PagesCallback {
        void onSuccess(List<String> pages);
        void onError(String message);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void getChapterPages(Context context, String chapterUrl, PagesCallback callback) {
        Log.d(TAG, "Starting page extraction for: " + chapterUrl);

        handler.post(() -> {
            WebView webView = new WebView(context);
            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "WebView finished loading. Waiting for reader initialization...");

                    // Delaying for 2.5 seconds to ensure the 'read-viewer' and scripts are ready
                    handler.postDelayed(() -> performInspection(view, callback), 2500);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Log.e(TAG, "WebView Error: " + description);
                    callback.onError("WebView failed to load: " + description);
                }
            });

            webView.loadUrl(chapterUrl);
        });
    }

    private void performInspection(WebView view, PagesCallback callback) {
        Log.d(TAG, "Inspecting DOM for page count and image pattern...");

        // JavaScript to find the number of .page divs and the source of the first image
        String js = "(function(){" +
                "  try {" +
                "    var pageDivs = document.querySelectorAll('.read-viewer .page');" +
                "    var firstImg = document.querySelector('.read-viewer .page img');" +
                "    var src = '';" +
                "    if(firstImg) {" +
                "       src = firstImg.src || firstImg.getAttribute('data-src') || firstImg.getAttribute('data-lazy-src');" +
                "    }" +
                "    return JSON.stringify({" +
                "      total: pageDivs.length," +
                "      firstUrl: src" +
                "    });" +
                "  } catch(e) {" +
                "    return JSON.stringify({error: e.message});" +
                "  }" +
                "})();";

        view.evaluateJavascript(js, result -> {
            try {
                // Clean WebView JSON wrapper
                String jsonStr = result;
                if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                    jsonStr = jsonStr.substring(1, jsonStr.length() - 1)
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
                }

                JSONObject obj = new JSONObject(jsonStr);
                if (obj.has("error")) {
                    Log.e(TAG, "JS Error: " + obj.getString("error"));
                    callback.onError("JS Error: " + obj.getString("error"));
                    return;
                }

                int totalPages = obj.optInt("total", 0);
                String firstUrl = obj.optString("firstUrl", "");

                Log.d(TAG, "DOM Inspection -> Total Pages Found: " + totalPages);
                Log.d(TAG, "DOM Inspection -> Sample Image URL: " + firstUrl);

                if (totalPages > 0 && !firstUrl.isEmpty()) {
                    generatePageList(firstUrl, totalPages, callback);
                } else {
                    Log.e(TAG, "Failed to detect reader structure. Pages: " + totalPages + ", URL empty? " + firstUrl.isEmpty());
                    callback.onError("Could not find images in the reader.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Parsing error in performInspection: " + e.getMessage());
                callback.onError("Parsing failed: " + e.getMessage());
            }
        });
    }

    private void generatePageList(String firstUrl, int total, PagesCallback callback) {
        Log.d(TAG, "Attempting to generate sequence from pattern...");

        List<String> pages = new ArrayList<>();

        // Logic: Identify where the number is.
        // Example: https://static.comix.to/images/1.webp
        int lastSlash = firstUrl.lastIndexOf("/");
        int lastDot = firstUrl.lastIndexOf(".");

        if (lastSlash == -1 || lastDot == -1 || lastSlash > lastDot) {
            Log.w(TAG, "URL pattern not recognized. Falling back to single page.");
            pages.add(firstUrl);
            callback.onSuccess(pages);
            return;
        }

        String basePath = firstUrl.substring(0, lastSlash + 1); // "https://.../"
        String extension = firstUrl.substring(lastDot);        // ".webp"

        Log.d(TAG, "Base Path detected: " + basePath);
        Log.d(TAG, "Extension detected: " + extension);

        int digits = (total >= 100) ? 3 : 2;

        for (int i = 1; i <= total; i++) {
            String number = String.format("%0" + digits + "d", i);
            String generatedUrl = basePath + number + extension;
            pages.add(generatedUrl);
        }

        Log.d(TAG, "Successfully generated " + pages.size() + " page URLs.");

        // Log the first and last for verification
        if (!pages.isEmpty()) {
            Log.d(TAG, "First Page: " + pages.get(0));
            Log.d(TAG, "Last Page: " + pages.get(pages.size() - 1));
        }

        callback.onSuccess(pages);
    }
}