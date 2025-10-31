package com.example.mangav5.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.MainActivitys.BookmarksPage;
import com.example.mangav5.MainActivitys.ChapterPage;
import com.example.mangav5.MainActivitys.MangaPage;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.ServiceMaster.ServiceController;
import com.example.mangav5.ServiceMaster.BookmarkService;

import java.util.List;
import java.util.concurrent.Executors;

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkMangaViewHolder> {

    private final List<BookmarkEntity> bookmarkList;
    private final Context context;
    private List<MangaItemModel> mangaList;
    private BookmarkDao bookmarkDao;
    private BookmarksAdapter bookmarkAdapter;

    public BookmarksAdapter(List<BookmarkEntity> bookmarkList, Context context) {
        this.bookmarkList = bookmarkList;
        this.context = context;
        AppDatabase db = AppDatabase.getInstance(context);
        this.bookmarkDao = db.bookmarkDao();
    }

    @NonNull
    @Override
    public BookmarkMangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_bookmark_item, parent, false);
        return new BookmarkMangaViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkMangaViewHolder holder, int position) {
        BookmarkEntity manga = bookmarkList.get(position);
        MangaItemModel mangaItemModel = new MangaItemModel();
        mangaItemModel.setMangaId(manga.getMangaId());
        mangaItemModel.setTitle(manga.getTitle());
        mangaItemModel.setCoverImageUrl(manga.getCoverUrl());
        mangaItemModel.setDescription(manga.getDescription());
        mangaItemModel.setMangaUrl(manga.getMangaUrl());
        mangaItemModel.setSource(manga.getSource());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            holder.mangaBlurCard.setRenderEffect(
                    RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
            );
        }


        holder.title.setText(manga.getTitle());
        // Load cover image using Picasso or Glide
        // Load manga cover image with Glide
        String coverUrl = manga.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(context)
                    .load(coverUrl)
                    .placeholder(android.R.drawable.ic_dialog_info)
                    .error(android.R.drawable.ic_dialog_alert)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontTransform()
                    .into(holder.cover);
        } else {
            holder.cover.setImageResource(android.R.drawable.picture_frame);
        }
        GetLastChapterTitle(mangaItemModel, holder.lastChapter, holder.viewedChapter);

        //Toggle bookmark star icon and delete or insert bookmark in database
        TooggleBookmark(holder, manga, bookmarkDao, position);
        StarToggle(holder.bookmarkStar);

        holder.itemView.setOnClickListener(v -> {
            GoToMangaItem(mangaItemModel);
        });

        holder.lastChapter.setOnClickListener(v -> {
            GetLastChapter(mangaItemModel);
        });

        holder.viewedChapter.setOnClickListener(v -> {
            GetViewdChapter(mangaItemModel);
        });
    }

    private static TextView getViewedChapter(@NonNull BookmarkMangaViewHolder holder) {
        return holder.viewedChapter;
    }

    private void GetViewdChapter(MangaItemModel mangaItem) {
        Intent intent = new Intent(context, ChapterPage.class);

        AppDatabase db = AppDatabase.getInstance(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            var historyItem = db.historyDao().getHistoryItem(mangaItem.getMangaId());

            if (historyItem != null) {
                String viewedChapterTitle = historyItem.chapterTitle;
                String viewedChapterId = historyItem.chapterId;

                intent.putExtra("chapterId", viewedChapterId);
                intent.putExtra("chapterTitle", viewedChapterTitle);
                intent.putExtra("mangaId", mangaItem.getMangaId());
                intent.putExtra("mangaUrl", mangaItem.getMangaUrl());
                intent.putExtra("chapterUrl", historyItem.chapterUrl);
                intent.putExtra("source", mangaItem.getSource());

                context.startActivity(intent);
            } else {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "You haven’t read any chapters yet.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }


    private void GetLastChapter(MangaItemModel mangaItem) {
        ServiceController.fetchChapterListController(mangaItem.getSource(), mangaItem.getMangaId(), mangaItem.getMangaUrl(), 0, 1, "desc", new ServiceController.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> fetchedChapters) {
                Intent intent = new Intent(context, ChapterPage.class);
                intent.putExtra("chapterId", fetchedChapters.get(0).getChapterId());
                intent.putExtra("chapterTitle", fetchedChapters.get(0).getTitle());
                intent.putExtra("mangaId", mangaItem.getMangaId());
                intent.putExtra("mangaUrl", mangaItem.getMangaUrl());
                intent.putExtra("chapterUrl", fetchedChapters.get(0).getChapterUrl());
                intent.putExtra("source", mangaItem.getSource());
                context.startActivity(intent);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void GetLastChapterTitle(MangaItemModel mangaItem, TextView currentItemView, TextView viewedChapterView) {

        ServiceController.fetchChapterListController(mangaItem.getSource(), mangaItem.getMangaId(), mangaItem.getMangaUrl(), 0, 1, "desc",
                new ServiceController.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {
                        AppDatabase db = AppDatabase.getInstance(context);

                        Executors.newSingleThreadExecutor().execute(() -> {
                            HistoryEntity history = db.historyDao().getHistoryItem(mangaItem.getMangaId());
                            String viewedChapterTitle = (history != null && history.chapterTitle != null)
                                    ? history.chapterTitle : "-";

                            // Post viewed chapter safely
                            viewedChapterView.post(() ->
                                    viewedChapterView.setText("Viewed: " + viewedChapterTitle)
                            );

                            // If chapter list is empty or null, use viewed chapter as "current"
                            if (chapters == null || chapters.isEmpty()) {
                                currentItemView.post(() ->
                                        currentItemView.setText("Current: " + viewedChapterTitle)
                                );
                            } else {
                                // Normal case — use latest fetched chapter title
                                ChapterModel latest = chapters.get(0);
                                String latestTitle = (latest.getTitle() != null) ? latest.getTitle() : "-";
                                currentItemView.post(() ->
                                        currentItemView.setText("Current: " + latestTitle)
                                );
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        // Handle network or parsing errors
                        AppDatabase db = AppDatabase.getInstance(context);
                        Executors.newSingleThreadExecutor().execute(() -> {
                            HistoryEntity history = db.historyDao().getHistoryItem(mangaItem.getMangaId());
                            String viewedChapterTitle = (history != null && history.chapterTitle != null)
                                    ? history.chapterTitle : "-";

                            // Use history as fallback if fetch failed
                            currentItemView.post(() ->
                                    currentItemView.setText("Current: " + viewedChapterTitle)
                            );
                            viewedChapterView.post(() ->
                                    viewedChapterView.setText("Viewed: " + viewedChapterTitle)
                            );
                        });
                    }
                });
    }


    public void GoToMangaItem(MangaItemModel manga) {
        Intent intent = new Intent(context, MangaPage.class);
        intent.putExtra("mangaId", manga.getMangaId());
        intent.putExtra("mangaUrl", manga.getMangaUrl());
        intent.putExtra("source", manga.getSource());
        BookmarksPage.mangaPageLauncher.launch(intent);
    }


    private void TooggleBookmark(BookmarkMangaViewHolder holder, BookmarkEntity manga, BookmarkDao bookmarkDao, int position) {
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
                    if (indexOfTheMangaToBeDeleted != -1) {
                        bookmarkList.remove(indexOfTheMangaToBeDeleted); // remove from list
                        notifyItemRemoved(indexOfTheMangaToBeDeleted);   // update UI
                    } else {
                        Log.w("BookmarksAdapter", "Item to remove was not found in the list (main kotlin.concurrent.thread). It might have been removed by another operation.");
                    }
                });
            }).start();
        });
    }

    private void StarToggle(ImageView holder) {
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
        TextView title, lastChapter, viewedChapter;
        CardView mangaBlurCard;


        public BookmarkMangaViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.mangaCoverImage);
            bookmarkStar = itemView.findViewById(R.id.bookmarkStar);
            title = itemView.findViewById(R.id.mangaTitle);
            lastChapter = itemView.findViewById(R.id.currentChapter);
            viewedChapter = itemView.findViewById(R.id.viewedChapter);
            mangaBlurCard = itemView.findViewById(R.id.mangaBlurCard);
        }
    }
}
