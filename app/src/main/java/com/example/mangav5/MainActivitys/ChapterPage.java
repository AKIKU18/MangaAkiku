package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.ChapterPageAdapter;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.Services.ChaptersService;
import com.example.mangav5.Services.FeedMangaService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChapterPage extends AppCompatActivity {
    ChapterPageAdapter chapterPageAdapter;
    private RecyclerView recycleViewPage;
    private TextView tvChapterNumber;
    private Button btnPrevious;
    private Button btnHome;
    private Button btnNext;
    private ImageButton btnRefresh;
    private TextView tvMangaTitle;
    private List<String> chapters = new ArrayList<>();

    private String currentChapterId;
    private String currentChapterTitle;

    public String getCurrentChapterTitle() {
        return currentChapterTitle;
    }

    public void setCurrentChapterTitle(String chapterTitle) {
        this.currentChapterTitle = chapterTitle;
    }

    public String getCurrentChapterId() {
        return currentChapterId;
    }

    public void setCurrentChapterId(String chapterId) {
        this.currentChapterId = chapterId;
    }
    private ImageButton btnToggleUI;
    private ConstraintLayout upperPartLayout;
    private ConstraintLayout lowerPartLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        String chapterTitle = getIntent().getStringExtra("chapterTitle");
        String mangaId = getIntent().getStringExtra("mangaId");
        String chapterId = getIntent().getStringExtra("chapterId"); // initial chapter

        setCurrentChapterId(chapterId);
        setCurrentChapterTitle(chapterTitle); // <-- add this
        setContentView(R.layout.activity_chapter_page);

        recycleViewPage = findViewById(R.id.recyclerPages);
        recycleViewPage.setLayoutManager(new LinearLayoutManager(this));
        chapterPageAdapter = new ChapterPageAdapter(chapters, ChapterPage.this, chapterTitle);
        recycleViewPage.setAdapter(chapterPageAdapter);

        tvChapterNumber = findViewById(R.id.chapterNumber);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        tvMangaTitle = findViewById(R.id.mangaTitle);
        btnHome = findViewById(R.id.btnHome);
        btnRefresh = findViewById(R.id.btnRefresh);


        tvChapterNumber.setText(chapterTitle);
        GetChapterPages(chapterId);
        NextChapter();
        PrevChapter();
        InsertChapterIntoHistory();
        SetMangaTitle();
        GoToHomePage();
        ChapterRefresh();
        updateLoadChapterList(0);
        setupRecyclerScroll();
    }

    private void setupRecyclerScroll() {
        // --- Step 1: Initialize Views (in onCreate) ---
        btnToggleUI = findViewById(R.id.btnToggleUI);
        upperPartLayout = findViewById(R.id.upperPartLayout);
        lowerPartLayout = findViewById(R.id.lowerPartLayout);
        recycleViewPage = findViewById(R.id.recyclerPages); // Add your RecyclerView ID

        // --- Step 2: Set the initial state ---
        setUiVisibility(false); // Start hidden

        // --- Step 3: Set up listeners ---

        // Toggle with the button
        btnToggleUI.setOnClickListener(v -> toggleUiVisibility());

        // Toggle with a tap on the RecyclerView
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleUiVisibility();
                return true;
            }
        });

        recycleViewPage.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Allow scroll events to pass through
        });

        // Show UI when scrolled to the bottom
        recycleViewPage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Show the UI if the user scrolls down and has reached the end.
                // !canScrollVertically(1) is a simpler check for the bottom.
                if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                    setUiVisibility(true);
                }
            }
        });
    }

    /**
     * Toggles the visibility of the UI elements.
     */
    private void toggleUiVisibility() {
        boolean isCurrentlyVisible = (upperPartLayout.getVisibility() == View.VISIBLE);
        setUiVisibility(!isCurrentlyVisible);
    }

    /**
     * Central function to show or hide the UI elements and update the icon.
     * @param isVisible True to show the UI, false to hide it.
     */
    private void setUiVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        upperPartLayout.setVisibility(visibility);
        lowerPartLayout.setVisibility(visibility);

        int iconRes = isVisible ? R.drawable.ic_visibility_on : R.drawable.ic_visibility_off;
        btnToggleUI.setImageResource(iconRes);
    }


    private void ChapterRefresh() {
        btnRefresh.setOnClickListener(v -> {
            GetChapterPages(getCurrentChapterId());
            Toast.makeText(ChapterPage.this, "Refreshed", Toast.LENGTH_SHORT).show();
        });
    }


    private void SetMangaTitle() {
        String mangaId = getIntent().getStringExtra("mangaId");
        AppDatabase db = AppDatabase.getInstance(this);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            MangaItemEntity manga = db.mangaItemDao().getMangaById(mangaId);
            if (manga != null) {
                tvMangaTitle.setText(manga.getTitle());
            }
        });

        tvMangaTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoToMangaItem(mangaId);
            }
        });
    }

    public void GoToMangaItem(String mangaId) {
        Intent intent = new Intent(this, MangaPage.class);
        intent.putExtra("mangaId", mangaId);
        this.startActivity(intent);
    }

    private void GoToHomePage() {
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ChapterPage.this, HomePage.class);
            startActivity(intent);
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
                // Load next batch recursively
                // Fetch next batch only if we got full LIMIT
                if (fetchedChapters.size() == LIMIT) {
                    updateLoadChapterList(offset + LIMIT);
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(ChapterPage.this, "Error loading chapters: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void NextChapter() {
        btnNext.setOnClickListener(v -> {
            AppDatabase db = AppDatabase.getInstance(this);
            String mangaId = getIntent().getStringExtra("mangaId");
            String chapterId = getCurrentChapterId();


            Executors.newSingleThreadExecutor().execute(() -> {
                ChapterItemEntity nextChapter = db.chapterDao().getNextChapter(mangaId, chapterId);
                if (nextChapter != null) {
                    runOnUiThread(() -> {
                        setCurrentChapterId(nextChapter.getChapterId()); // update currentChapterId
                        setCurrentChapterTitle(nextChapter.getTitle());
                        runOnUiThread(() -> {
                            tvChapterNumber.setText(nextChapter.getTitle());
                            GetChapterPages(nextChapter.getChapterId());
                            recycleViewPage.scrollToPosition(0);

                            ImageButton btnToggleUI = findViewById(R.id.btnToggleUI);
                            ConstraintLayout upper = findViewById(R.id.upperPartLayout);
                            ConstraintLayout lower = findViewById(R.id.lowerPartLayout);

                            // Start hidden
                            upper.setVisibility(View.GONE);
                            lower.setVisibility(View.GONE);
                            btnToggleUI.setImageResource(R.drawable.ic_visibility_off); // 👁️ hidden
                        });
                    });
                    GetChapterPages(nextChapter.getChapterId());
                    Log.e("ChapterPage", "Next chapter fetched successfully: " + nextChapter.getTitle());
                } else {
                    Log.e("ChapterPage", "No next chapter found");
                    runOnUiThread(() -> {
                        Toast.makeText(ChapterPage.this, "No next chapter found", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    private void PrevChapter() {
        btnPrevious.setOnClickListener(v -> {
            AppDatabase db = AppDatabase.getInstance(this);
            String mangaId = getIntent().getStringExtra("mangaId");
            String chapterId = getCurrentChapterId();

            Executors.newSingleThreadExecutor().execute(() -> {
                Log.e("ChapterPage", "Current Manga ID: " + mangaId);
                ChapterItemEntity prevChapter = db.chapterDao().getPrevChapter(mangaId, chapterId);

                if (prevChapter != null) {
                    setCurrentChapterId(prevChapter.getChapterId()); // update currentChapterId
                    setCurrentChapterTitle(prevChapter.getTitle());
                    runOnUiThread(() -> {
                        tvChapterNumber.setText(prevChapter.getTitle());
                        GetChapterPages(prevChapter.getChapterId());
                        recycleViewPage.scrollToPosition(0);

                        // Update UI
                        ImageButton btnToggleUI = findViewById(R.id.btnToggleUI);
                        ConstraintLayout upper = findViewById(R.id.upperPartLayout);
                        ConstraintLayout lower = findViewById(R.id.lowerPartLayout);

                        // Start hidden
                        upper.setVisibility(View.GONE);
                        lower.setVisibility(View.GONE);
                        btnToggleUI.setImageResource(R.drawable.ic_visibility_off); // 👁️ hidden
                    });
                    Log.e("ChapterPage", "Prev chapter fetched successfully: " + prevChapter.getTitle());
                } else {
                    Log.e("ChapterPage", "No prev chapter found");
                    runOnUiThread(() -> {
                        Toast.makeText(ChapterPage.this, "No prev chapter found", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    private void GetChapterPages(String chapterId) {
        ChaptersService.fetchChapterPages(chapterId, new ChaptersService.PagesCallback() {
            @Override
            public void onSuccess(List<String> fetchPages) {
                runOnUiThread(() -> {
                    chapters.clear();
                    chapters.addAll(fetchPages);
                    InsertChapterIntoHistory();
                    chapterPageAdapter.notifyDataSetChanged();
                    Log.e("ChapterPage", "Pages fetched successfully:" + chapters.size());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(ChapterPage.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void InsertChapterIntoHistory() {
        String mangaId = getIntent().getStringExtra("mangaId");
        String chapterId = getIntent().getStringExtra("chapterId");
        String chapterTitle = getIntent().getStringExtra("chapterTitle");

        FeedMangaService.fetchMangaById(mangaId, new FeedMangaService.MangaCallback() {

            @Override
            public void onSuccess(MangaItemModel manga) {
                AppDatabase db = AppDatabase.getInstance(ChapterPage.this);

                Executors.newSingleThreadExecutor().execute(() -> {
                    HistoryEntity historyItem = new HistoryEntity(manga.getMangaId(), getCurrentChapterId(), getCurrentChapterTitle(), manga.getCoverImageUrl(), manga.getDescription(), System.currentTimeMillis(), manga.getTitle());
                    db.historyDao().insertHistoryItem(historyItem);
                    Log.e("ChapterPage", "Chapter inserted into history: " + getCurrentChapterTitle());
                });
            }

            @Override
            public void onError(String errorMessage) {

            }
        });


    }


}
