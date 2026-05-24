package com.example.mangav5.Sources;

import android.content.Context;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import java.util.List;

public interface MangaSource {
    String getSourceName();

    // Core functionality
    void fetchFeed(int page, MangaListCallback callback);
    void fetchDetails(Context context, String mangaUrl, MangaCallback callback);
    void fetchChapters(Context context, String mangaUrl, ChapterListCallback callback);
    void fetchPages(Context context, String chapterUrl, PagesCallback callback);
    void search(String query, MangaListCallback callback);

    // Metadata
    default boolean useUrlAsId() { return true; }
    default boolean useUrlAsChapterId() { return true; }
    default boolean isPageBased() { return true; }
    default int getStartingPage() { return 1; }

    // Callbacks
    interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);
        void onError(String message);
    }

    interface MangaCallback {
        void onSuccess(MangaItemModel manga);
        void onError(String message);
    }

    interface ChapterListCallback {
        void onSuccess(List<ChapterModel> chapters);
        void onError(String message);
    }

    interface PagesCallback {
        void onSuccess(List<String> pages);
        void onError(String message);
    }
}
