package com.example.mangav5.ServicesMangaWebsites.ServiceFlameComics;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.ScriptHelper.GenerateMangaIDHex;
import com.example.mangav5.ServicesMangaWebsites.ServiceManhuaPlus.ManhuaPlusFeedService;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlameComicsFeedService {
    private static final String TAG = "FlameComicsServiceFeed";
    private static final int TIMEOUT_MS = 60_000;

    /**
     * Fetch homepage feed from FlameComics
     */
    public static void getMangaFeedFlameComics(FlameComicsFeedService.MangaListCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        String websiteManga = "https://flamecomics.xyz/latest";
        executor.execute(() -> {
            List<MangaItemModel> mangaList = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(websiteManga)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(TIMEOUT_MS)
                        .get();

                // Each manga block in homepage
                Elements cards = doc.select("div.SeriesCard_chapterCardContainer__8ik-p");

                for (Element card : cards) {
                    try {
                        Element chapEl = card.selectFirst("div.SeriesCard_chapterPillWrapper__8qzPl.m_4081bf90.mantine-Group-root");
                        Element imgEl = card.selectFirst("div.SeriesCard_chapterImageContainer__b2jxP img");
                        if (imgEl == null) continue;

                        String mangaUrl = "https://flamecomics.xyz" + card.selectFirst("a").attr("href");
                        String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);
                        String title = imgEl.attr("alt");
                        String imgCover = extractRealImageUrl(imgEl.attr("src"));
                        String lastChapter = chapEl != null ? chapEl.text() : "N/A";
                        String chapterNumber = extractChapterNumber(lastChapter);

                        MangaItemModel m = new MangaItemModel();
                        m.setMangaId(mangaId);
                        m.setTitle(title);
                        m.setCoverImageUrl(imgCover);
                        m.setMangaUrl(mangaUrl);
                        m.setDescription("");
                        m.setLastChapter(lastChapter);
                        m.setSource("FlameComics");

                        mangaList.add(m);
                    } catch (Exception innerEx) {
                        Log.e("ERR", "Failed parsing: " + innerEx.getMessage());
                    }
                }


                mainHandler.post(() -> callback.onSuccess(mangaList));

            } catch (IOException e) {
                final String err = e.getMessage() != null ? e.getMessage() : "IO Error";
                Log.e(TAG, "Error fetching FlameComics homepage: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            } catch (Exception e) {
                final String err = e.getMessage() != null ? e.getMessage() : "Unknown Error";
                Log.e(TAG, "Unexpected error: " + err, e);
                mainHandler.post(() -> callback.onError(err));
            }
        });
    }


    /**
     * Fetch manga details page from FlameComics
     */
    public static void getMangaDetailsFlameComics(String mangaUrl, FlameComicsFeedService.MangaCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        String websiteManga = mangaUrl;
        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(websiteManga)
                        .userAgent("Mozilla/5.0 (Android App; +https://myapp.example)")
                        .timeout(TIMEOUT_MS)
                        .get();

                // Extract ID from URL
                String mangaId = GenerateMangaIDHex.generateUuidHex(mangaUrl);
                // Title
                String title = doc.selectFirst("div.SeriesPage_paper__MMnBx.m_1b7284a3.mantine-Paper-root > h1").text();
                String description = "";
                String imgCover ="https://flamecomics.xyz" + doc.selectFirst("img.SeriesPage_cover__cEjW-").attr("src");
                // Selectează toate capitolele
                Elements chapters = doc.select("a.ChapterCard_chapterWrapper__NIPp5");
                String chapterTitle ="";
                String chapterUrl = "";
                if (!chapters.isEmpty()) {
                    // Presupunem că primul element e ultimul capitol (cel mai recent)
                    Element lastChapter = chapters.first();

                    // Numele capitolului
                     chapterTitle = lastChapter.selectFirst("p.mantine-Text-root[data-size=md]").text().trim();

                    // URL complet (adaugă domeniul dacă href e relativ)
                     chapterUrl = "https://flamecomics.xyz" + lastChapter.attr("href");
                }



                // Extrage script-ul JSON-LD
                Element script = doc.selectFirst("script[type=application/ld+json]");
                if (script != null) {
                    String jsonText = script.html();
                    JSONObject json = new JSONObject(jsonText);
                    description = json.getString("description");
                }


                // Build Manga model
                MangaItemModel manga = new MangaItemModel(
                        mangaId,
                        title,
                        description,
                        imgCover,
                        false,
                        mangaUrl,
                        chapterTitle,
                        "FlameComics"
                );





                mainHandler.post(() -> callback.onSuccess(manga));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Unknown error"
                ));
            }
        });
    }


    private static String extractChapterNumber(String text) {
        if (text == null) return "0";

        // Găsește prima secvență de cifre (ex: 12, 45, 102)
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)")
                .matcher(text);

        if (m.find()) {
            return m.group(1); // returnează numărul găsit
        }

        return "0"; // fallback
    }

    private static String extractRealImageUrl(String nextImageUrl) {
        try {
            int start = nextImageUrl.indexOf("url=") + 4;
            int end = nextImageUrl.indexOf("&", start);

            String encoded = nextImageUrl.substring(start, end);
            return URLDecoder.decode(encoded, "UTF-8");
        } catch (Exception e) {
            return null;
        }
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
