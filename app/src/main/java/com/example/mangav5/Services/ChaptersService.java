package com.example.mangav5.Services;

import android.util.Log;

import com.example.mangav5.Models.ChapterListModel;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.google.gson.JsonObject;

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

public class ChaptersService {
    private static final OkHttpClient client = new OkHttpClient();

    public interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    public interface PagesCallback {
        void onSuccess(List<String> chapters);
        void onError(String message);
    }

    public static void fetchChapterList(String mangaId, ChapterListCallback callback) {
        int limit = 100;
        int offset = 0;
        String url = "https://api.mangadex.org/chapter?manga=" + mangaId +
                "&translatedLanguage[]=en&order[chapter]=asc&limit=" + limit + "&offset=" + offset;

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
                    Log.e("data", "data: " + data.toString());
                    final int totalChapters = data.length();
                    List<ChapterModel> chapterList = new ArrayList<>();

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        String id = obj.getString("id");
                        JSONObject attr = obj.getJSONObject("attributes");


                        String title = attr.isNull("title") ? "" : attr.optString("title", "").trim();
                        String chapterNumber = attr.isNull("chapter") ? "" : attr.optString("chapter", "").trim();

                        //Display Chapter Title and ChapterNumber
                        String displayTitle;
                        if (title.isEmpty()) {
                            displayTitle = "Chapter" + (chapterNumber.isEmpty() ? "" : ": " + chapterNumber);
                        } else {
                            displayTitle = title + (chapterNumber.isEmpty() ? "" : ": " + chapterNumber);
                        }

                        chapterList.add(new ChapterModel(id, displayTitle, chapterNumber));
                    }
                    callback.onSuccess(chapterList);

                } catch (JSONException e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });

    }


    public static void fetchChapterPages(String chapterId,PagesCallback callback){
        String url = "https://api.mangadex.org/at-home/server/" + chapterId;

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
                    String baseUrl = json.getString("baseUrl");
                    JSONObject chapterObj = json.getJSONObject("chapter");
                    String hash = chapterObj.getString("hash");
                    JSONArray data = chapterObj.getJSONArray("data");

                    List<String> pages = new ArrayList<>();

                    for (int i = 0; i < data.length(); i++) {
                        String filename = data.getString(i);
                        String pageUrl = baseUrl + "/data/" + hash + "/" + filename;
                        pages.add(pageUrl);
                    }
                    callback.onSuccess(pages);
                } catch (JSONException e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }
}
