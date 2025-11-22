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
    private BookmarkDao bookmarkDao;

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

        holder.mangaSource.setText(manga.getSource());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            holder.mangaBlurCard.setRenderEffect(
                    RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
            );
        }

        holder.title.setText(manga.getTitle());

        if (manga.getCoverUrl() != null && !manga.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(manga.getCoverUrl())
                    .placeholder(android.R.drawable.ic_dialog_info)
                    .error(android.R.drawable.ic_dialog_alert)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontTransform()
                    .into(holder.cover);
        } else {
            holder.cover.setImageResource(android.R.drawable.picture_frame);
        }

        // FIXED — stable binding using tags
        GetLastChapterTitle(mangaItemModel, holder.lastChapter, holder.viewedChapter);

        // Bookmark toggle
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

    private void GetViewdChapter(MangaItemModel mangaItem) {
        Intent intent = new Intent(context, ChapterPage.class);

        AppDatabase db = AppDatabase.getInstance(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            var historyItem = db.historyDao().getHistoryItemInOrder(mangaItem.getMangaId());

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
        final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(
                mangaItem.getSource(), mangaItem.getMangaId(), mangaItem.getMangaUrl()
        );

        AppDatabase db = AppDatabase.getInstance(context);

        Executors.newSingleThreadExecutor().execute(() -> {
            // Save manga in Room
            db.mangaItemDao().insertManga(
                    new com.example.mangav5.Entity.MangaItemEntity(
                            mangaItem.getMangaId(),
                            mangaItem.getTitle(),
                            mangaItem.getCoverImageUrl(),
                            mangaItem.getDescription(),
                            mangaItem.getMangaUrl()
                            , mangaItem.getLastChapter(),
                            mangaItem.getSource()
                    )
            );
        });

        ServiceController.fetchChapterListController(context,
                mangaItem.getSource(),
                mangaIdOrUrlFinal,
                0,
                1,
                "desc",
                new ServiceController.ChapterListCallback() {
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

    // 🔥 FIXED METHOD — uses tag-binding protection
    private void GetLastChapterTitle(MangaItemModel mangaItem, TextView currentItemView, TextView viewedChapterView) {

        // Tag views with mangaId — ESSENTIAL FIX
        currentItemView.setTag(mangaItem.getMangaId());
        viewedChapterView.setTag(mangaItem.getMangaId());

        final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(
                mangaItem.getSource(), mangaItem.getMangaId(), mangaItem.getMangaUrl()
        );

        ServiceController.fetchChapterListController(context,
                mangaItem.getSource(),
                mangaIdOrUrlFinal,
                0,
                1,
                "desc",
                new ServiceController.ChapterListCallback() {

                    @Override
                    public void onSuccess(List<ChapterModel> chapters) {

                        AppDatabase db = AppDatabase.getInstance(context);

                        Executors.newSingleThreadExecutor().execute(() -> {

                            HistoryEntity history = db.historyDao().getHistoryItemInOrder(mangaItem.getMangaId());
                            String viewedChapterTitle = (history != null && history.chapterTitle != null)
                                    ? history.chapterTitle : "-";

                            // Update Viewed only if ViewHolder is still the same
                            viewedChapterView.post(() -> {
                                if (mangaItem.getMangaId().equals(viewedChapterView.getTag())) {
                                    viewedChapterView.setText("Viewed: " + viewedChapterTitle);
                                }
                            });

                            // No chapters returned → fallback to viewed
                            if (chapters == null || chapters.isEmpty()) {
                                currentItemView.post(() -> {
                                    if (mangaItem.getMangaId().equals(currentItemView.getTag())) {
                                        currentItemView.setText("Current: " + viewedChapterTitle);
                                    }
                                });
                            } else {
                                ChapterModel latest = chapters.get(0);
                                String latestTitle = latest.getTitle() != null ? latest.getTitle() : "-";

                                currentItemView.post(() -> {
                                    if (mangaItem.getMangaId().equals(currentItemView.getTag())) {
                                        currentItemView.setText("Current: " + latestTitle);
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {

                        AppDatabase db = AppDatabase.getInstance(context);

                        Executors.newSingleThreadExecutor().execute(() -> {
                            HistoryEntity history = db.historyDao().getHistoryItemInOrder(mangaItem.getMangaId());
                            String viewedChapterTitle = (history != null && history.chapterTitle != null)
                                    ? history.chapterTitle : "-";

                            currentItemView.post(() -> {
                                if (mangaItem.getMangaId().equals(currentItemView.getTag())) {
                                    currentItemView.setText("Current: " + viewedChapterTitle);
                                }
                            });

                            viewedChapterView.post(() -> {
                                if (mangaItem.getMangaId().equals(viewedChapterView.getTag())) {
                                    viewedChapterView.setText("Viewed: " + viewedChapterTitle);
                                }
                            });
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
                bookmarkDao.delete(manga);

                new Handler(Looper.getMainLooper()).post(() -> {
                    int index = bookmarkList.indexOf(manga);
                    if (index != -1) {
                        bookmarkList.remove(index);
                        notifyItemRemoved(index);
                    }
                });
            }).start();
        });
    }

    private void StarToggle(ImageView holder) {
        holder.setImageResource(R.drawable.ic_bookmark_filled);
    }

    @Override
    public int getItemCount() {
        return bookmarkList.size();
    }

    public static class BookmarkMangaViewHolder extends RecyclerView.ViewHolder {
        ImageView cover, bookmarkStar;
        TextView title, lastChapter, viewedChapter, mangaSource;
        CardView mangaBlurCard;

        public BookmarkMangaViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.mangaCoverImage);
            bookmarkStar = itemView.findViewById(R.id.bookmarkStar);
            title = itemView.findViewById(R.id.mangaTitle);
            lastChapter = itemView.findViewById(R.id.currentChapter);
            viewedChapter = itemView.findViewById(R.id.viewedChapter);
            mangaBlurCard = itemView.findViewById(R.id.mangaBlurCard);
            mangaSource = itemView.findViewById(R.id.mangaSource);
        }
    }
}
