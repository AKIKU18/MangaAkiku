package com.example.mangav5.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.Services.BookmarkService;
import com.example.mangav5.Services.ChaptersService;
import com.example.mangav5.MainActivitys.ChapterPage;
import com.example.mangav5.MainActivitys.MangaPage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class HomePageAdapter extends RecyclerView.Adapter<HomePageAdapter.MangaViewHolder> {

    private final List<MangaItemModel> mangaList;
    private final Context context;
    private final BookmarkDao bookmarkDao;
    private final ActivityResultLauncher<Intent> mangaPageLauncher;

    public HomePageAdapter(List<MangaItemModel> mangaList, Context context, ActivityResultLauncher<Intent> launcher) {
        this.mangaList = mangaList;
        this.context = context;
        this.mangaPageLauncher = launcher;
        AppDatabase db = AppDatabase.getInstance(context);
        this.bookmarkDao = db.bookmarkDao();
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

        // Load cover image
        String coverUrl = manga.getCoverImageUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(context)
                    .load(coverUrl)
                    .placeholder(R.drawable.error_placeholder)
                    .error(R.drawable.error_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .dontAnimate()
                    .format(DecodeFormat.PREFER_RGB_565) // or PREFER_ARGB_8888
                    .into(holder.cover);
        } else {
            holder.cover.setImageResource(android.R.drawable.picture_frame);
        }

        // Bookmark star
        holder.bookmarkStar.setImageResource(manga.getIsBookmarked() ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
        BookmarkService.OnClickToggleBookmark(holder.bookmarkStar, manga, bookmarkDao);

        // Load last chapter safely
        ChaptersService.fetchAllChapters(manga.getMangaId(), "desc", 0, 1, new ChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (chapters != null && !chapters.isEmpty()) {
                        holder.lastChapter.setText("Last Chapter: " + chapters.get(0).getTitle());
                    } else {
                        holder.lastChapter.setText("No chapters");
                    }
                });
            }

            @Override
            public void onError(String message) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    holder.lastChapter.setText("Error loading chapters");
                });
            }
        });

        holder.lastChapter.setOnClickListener(v -> goToChapterPage(manga));

        holder.title.setOnClickListener(v -> goToMangaPage(manga));
    }

    private void goToChapterPage(MangaItemModel manga) {
        AppDatabase db = AppDatabase.getInstance(context);

        Executors.newSingleThreadExecutor().execute(() -> {
            // Save manga in Room
            db.mangaItemDao().insertManga(
                    new com.example.mangav5.Entity.MangaItemEntity(
                            manga.getMangaId(),
                            manga.getTitle(),
                            manga.getCoverImageUrl(),
                            manga.getDescription()
                    )
            );
        });

        // Fetch latest chapter
        ChaptersService.fetchAllChapters(manga.getMangaId(), "desc", 0, 1, new ChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (chapters != null && !chapters.isEmpty()) {
                        ChapterModel lastChapter = chapters.get(0);
                        Intent intent = new Intent(context, ChapterPage.class);
                        intent.putExtra("chapterId", lastChapter.getChapterId());
                        intent.putExtra("chapterTitle", lastChapter.getTitle());
                        intent.putExtra("mangaId", manga.getMangaId());
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "No chapters found", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Error fetching chapters", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void goToMangaPage(MangaItemModel manga) {
        Intent intent = new Intent(context, MangaPage.class);
        intent.putExtra("mangaId", manga.getMangaId());
        mangaPageLauncher.launch(intent);
    }

    public void refreshBookmarkStates() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            BookmarkDao dao = db.bookmarkDao();

            // make a safe copy of the current list
            List<MangaItemModel> snapshot;
            synchronized (mangaList) {
                snapshot = new ArrayList<>(mangaList);
            }

            // update the copy
            for (MangaItemModel manga : snapshot) {
                boolean isBookmarked = dao.isBookmarked(manga.getMangaId());
                manga.setIsBookmarked(isBookmarked);
            }

            // push updates back to main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                synchronized (mangaList) {
                    for (int i = 0; i < mangaList.size(); i++) {
                        mangaList.get(i).setIsBookmarked(snapshot.get(i).getIsBookmarked());
                    }
                }
                notifyDataSetChanged();
            });
        });
    }


    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    public static class MangaViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        ImageView bookmarkStar;
        TextView title, description, lastChapter;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.mangaCoverImage);
            bookmarkStar = itemView.findViewById(R.id.bookmarkStar);
            title = itemView.findViewById(R.id.mangaTitle);
            description = itemView.findViewById(R.id.mangaDescription);
            lastChapter = itemView.findViewById(R.id.lastChapter);
        }
    }
}
