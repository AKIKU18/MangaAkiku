package com.example.mangav5.Services;

import android.util.Log;

import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Dao.MangaItemDao;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Models.ChapterModel;
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

public class FeedMangaService {
    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);
        void onError(String message);
    }

    public interface MangaCallback {
        void onSuccess(MangaItemModel manga);
        void onError(String errorMessage);
    }

    private static final OkHttpClient client = new OkHttpClient();

    public static void fetchMangaById(String mangaId, MangaCallback callback) {
        String url = "https://api.mangadex.org/manga/" + mangaId + "?includes[]=cover_art";

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
                    JSONObject data = json.getJSONObject("data");
                    String id = data.getString("id");
                    JSONObject attr = data.getJSONObject("attributes");

                    String title = getLocalizedString(attr, "title", "No Title");
                    String description = getLocalizedString(attr, "description", "No Description");
                    String coverUrl = null;

                    JSONArray rels = data.optJSONArray("relationships");
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

                    MangaItemModel manga = new MangaItemModel(id, title, description, coverUrl, false);
                    callback.onSuccess(manga);

                } catch (JSONException e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }


    public static void fetchMangaList(int offset, int limit, MangaListCallback callback) {
        String url = "https://api.mangadex.org/manga?limit=" + limit
                + "&offset=" + offset
                + "&availableTranslatedLanguage[]=en"
                + "&hasAvailableChapters=true"
                + "&order[latestUploadedChapter]=desc"
                + "&includes[]=cover_art";

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

                        String title = getLocalizedString(attr, "title", "No Title");
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

                        mangaList.add(new MangaItemModel(id, title, description, coverUrl,false));
                    }

                    callback.onSuccess(mangaList);
                } catch (JSONException e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
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


    private static void SaveMangaItem(MangaItemModel manga, MangaItemDao mangaItemDao, List<ChapterModel> chapters){


    }
}
