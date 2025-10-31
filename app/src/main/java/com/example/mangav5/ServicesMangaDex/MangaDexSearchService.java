package com.example.mangav5.ServicesMangaDex;

import com.example.mangav5.Models.MangaItemModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MangaDexSearchService {
    private static final OkHttpClient client = new OkHttpClient();

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);
        void onError(String message);
    }

    public static void searchManga(String query, int offset, int limit, MangaListCallback callback) {
        String encodedQuery = query.replace(" ", "%20");

        String url = "https://api.mangadex.org/manga?title=" + encodedQuery +
                "&limit=" + limit +
                "&offset=" + offset +
                "&availableTranslatedLanguage[]=en" +
                "&hasAvailableChapters=true" +
                "&order[latestUploadedChapter]=desc" +
                "&includes[]=cover_art";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Server error: " + response.code());
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray data = json.getJSONArray("data");

                    List<MangaItemModel> mangaList = new ArrayList<>();

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        String id = obj.getString("id");
                        JSONObject attr = obj.getJSONObject("attributes");

                        String title = getMangaTitle(attr, "No Title");
                        String description = getLocalizedString(attr, "description", "No Description");
                        String coverUrl = null;

                        JSONArray rels = obj.optJSONArray("relationships");
                        if (rels != null) {
                            for (int j = 0; j < rels.length(); j++) {
                                JSONObject rel = rels.getJSONObject(j);
                                if ("cover_art".equals(rel.optString("type"))) {
                                    JSONObject relAttr = rel.optJSONObject("attributes");
                                    if (relAttr != null) {
                                        String fileName = relAttr.optString("fileName", null);
                                        if (fileName != null) {
                                            coverUrl = "https://uploads.mangadex.org/covers/" + id + "/" + fileName;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        mangaList.add(new MangaItemModel(id, title, description, coverUrl,false,url,"","MangaDex"));
                    }

                    callback.onSuccess(mangaList);
                } catch (JSONException e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    private static String getMangaTitle(JSONObject attr, String defaultValue) {
        try {
            // 1. Titlul principal în engleză
            JSONObject titleObj = attr.optJSONObject("title");
            if (titleObj != null) {
                if (titleObj.has("en")) {
                    return titleObj.optString("en", defaultValue);
                }else if (titleObj.has("ja")) {
                    return titleObj.optString("ja", defaultValue);
                } else if (titleObj.has("ja-ro")) {
                    return titleObj.optString("ja-ro", defaultValue);
                }
            }

            // 2. Alternative titles
            JSONArray altTitles = attr.optJSONArray("altTitles");
            if (altTitles != null) {
                for (int i = 0; i < altTitles.length(); i++) {
                    JSONObject alt = altTitles.optJSONObject(i);
                    if (alt != null) {
                        if (alt.has("en")) {
                            return alt.optString("en", defaultValue);
                        } else if (alt.has("ja-ro")) {
                            return alt.optString("ja-ro", defaultValue);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback
        return defaultValue;
    }

    private static String getLocalizedString(JSONObject jsonObject, String key, String defaultValue) {
        try {
            JSONObject obj = jsonObject.optJSONObject(key);
            if (obj != null) {
                return obj.optString("en", defaultValue);
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }
}
