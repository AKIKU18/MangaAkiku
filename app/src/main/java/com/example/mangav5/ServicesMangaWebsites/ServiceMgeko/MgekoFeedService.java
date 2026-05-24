package com.example.mangav5.ServicesMangaWebsites.ServiceMgeko;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ScriptHelper.GenerateMangaIDHex;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusFeedService;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MgekoFeedService {
    private static final String TAG = "MgekoFeedService";
    private static final int TIMEOUT_MS = 60_000;

    /**
     * Fetch homepage feed from ManhuaPlus
     */
    public static void getMangaFeedMgeko(int page, MangaListCallback callback) {

        OkHttpClient client = new OkHttpClient();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        String url = "https://www.mgeko.cc/browse-comics/data/?page=" + page;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                List<MangaItemModel> mangaList = new ArrayList<>();

                try {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);

                    String html = obj.getString("results_html");
                    Document doc = Jsoup.parse(html);

                    // ✅ REAL selector used by mgeko cards
                    Elements items = doc.select("article.comic-card");
                    //Log.e("Items:", items.toString());
                    for (Element item : items) {

                        String titleEl = item.selectFirst(".comic-card__title").text();
                        String imageCover = item.selectFirst(".comic-card__cover img").attr("src");
                        String mangaUrl ="https://www.mgeko.cc" + item.selectFirst(".comic-card__title a").attr("href");
                        String description = item.selectFirst(".comic-card__description").text();
                        String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);

                        MangaItemModel m = new MangaItemModel();

                        m.setMangaId(mangaId);
                        m.setTitle(titleEl);
                        m.setCoverImageUrl(imageCover);
                        m.setMangaUrl(mangaUrl);
                        m.setDescription(description);
                        m.setLastChapter("");
                        m.setSource("Mgeko");

                        mangaList.add(m);
                    }

                    mainHandler.post(() -> callback.onSuccess(mangaList));

                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }


    /**
     * Fetch manga details page from Mgeko
     */
    public static void getMangaDetailsMgeko(String mangaUrl, MangaCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(mangaUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(TIMEOUT_MS)
                        .get();

                // ✅ MAIN CONTAINER
                Element container = doc.selectFirst("div.header-body");
                if (container == null)
                    throw new Exception("Header body not found");




                // ✅ TITLE
                String title = "";
                Element titleEl = container.selectFirst("h1.novel-title");
                if (titleEl != null) title = titleEl.text().trim();

                // ✅ ID
                String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);

                // ✅ Description
                Element descEl = doc.selectFirst("p.description");
                String description = descEl != null ? descEl.text().trim() : "";

                // ✅ COVER (data-src!)
                String cover = "";
                Element imgEl = container.selectFirst("figure.cover img");
                if (imgEl != null) {
                    cover = imgEl.hasAttr("data-src")
                            ? imgEl.attr("data-src")
                            : imgEl.attr("src");
                }

                // ✅ STATUS
                String status = "";
                Element statusEl = container.selectFirst("strong.ongoing");
                if (statusEl != null) status = statusEl.text();

                // ✅ CATEGORIES
                List<String> genres = new ArrayList<>();
                Elements genreEls = container.select(".categories a.property-item");
                for (Element g : genreEls) {
                    genres.add(g.text().trim());
                }

                MangaItemModel manga = new MangaItemModel(
                        mangaId,
                        title,
                        description,
                        cover,
                        false,
                        mangaUrl,
                        "",
                        "Mgeko"
                );

                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (Exception e) {
                mainHandler.post(() ->
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Error")
                );
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
