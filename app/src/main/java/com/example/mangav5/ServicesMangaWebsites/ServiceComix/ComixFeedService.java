package com.example.mangav5.ServicesMangaWebsites.ServiceComix;

import static android.content.ContentValues.TAG;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
import java.util.List;
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
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();
    public static void getMangaFeedComix(int page, MangaListCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("comix.to")
                .addPathSegment("api")
                .addPathSegment("v2")
                .addPathSegment("manga")
                .addQueryParameter("order[views_30d]", "desc")
                .addQueryParameter("genres_mode", "and")
                .addQueryParameter("limit", "28")
                .addQueryParameter("page", String.valueOf(page))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://comix.to/")
                .header("Origin", "https://comix.to")
                .header("Accept-Language", "en-GB,en;q=0.9")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                Log.e("COMIX_HTTP", "code=" + response.code());
                Log.e("COMIX_HTTP", body);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError(
                            "Comix blocked the request. HTTP " + response.code()
                    ));
                    return;
                }

                try {
                    List<MangaItemModel> mangaList = new ArrayList<>();

                    JSONObject obj = new JSONObject(body);
                    JSONObject result = obj.getJSONObject("result");
                    JSONArray itemsArray = result.getJSONArray("items");

                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject item = itemsArray.getJSONObject(i);

                        String titleEl = item.optString("title");
                        JSONObject poster = item.optJSONObject("poster");
                        String imageCover = poster != null ? poster.optString("large") : "";

                        String hash_id = item.optString("hash_id");
                        String slug = item.optString("slug");
                        String mangaUrl = "https://comix.to/title/" + hash_id + "-" + slug;
                        String description = item.optString("synopsis");
                        String mangaId = generateUuidHex(mangaUrl);
                        String lastChapter = item.optString("latest_chapter");

                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(mangaId);
                        m.setTitle(titleEl);
                        m.setCoverImageUrl(imageCover);
                        m.setMangaUrl(mangaUrl);
                        m.setDescription(description);
                        m.setLastChapter(lastChapter);
                        m.setSource("Comix");

                        mangaList.add(m);
                    }

                    mainHandler.post(() -> callback.onSuccess(mangaList));

                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Parse error: " + e.getMessage()));
                }
            }
        });
    }

    public static void getMangaDetailsComix(String mangaUrl, ComixFeedService.MangaCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Log.e("test","test");
        executor.execute(() -> {
            try {
                Log.e("test","test2");

                Document doc = Jsoup.connect("https://comix.to/title/5n07-olgami")
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(60_000)
                        .get();

                Log.e("test","test3");

                Element elements = doc.selectFirst("#wrapper > main > div");

                Log.e("test", elements.toString());
                // Manga ID from bookmark button
                Element bookmarkEl = doc.selectFirst(".add-bookmark .wp-manga-action-button");
                String mangaId = bookmarkEl != null ? bookmarkEl.attr("data-post").trim() : "";
                // Title
                Element titleElement = doc.selectFirst(".summary_image a img"); // the image alt can be used
                String title = titleElement != null ? titleElement.attr("alt").trim() : "No title";

                // Description (optional, use summary if exists)
                Element description = doc.selectFirst("div.summary__content.show-more"); // empty fallback
                String descText = description != null ? description.text().trim() : "";

                // Cover image
                Element coverElement = doc.selectFirst(".summary_image img");
                String cover = "";
                if (coverElement != null) {
                    if (coverElement.hasAttr("data-src") && !coverElement.attr("data-src").isEmpty())
                        cover = coverElement.attr("data-src").trim();
                    else if (coverElement.hasAttr("src")) cover = coverElement.attr("src").trim();
                }


                // Build MangaItemModel exactly like your constructor
                MangaItemModel manga = new MangaItemModel(
                        mangaId,
                        title,
                        descText,
                        cover,
                        false,
                        mangaUrl,
                        "",
                        "Manhuaus"
                );

                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "IO Error"));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown Error"));
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
