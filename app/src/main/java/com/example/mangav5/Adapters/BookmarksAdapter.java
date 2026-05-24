package com.example.mangav5.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.mangav5.Dao.MangaItemDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.MainActivitys.BookmarksPage;
import com.example.mangav5.MainActivitys.ChapterPage;
import com.example.mangav5.MainActivitys.MangaPage;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.Models.NotificationModel;
import com.example.mangav5.R;
import com.example.mangav5.ServiceMaster.ServiceController;
import com.example.mangav5.ServiceMaster.BookmarkService;

import java.util.List;
import java.util.concurrent.Executors;

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkMangaViewHolder> {

    private final List<BookmarkEntity> bookmarkList;
    private final Context context;
    private BookmarkDao bookmarkDao;
    public List<NotificationModel> notificationBookmarkList;
    private Boolean isListFullyLoaded;
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
        mangaItemModel.setLastChapter(manga.getLastChapter());

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
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontTransform()
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(holder.cover);
        } else {
            holder.cover.setImageResource(android.R.drawable.picture_frame);
        }

        // FIXED — stable binding using tags
        GetLastChapterTitle(mangaItemModel, holder.lastChapter, holder.viewedChapter, holder.showNotificationBookmark);


        // Bookmark toggle
        TooggleBookmark(holder, manga, bookmarkDao, position);
        StarToggle(holder.bookmarkStar);

        holder.itemView.setOnClickListener(v -> {
            GoToMangaItem(mangaItemModel);
        });

        holder.lastChapter.setOnClickListener(v -> {
            GoToLastChapter(mangaItemModel);
        });

        holder.viewedChapter.setOnClickListener(v -> {
            GetViewdChapter(mangaItemModel);
        });
    }

    private void GetViewdChapter(MangaItemModel mangaItem) {
        Intent intent = new Intent(context, ChapterPage.class);

        AppDatabase db = AppDatabase.getInstance(context);
        Executors.newSingleThreadExecutor().execute(() -> {

            // 1. First, try to get the history item with the stored Manga ID.
            HistoryEntity historyItem = db.historyDao().getHistoryItemInOrder(mangaItem.getMangaId());

            if (historyItem == null) {
                try {
                    historyItem = db.historyDao().getHistoryByTitle(mangaItem.getTitle());
                } catch (Exception e) {
                    Log.e("BookmarksAdapter", "Error in GetViewdChapter fallback: " + e.getMessage());
                }
            }


            // Now, 'historyItem' will be populated correctly if a match was found by either ID or title.
            if (historyItem != null) {
                String viewedChapterTitle = historyItem.chapterTitle;
                String viewedChapterId = historyItem.chapterId;
                String viewedChapterUrl = historyItem.chapterUrl; // Get the URL from history

                intent.putExtra("chapterId", viewedChapterId);
                intent.putExtra("chapterTitle", viewedChapterTitle);
                intent.putExtra("mangaId", mangaItem.getMangaId());
                intent.putExtra("mangaUrl", mangaItem.getMangaUrl());
                intent.putExtra("chapterUrl", viewedChapterUrl); // Pass the correct URL
                intent.putExtra("source", mangaItem.getSource());

                context.startActivity(intent);
            } else {
                // This will only be reached if NO history exists for this manga at all.
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "You haven’t read any chapters yet.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }


    private void GoToLastChapter(MangaItemModel mangaItem) {
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

            List<ChapterItemEntity> chapterItemEntity = db.chapterDao().getChaptersByMangaIdAsc(mangaIdOrUrlFinal);
            Intent intent = new Intent(context, ChapterPage.class);
            intent.putExtra("chapterId", chapterItemEntity.get(chapterItemEntity.size() - 1).getChapterId());
            intent.putExtra("chapterTitle", chapterItemEntity.get(chapterItemEntity.size() - 1).getTitle());
            intent.putExtra("mangaId", mangaItem.getMangaId());
            intent.putExtra("mangaUrl", mangaItem.getMangaUrl());
            intent.putExtra("chapterUrl", chapterItemEntity.get(chapterItemEntity.size() - 1).getChapterUrl());
            intent.putExtra("source", mangaItem.getSource());
            intent.putExtra("chapterNumber", chapterItemEntity.get(chapterItemEntity.size() - 1).chapterId);
            context.startActivity(intent);
        });


    }

    // 🔥 FIXED METHOD — uses tag-binding protection
    // 🔥 FIXED METHOD WITH TITLE FALLBACK
    private void GetLastChapterTitle(MangaItemModel mangaItem, TextView currentItemView, TextView viewedChapterView, TextView showNotificationBookmark) {
        String mangaId = mangaItem.getMangaId();

        // 1. Tag views immediately
        currentItemView.setTag(mangaId);
        viewedChapterView.setTag(mangaId);
        showNotificationBookmark.setTag(mangaId);

        // 2. Set placeholders
        String cachedLatest = (mangaItem.getLastChapter() != null) ? mangaItem.getLastChapter() : "-";
        currentItemView.setText("Current: " + cachedLatest);
        viewedChapterView.setText("Loading...");
        showNotificationBookmark.setVisibility(View.GONE);



        // 4. Start Background Work
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            // --- A. GET VIEWED HISTORY (Local DB) ---
            // 1. Try Strict ID Match
            HistoryEntity history = db.historyDao().getHistoryItemInOrder(mangaId);

            // 2. 🔥 FALLBACK: Title Match (Fix for Mgeko domain changes)
            if (history == null) {
                // If ID lookup failed (domain changed), try finding by Title
                // Note: Ensure you added getHistoryByTitle to your HistoryDao!
                try {
                    history = db.historyDao().getHistoryByTitle(mangaItem.getTitle());
                } catch (Exception e) {
                    Log.e("BookmarksAdapter", "getHistoryByTitle method missing in DAO");
                }
            }

            String viewedTitle = (history != null && history.chapterTitle != null) ? history.chapterTitle : "-";

            // Update "Viewed" Text
            updateViewedUI(viewedChapterView, mangaId, viewedTitle);

            GetLatestChapterAndUpdateUIDB(
                    mangaItem,
                    currentItemView,
                    viewedChapterView,
                    showNotificationBookmark
            );

            if (canSync(mangaItem.getMangaId())) {

                GetLatestChapterAndUpdateUINetwork(
                        mangaItem,
                        currentItemView,
                        viewedChapterView,
                        showNotificationBookmark
                );

                saveLastSync(mangaItem.getMangaId());
            }

        });
    }

    private void saveLastSync(String mangaId) {
        SharedPreferences prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE);
        prefs.edit()
                .putLong(mangaId, System.currentTimeMillis())
                .apply();
    }

    private boolean canSync(String mangaId) {
        SharedPreferences prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE);

        long last = prefs.getLong(mangaId, 0);
        long now = System.currentTimeMillis();

        long ONE_DAY = 24 * 60 * 60 * 1000L;

        return (now - last) > ONE_DAY;
    }

    public void updateList(List<BookmarkEntity> newList) {
        this.bookmarkList.clear();
        this.bookmarkList.addAll(newList);
        notifyDataSetChanged();
    }

    private void GetLatestChapterAndUpdateUIDB(MangaItemModel mangaItem,TextView currentItemView, TextView viewedChapterView,TextView showNotificationBookmark){
        Executors.newSingleThreadExecutor().execute(() -> {

            AppDatabase db = AppDatabase.getInstance(context);

            // 1. Get last read chapter from history (DB only)
            HistoryEntity history =
                    db.historyDao().getHistoryItemInOrder(mangaItem.getMangaId());

            String viewedTitle = (history != null && history.getChapterTitle() != null)
                    ? history.getChapterTitle()
                    : "-";

            // 2. Get latest chapter from chapter table (DB only)
            ChapterItemEntity latestChapter =
                    db.chapterDao().getLastChapter(mangaItem.getMangaId());

            String validLatestTitle = (latestChapter != null)
                    ? latestChapter.getTitle()
                    : mangaItem.getLastChapter();

            if (validLatestTitle == null || validLatestTitle.isEmpty()) {
                validLatestTitle = viewedTitle;
            }

            // 3. Update UI safely
            String mangaId = mangaItem.getMangaId();

            currentItemView.setTag(mangaId);
            viewedChapterView.setTag(mangaId);

            updateLatestUI(
                    mangaItem,
                    currentItemView,
                    showNotificationBookmark,
                    mangaId,
                    validLatestTitle,
                    viewedTitle
            );

            // 4. Update local model + DB (NO NETWORK)
            if (!validLatestTitle.equals(mangaItem.getLastChapter())) {

                mangaItem.setLastChapter(validLatestTitle);

                db.bookmarkDao().updateLastChapter(
                        mangaId,
                        validLatestTitle
                );
            }
        });
    }

    private void GetLatestChapterAndUpdateUINetwork(MangaItemModel mangaItem,TextView currentItemView, TextView viewedChapterView,TextView showNotificationBookmark){
        //TO DO: MAKE A NEW METHOD WHERE YOU CHECK FOR THE LAST CHAPTER  IN A WHILE AND NOT EVERYTIME IT ENTERS THE BOOKMARK PAGE
        // --- B. GET LATEST CHAPTER (Network) ---
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            HistoryEntity history = db.historyDao().getHistoryItemInOrder(mangaItem.getMangaId());

            String viewedTitle = (history != null && history.chapterTitle != null) ? history.chapterTitle : "-";

// 3. Prepare ID for network call
            final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(
                    mangaItem.getSource(), mangaItem.getMangaId(), mangaItem.getMangaUrl()
            );
            // 1. Tag views immediately
            currentItemView.setTag(mangaItem.getMangaId());
            viewedChapterView.setTag(mangaItem.getMangaId());
            ServiceController.fetchChapterListController(context,
                    mangaItem.getSource(),
                    mangaIdOrUrlFinal,
                    0,
                    1,
                    "desc",
                    new ServiceController.ChapterListCallback() {
                        @Override
                        public void onSuccess(List<ChapterModel> chapters) {
                            String validLatestTitle;

                            if (chapters != null && !chapters.isEmpty()) {
                                validLatestTitle = chapters.get(0).getTitle();
                                mangaItem.setLastChapter(validLatestTitle);
                                for (BookmarkEntity bookmark: bookmarkList)
                                {
                                    if(bookmark.getMangaId().equals(mangaItem.getMangaId())){
                                        Executors.newSingleThreadExecutor().execute(() -> {
                                            if(!db.bookmarkDao().getLastChapter(mangaItem.getMangaId()).equals(validLatestTitle)){
                                                db.bookmarkDao().updateLastChapter(mangaItem.getMangaId(), validLatestTitle);
                                            }else{
                                            }
                                        });
                                    }
                                }
                            } else {
                                validLatestTitle = (mangaItem.getLastChapter() != null && !mangaItem.getLastChapter().isEmpty())
                                        ? mangaItem.getLastChapter()
                                        : viewedTitle;
                            }



                            updateLatestUI(mangaItem,currentItemView, showNotificationBookmark, mangaItem.getMangaId(), validLatestTitle, viewedTitle);
                            ;                        }

                        @Override
                        public void onError(String message) {
                            String fallbackLatest = (mangaItem.getLastChapter() != null) ? mangaItem.getLastChapter() : "-";
                            updateLatestUI(mangaItem,currentItemView, showNotificationBookmark, mangaItem.getMangaId(), fallbackLatest, viewedTitle);
                        }
                    });
        });

    }
    // Helper to safely update the "Viewed" text on Main Thread
    private void updateViewedUI(TextView view, String tagId, String text) {
        view.post(() -> {
            if (tagId.equals(view.getTag())) {
                view.setText("Viewed: " + text);
            }
        });
    }

    // Helper to safely update "Current" text and Notification Dot
    private void updateLatestUI(
            MangaItemModel manga,
            TextView currentView,
            TextView dotView,
            String tagId,
            String latestId,
            String readId
    ) {
        currentView.post(() -> {

            if (!tagId.equals(currentView.getTag())) return;

            currentView.setText("Current: " + latestId);

            boolean hasNewChapters =
                    latestId != null &&
                            readId != null &&
                            !latestId.equals(readId);

            dotView.setVisibility(hasNewChapters ? View.VISIBLE : View.GONE);
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
        TextView title, lastChapter, viewedChapter, mangaSource, showNotificationBookmark;
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
            showNotificationBookmark = itemView.findViewById(R.id.showNotificationBookmark);
        }
    }
}
