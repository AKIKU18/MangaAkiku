package com.example.mangav5.ServicesMangaWebsites.ServiceComix;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ServicesMangaWebsites.ServiceDemonicScans.DemonicScansFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuas.ManhuausFeedService;
import com.example.mangav5.ServicesMangaWebsites.ServiceMgeko.MgekoFeedService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ComixFeedService {

    //Still Need somework here in the getMangaDetails
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    public static void getMangaFeedComix(int page, MangaListCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        HttpUrl url = HttpUrl.parse("https://comix.to/api/v1/manga")
                .newBuilder()
                .addQueryParameter("order[chapter_updated_at]", "desc")
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("limit", "28")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .header("Referer", "https://comix.to/")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    mainHandler.post(() ->
                            callback.onError("HTTP error: " + response.code()));
                    return;
                }

                try {
                    List<MangaItemModel> mangaList = new ArrayList<>();

                    JSONObject obj = new JSONObject(body);
                    JSONObject result = obj.getJSONObject("result");
                    JSONArray items = result.getJSONArray("items");

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);

                        String title = item.optString("title");
                        String synopsis = item.optString("synopsis");

                        JSONObject poster = item.optJSONObject("poster");
                        String image = poster != null ? poster.optString("large") : "";

                        String urlManga = item.optString("url"); // ✅ FIX IMPORTANT
                        String latestChapter = item.optString("latestChapter"); // ✅ FIX

                        String mangaId = generateUuidHex(urlManga);

                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(mangaId);
                        m.setTitle(title);
                        m.setDescription(synopsis);
                        m.setCoverImageUrl(image);
                        m.setMangaUrl(urlManga);
                        m.setLastChapter(String.valueOf(latestChapter));
                        m.setSource("Comix");

                        mangaList.add(m);
                    }

                    mainHandler.post(() -> callback.onSuccess(mangaList));

                } catch (Exception e) {
                    mainHandler.post(() ->
                            callback.onError("Parse error: " + e.getMessage()));
                }
            }
        });
    }

    public static void getMangaDetailsComix(Context context, String mangaUrl, MangaCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        .header("Accept-Language", "en-GB,en;q=0.9")
                        .timeout(60000)
                        .get();

                String mangaId = generateUuidHex(mangaUrl);

                Element titleElement = doc.selectFirst("h1.title");
                String title = titleElement != null ? titleElement.text().trim() : "";

                Element descriptionElement = doc.selectFirst("div.description div.content");
                String descText = descriptionElement != null ? descriptionElement.text().trim() : "";

                String cover = "";
                Element coverElement = doc.selectFirst("section.comic-info .poster img");
                if (coverElement != null) {
                    cover = coverElement.absUrl("src");
                    if (cover == null || cover.isEmpty()) {
                        cover = coverElement.attr("src").trim();
                    }
                }




                Element lastChapterElement = doc.selectFirst(".chap-list");
                String lastChapter = lastChapterElement != null ? lastChapterElement.text().trim() : "";

                MangaItemModel manga = new MangaItemModel(
                        mangaId,
                        title,
                        descText,
                        cover,
                        false,
                        mangaUrl,
                        lastChapter,
                        "Comix"
                );

                Log.e(TAG, "getMangaId: " + manga.getMangaId());
                Log.e(TAG, "getMangaTitle: " + manga.getTitle());
                Log.e(TAG, "getMangaDescription: " + manga.getDescription());
                Log.e(TAG, "getMangaCoverImageUrl: " + manga.getCoverImageUrl());
                Log.e(TAG, "getMangaUrl: " + manga.getMangaUrl());
                Log.e(TAG, "getLastChapter: " + manga.getLastChapter());




                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "IO Error"
                ));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Unknown Error"
                ));
            } finally {
                executor.shutdown();
            }
        });
    }
    public static String extractSlug(String url) {
        String[] parts = url.replaceAll("/+$", "").split("/");
        return parts[parts.length - 1];
    }

    public static String normalizeSlug(String slug) {
        return slug.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public static String generateUuidHex(String url) {
        String slug = normalizeSlug(extractSlug(url));
        UUID uuid = UUID.nameUUIDFromBytes(slug.getBytes(StandardCharsets.UTF_8));
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        return Long.toHexString(msb) + Long.toHexString(lsb);
    }


    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);
        void onError(String message);
    }

    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);
        void onError(String errorMessage);
    }
}
