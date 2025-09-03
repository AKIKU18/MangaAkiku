package com.example.mangav5.MainActivitys;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mangav5.Adapters.BookmarksAdapter;
import com.example.mangav5.Adapters.MangaPageAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.Services.ChaptersService;
import com.example.mangav5.Services.FeedMangaService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MangaPage extends AppCompatActivity {
    RecyclerView chapterRecyclerView;
    MangaPageAdapter mangaPageAdapter;
    List<ChapterModel> chapterList;
    ImageView cover;
    ImageView bookmarkStar;
    TextView title, description;
    Button firstChapterButtonPage, lastChapterButtonPage;


    Boolean isLoading = false;
    private int offset = 0;
    private final int LIMIT = 100;
    private MangaItemModel mangaItem;
    private String getMangaId;
    private BookmarksAdapter bookmarkAdapter;
    private BookmarkDao bookmarkDao;
    private List<BookmarkEntity> bookmarkList = new ArrayList<>();
    private boolean bookmarkChanged = false;
    ImageButton scrollToBottomButton;
    Button button_homeMangaPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_manga_page);
        getMangaId = getIntent().getStringExtra("mangaId");

        AppDatabase db = AppDatabase.getInstance(this);
        this.bookmarkDao =db.bookmarkDao();
        chapterList = new ArrayList<>();
        chapterRecyclerView = findViewById(R.id.chaptersRecyclerViewPage);
        chapterRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mangaPageAdapter = new MangaPageAdapter(chapterList, this,getMangaId);
        chapterRecyclerView.setAdapter(mangaPageAdapter);

        cover = findViewById(R.id.mangaCoverImagePage);
        bookmarkStar = findViewById(R.id.bookmarkButtonPage);
        title = findViewById(R.id.mangaTitlePage);
        description = findViewById(R.id.mangaDescriptionPage);
        firstChapterButtonPage = findViewById(R.id.firstChapterButtonPage);
        lastChapterButtonPage = findViewById(R.id.lastChapterButtonPage);
        scrollToBottomButton = findViewById(R.id.scrollToBottomButtonPage);
        button_homeMangaPage = findViewById(R.id.button_homeMangaPage);

        loadMangaItem();
        //loadChapterList(0);
        PrevOrNextChapter();
        handleBackPress();
        OnClickToggleMangaPage(bookmarkStar,mangaItem,bookmarkDao);
        CheckIfStillBookmarked();
        ScrollButton();
        HomePageGoTo();
    }

    private void ScrollButton(){
        scrollToBottomButton.setOnClickListener(v -> {
            assert chapterRecyclerView.getAdapter() != null;
            int itemCount = chapterRecyclerView.getAdapter().getItemCount();
            if (itemCount > 0) {
                chapterRecyclerView.smoothScrollToPosition(itemCount - 1); // scroll to bottom
            }
        });
    }

    private void CheckIfStillBookmarked() {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean isBookmarked = bookmarkDao.isBookmarked(getMangaId); // DB query in background

            // Update UI on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isBookmarked) {
                    bookmarkStar.setImageResource(R.drawable.ic_star_filled);
                } else {
                    bookmarkStar.setImageResource(R.drawable.ic_star_border);
                }
            });
        });
    }

    private  void OnClickToggleMangaPage(ImageView holder, MangaItemModel manb, BookmarkDao bookmarkDao){
            FeedMangaService.fetchMangaById(getMangaId, new FeedMangaService.MangaCallback() {
                @Override
                public void onSuccess(MangaItemModel manga) {
                        runOnUiThread(() -> {
                            holder.setOnClickListener(v -> {
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    ToggleBookmarkMangaPage(manga, bookmarkDao); // safely runs in background
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
                            mangaItem = manga;
                        });
                }
                @Override
                public void onError(String errorMessage) {

                };
            });
    }

    private  void ToggleBookmarkMangaPage(MangaItemModel manga, BookmarkDao bookmarkDao) {
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
        bookmarkChanged = true; // mark that something changed
    }
    private void ScrollLoadChapterList(){
        chapterRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastVisibleItemPosition() >= chapterList.size() - 5) {
                    //loadChapterList(100,true);
                }
            }
        });
    }

    private void loadMangaItem() {
        String mangaId = getIntent().getStringExtra("mangaId");

        FeedMangaService.fetchMangaById(mangaId, new FeedMangaService.MangaCallback() {

            @Override
            public void onSuccess(MangaItemModel manga) {

                if (manga == null) return;

                AppDatabase db = AppDatabase.getInstance(MangaPage.this);

                // Save manga to Room
                Executors.newSingleThreadExecutor().execute(() -> {
                    MangaItemEntity mangaEntity = new MangaItemEntity(
                            manga.getMangaId(),
                            manga.getTitle(),
                            manga.getCoverImageUrl(),
                            manga.getDescription()
                    );
                    db.mangaItemDao().insertManga(mangaEntity);
                    // Load chapters from Room first
                    List<ChapterItemEntity> savedChapters = db.chapterDao().getChaptersByMangaId(mangaId);
                    Log.e("MangaPage", "Loaded chapters: " + savedChapters.size());
                    runOnUiThread(() -> {
                        for (ChapterItemEntity c : savedChapters) {
                            if (chapterList.stream().noneMatch(ch -> ch.getChapterId().equals(c.getChapterId()))) {
                                ChapterModel ch = new ChapterModel(c.getChapterId(), c.getTitle(), c.getNumber());
                                chapterList.add(ch);
                                Log.e("MangaPage", "Loaded chapter FROM DB: " + c.getTitle());
                            }
                        }
                        mangaPageAdapter.notifyDataSetChanged();
                    });

                    // Then fetch remaining chapters from API
                    updateLoadChapterList(0); // start offset after existing chapters
                });

                runOnUiThread(() -> {
                    mangaItem = manga;
                    if (mangaItem != null && mangaItem.getCoverImageUrl() != null) {
                        title.setText(mangaItem.getTitle());
                        description.setText(mangaItem.getDescription());
                        Glide.with(MangaPage.this)
                                .load(mangaItem.getCoverImageUrl())
                                .placeholder(android.R.drawable.ic_dialog_info)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .error(android.R.drawable.ic_dialog_alert)
                                .into(cover);
                    } else {
                        cover.setImageResource(android.R.drawable.picture_frame);
                    }
                });;
            }

            @Override
            public void onError(String errorMessage) {

            };
        });
    }

    private void PrevOrNextChapter(){
        firstChapterButtonPage.setOnClickListener(v -> {
            GetFirstOrLastChapter("asc");
        });

        lastChapterButtonPage.setOnClickListener(v -> {
            GetFirstOrLastChapter("desc");
        });
    }

    private void GetFirstOrLastChapter(String descAsc){
        ChaptersService.fetchAllChapters(getMangaId, descAsc, 0, 1, new ChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> fetchedChapters) {
                Intent intent = new Intent(MangaPage.this, ChapterPage.class);
                intent.putExtra("chapterId", fetchedChapters.get(0).getChapterId());
                intent.putExtra("chapterTitle", fetchedChapters.get(0).getTitle());
                String id = getIntent().getStringExtra("mangaId");
                intent.putExtra("mangaId", id);
                MangaPage.this.startActivity(intent);
                Log.e("MangaPage", "Chapters fetched: " + fetchedChapters.size() + " total: " + chapterList.size());
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MangaPage.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private void updateLoadChapterList(int offset) {
        String mangaId = getIntent().getStringExtra("mangaId");
        int LIMIT = 100;
        AppDatabase db = AppDatabase.getInstance(this);

        ChaptersService.fetchAllChapters(mangaId, "desc", offset, LIMIT, new ChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> fetchedChapters) {
                if (fetchedChapters.isEmpty()) return; // stop recursion

                // Save/update in Room
                Executors.newSingleThreadExecutor().execute(() -> {
                    List<ChapterItemEntity> entities = new ArrayList<>();
                    for (ChapterModel c : fetchedChapters) {
                        entities.add(new ChapterItemEntity(
                                c.getChapterId(), // primary key
                                mangaId,
                                c.getTitle(),
                                c.getNumber()
                        ));
                    }
                    db.chapterDao().insertChapters(entities);
                });

                // Update UI
                runOnUiThread(() -> {
                    for (ChapterModel c : fetchedChapters) {
                        if (chapterList.stream().noneMatch(ch -> ch.getChapterId().equals(c.getChapterId()))) {
                            chapterList.add(c);

                        }
                    }
                    mangaPageAdapter.notifyDataSetChanged();
                });


                // Load next batch recursively
                // Fetch next batch only if we got full LIMIT
                if (fetchedChapters.size() == LIMIT) {
                    updateLoadChapterList(offset + LIMIT);
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(MangaPage.this, "Error loading chapters: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void handleBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("bookmarkChanged", bookmarkChanged);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void HomePageGoTo(){

        button_homeMangaPage.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("bookmarkChanged", bookmarkChanged);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
