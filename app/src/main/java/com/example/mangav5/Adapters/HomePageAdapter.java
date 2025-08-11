package com.example.mangav5.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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
import com.example.mangav5.MainActivitys.HomePage;
import com.example.mangav5.MainActivitys.MangaPage;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.Services.BookmarkService;

import java.util.List;
import java.util.concurrent.Executors;

public class HomePageAdapter extends RecyclerView.Adapter<HomePageAdapter.MangaViewHolder>{

    private final List<MangaItemModel> mangaList;
    private final Context context;
    private BookmarkDao bookmarkDao;



    public HomePageAdapter(List<MangaItemModel> mangaList, Context context) {
        this.mangaList = mangaList;
        this.context = context;
        AppDatabase db = AppDatabase.getInstance(context);
        this.bookmarkDao =db.bookmarkDao();
    }

    @NonNull
    @Override
    public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_manga_item, parent, false);
        return new MangaViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MangaViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MangaItemModel manga = mangaList.get(position);
        holder.title.setText(manga.getTitle());
        holder.description.setText(manga.getDescription());
        // Load cover image using Picasso or Glide
        // Load manga cover image with Glide
        String coverUrl = manga.getCoverImageUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(context)
                    .load(coverUrl)
                    .placeholder(android.R.drawable.ic_dialog_info)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(holder.cover);
        } else {
            holder.cover.setImageResource(android.R.drawable.picture_frame);
        }
        holder.bookmarkStar.setImageResource(manga.getIsBookmarked() ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
        //Toggle bookmark star icon and delete or insert bookmark in database
        BookmarkService.OnClickToggleBookmark(holder.bookmarkStar, manga, bookmarkDao);

        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               GoToMangaItem(position);
            }
        });

    }

    public void GoToMangaItem(int position){
        MangaItemModel manga = mangaList.get(position);
        Intent intent = new Intent(context, MangaPage.class);
        intent.putExtra("mangaId", manga.getMangaId());
        context.startActivity(intent);
    }



    public void refreshBookmarkStates() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            BookmarkDao dao = db.bookmarkDao();

            for (MangaItemModel manga : mangaList) {
                boolean isBookmarked = dao.isBookmarked(manga.getMangaId());
                manga.setIsBookmarked(isBookmarked);
            }

            new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
        });
    }



    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    public static class MangaViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        ImageView bookmarkStar;
        TextView title, description;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.mangaCoverImage);
            bookmarkStar = itemView.findViewById(R.id.bookmarkStar);
            title = itemView.findViewById(R.id.mangaTitle);
            description = itemView.findViewById(R.id.mangaDescription);
        }
    }
}
