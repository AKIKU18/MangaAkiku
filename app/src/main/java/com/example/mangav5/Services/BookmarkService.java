package com.example.mangav5.Services;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.BookmarksAdapter;
import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;

import java.util.List;
import java.util.concurrent.Executors;

public class BookmarkService {

    public static void OnClickToggleBookmark(ImageView holder, MangaItemModel manga, BookmarkDao bookmarkDao){
        holder.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                toggleBookmark(manga, bookmarkDao); // safely runs in background
                boolean isBookmarked = bookmarkDao.isBookmarked(manga.getMangaId());

                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isBookmarked) {
                        holder.setImageResource(R.drawable.ic_star_filled);
                    } else {
                        holder.setImageResource(R.drawable.ic_star_border);
                    }
                });
            });
        });
    }

    private static void toggleBookmark(MangaItemModel manga, BookmarkDao bookmarkDao) {
        BookmarkEntity bookmark = new BookmarkEntity(manga.getMangaId(), manga.getTitle(), manga.getCoverImageUrl(), manga.getDescription());
        bookmark.setMangaId(manga.getMangaId());
        bookmark.setTitle(manga.getTitle());
        bookmark.setCoverUrl(manga.getCoverImageUrl());
        bookmark.setDescription(manga.getDescription());
        //If exist in database bookmark than delete
        if (bookmarkDao.isBookmarked(manga.getMangaId())) {
            bookmarkDao.delete(bookmark);
            manga.setIsBookmarked(false);
            Log.e("Bookmark deleted", String.valueOf(bookmarkDao.getAllBookmarks().size()));
        } else {
            //Else insert in database as a new bookmark
            bookmarkDao.insert(bookmark);
            manga.setIsBookmarked(true);
            Log.e("Bookmark Inserted", String.valueOf(bookmarkDao.getAllBookmarks().size()));

        }
    }


}
