package com.example.mangav5.ServicesMangaWebsites.ServiceComix;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Network.NetworkHelper;
import com.example.mangav5.ScriptHelper.GenerateMangaIDHex;
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
    private static final OkHttpClient client = NetworkHelper.getOkHttpClient();

    public static void getMangaFeedComix(int page, MangaListCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Try API v1 first as it was previously working
        HttpUrl url = HttpUrl.parse("https://comix.to/api/v1/manga")
                .newBuilder()
                .addQueryParameter("order[chapter_updated_at]", "desc")
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("limit", "28")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", NetworkHelper.USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
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

                    JSONObject root = new JSONObject(body);
                    JSONArray itemsArray = null;

                    if (root.has("result")) {
                        JSONObject resultObj = root.optJSONObject("result");
                        if (resultObj != null) {
                            itemsArray = resultObj.optJSONArray("items");
                        }
                    }

                    if (itemsArray == null && root.has("data")) {
                        itemsArray = root.optJSONArray("data");
                    }

                    if (itemsArray == null) {
                        mainHandler.post(() -> callback.onError("No items found."));
                        return;
                    }

                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject item = itemsArray.getJSONObject(i);

                        String hashId = item.optString("hash_id", item.optString("id", ""));
                        String slug = item.optString("slug", "");
                        String title = item.optString("title", "");
                        String description = item.optString("synopsis", item.optString("description", ""));
                        String lastChapter = item.optString("latest_chapter", "");

                        String cover = "";
                        JSONObject poster = item.optJSONObject("poster");
                        if (poster != null) {
                            cover = poster.optString("large", poster.optString("medium", ""));
                        }

                        String urlManga = "";
                        if (!hashId.isEmpty() && !slug.isEmpty()) {
                            urlManga = "https://comix.to/title/" + hashId + "-" + slug;
                        } else if (!slug.isEmpty()) {
                            urlManga = "https://comix.to/title/" + slug;
                        }

                        if (!title.isEmpty() && !urlManga.isEmpty()) {
                            MangaItemModel m = new MangaItemModel();
                            m.setMangaId(GenerateMangaIDHex.generateUuidHex(urlManga));
                            m.setTitle(title);
                            m.setDescription(description);
                            m.setCoverImageUrl(cover);
                            m.setMangaUrl(urlManga);
                            m.setLastChapter(lastChapter);
                            m.setSource("Comix");
                            mangaList.add(m);
                        }
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
                // Comix detail pages are often JS-rendered. 
                // Let's try to extract the ID from the URL and use the API v2 if possible.
                // URL format: https://comix.to/title/hashId-slug
                
                String slugPart = mangaUrl.substring(mangaUrl.lastIndexOf("/") + 1);
                String hashId = slugPart.contains("-") ? slugPart.split("-")[0] : slugPart;

                String apiUrl = "https://comix.to/api/v2/manga/" + hashId;
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .header("User-Agent", NetworkHelper.USER_AGENT)
                        .header("Accept", "application/json")
                        .header("Referer", mangaUrl)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful() || response.code() == 404) {
                    // Try v1 as fallback
                    apiUrl = "https://comix.to/api/v1/manga/" + hashId;
                    request = new Request.Builder()
                            .url(apiUrl)
                            .header("User-Agent", NetworkHelper.USER_AGENT)
                            .header("Accept", "application/json")
                            .header("Referer", mangaUrl)
                            .build();
                    response = client.newCall(request).execute();
                }

                if (response.isSuccessful() && response.body() != null) {
                    JSONObject root = new JSONObject(response.body().string());
                    JSONObject item = root.optJSONObject("result");
                    if (item == null) item = root.optJSONObject("data");
                    
                    if (item != null) {
                        String title = item.optString("title", "");
                        String description = item.optString("synopsis", item.optString("description", ""));
                        String lastChapter = item.optString("latest_chapter", "");
                        
                        String cover = "";
                        JSONObject poster = item.optJSONObject("poster");
                        if (poster != null) {
                            cover = poster.optString("large", poster.optString("medium", ""));
                        }

                        MangaItemModel manga = new MangaItemModel();
                        manga.setMangaId(GenerateMangaIDHex.generateUuidHex(mangaUrl));
                        manga.setTitle(title);
                        manga.setDescription(description);
                        manga.setCoverImageUrl(cover);
                        manga.setMangaUrl(mangaUrl);
                        manga.setLastChapter(lastChapter);
                        manga.setSource("Comix");

                        mainHandler.post(() -> callback.onSuccess(manga));
                        return;
                    }
                }
                
                // Fallback to Jsoup if API fails
                Document doc = NetworkHelper.getJsoupConnection(mangaUrl).get();
                String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);

                Element titleElement = doc.selectFirst("h1.mpage__title, .mpage__title");
                String title = titleElement != null ? titleElement.text().trim() : "";

                Element descriptionElement = doc.selectFirst("p.mpage__desc, .mpage__desc");
                String descText = descriptionElement != null ? descriptionElement.text().trim() : "";

                String cover = "";
                Element coverElement = doc.selectFirst(".mpage__poster img, img.mpage__poster");
                if (coverElement != null) {
                    cover = coverElement.absUrl("src");
                    if (cover.isEmpty()) cover = coverElement.attr("src").trim();
                }

                String lastChapter = "";
                Element chapterElement = doc.selectFirst(".chapter-list-item, .mchap-row__ch");
                if (chapterElement != null) {
                    lastChapter = chapterElement.text().trim();
                }

                MangaItemModel manga = new MangaItemModel(
                        mangaId, title, descText, cover, false, mangaUrl, lastChapter, "Comix"
                );

                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (Exception e) {
                Log.e(TAG, "getMangaDetailsComix error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown Error"));
            } finally {
                executor.shutdown();
            }
        });
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
