package com.example.mangav5.Adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.MainActivitys.BookmarksPage;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.Services.BookmarkService;

import java.util.List;

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkMangaViewHolder>{

    private final List<BookmarkEntity> bookmarkList;
    private final Context context;
    private BookmarkDao bookmarkDao;
    private BookmarksAdapter bookmarkAdapter;
    public BookmarksAdapter(List<BookmarkEntity> bookmarkList, Context context) {
        this.bookmarkList = bookmarkList;
        this.context = context;
        AppDatabase db = AppDatabase.getInstance(context);
        this.bookmarkDao =db.bookmarkDao();
    }

    @NonNull
    @Override
    public BookmarkMangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_bookmark_item, parent, false);
        return new BookmarksAdapter.BookmarkMangaViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkMangaViewHolder holder, int position) {
        BookmarkEntity manga = bookmarkList.get(position);
        holder.title.setText(manga.getTitle());
        holder.description.setText(manga.getDescription());
        // Load cover image using Picasso or Glide
        // Load manga cover image with Glide
        String coverUrl = manga.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(context)
                    .load(coverUrl)
                    .placeholder(android.R.drawable.ic_dialog_info)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(holder.cover);
        } else {
            holder.cover.setImageResource(android.R.drawable.picture_frame);
        }

        //Toggle bookmark star icon and delete or insert bookmark in database
        TooggleBookmark(holder, manga, bookmarkDao,position);
        StarToggle(holder.bookmarkStar);
    }



    private void TooggleBookmark(BookmarkMangaViewHolder holder, BookmarkEntity manga, BookmarkDao bookmarkDao, int position){
        MangaItemModel mangaItemModel = new MangaItemModel();
        mangaItemModel.setMangaId(manga.getMangaId());
        mangaItemModel.setTitle(manga.getTitle());
        mangaItemModel.setCoverImageUrl(manga.getCoverUrl());
        mangaItemModel.setDescription(manga.getDescription());
        BookmarkService.OnClickToggleBookmark(holder.bookmarkStar, mangaItemModel, bookmarkDao);
        holder.bookmarkStar.setOnClickListener(v -> {
            new Thread(() -> {
                bookmarkDao.delete(manga); // remove from DB

                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    int indexOfTheMangaToBeDeleted = bookmarkList.indexOf(manga);
                    if(indexOfTheMangaToBeDeleted != -1){
                        bookmarkList.remove(indexOfTheMangaToBeDeleted); // remove from list
                        notifyItemRemoved(indexOfTheMangaToBeDeleted);   // update UI
                    }else{
                        Log.w("BookmarksAdapter", "Item to remove was not found in the list (main kotlin.concurrent.thread). It might have been removed by another operation.");
                    }
                });
            }).start();
        });
    }

    private void StarToggle(ImageView holder){
        for (BookmarkEntity bookmark : bookmarkList) {
            holder.setImageResource(R.drawable.ic_star_filled);
        }
    }

    @Override
    public int getItemCount() {
        return bookmarkList.size();
    }

    public static class BookmarkMangaViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        ImageView bookmarkStar;
        TextView title, description;

        public BookmarkMangaViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.mangaCoverImage);
            bookmarkStar = itemView.findViewById(R.id.bookmarkStar);
            title = itemView.findViewById(R.id.mangaTitle);
            description = itemView.findViewById(R.id.mangaDescription);
        }
    }
}
