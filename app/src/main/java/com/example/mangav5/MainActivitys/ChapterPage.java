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
import com.example.mangav5.ServicesAsuraScans.AsuraScansChapterPages;
import com.example.mangav5.ServicesAsuraScans.AsuraScraperTask;
import com.example.mangav5.ServicesMangaDex.ChaptersService;
import com.example.mangav5.ServicesMangaDex.FeedMangaService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChapterPage extends AppCompatActivity {

    private RecyclerView recycleViewPage;
    private ChapterPageAdapter chapterPageAdapter;
    private List<String> chapters = new ArrayList<>();

    private TextView tvChapterNumber, tvMangaTitle;
    private Button btnPrevious, btnNext, btnHome;
    private ImageButton btnRefresh, btnToggleUI;
    private ConstraintLayout upperPartLayout, lowerPartLayout;

    private String currentChapterId;
    private String currentChapterTitle;

    public String getCurrentChapterTitle() { return currentChapterTitle; }
    public void setCurrentChapterTitle(String chapterTitle) { this.currentChapterTitle = chapterTitle; }
    public String getCurrentChapterId() { return currentChapterId; }
    public void setCurrentChapterId(String chapterId) { this.currentChapterId = chapterId; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_page);

        // Remove title bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // --- Initialize Views ---
        recycleViewPage = findViewById(R.id.recyclerPages);
        tvChapterNumber = findViewById(R.id.chapterNumber);
        tvMangaTitle = findViewById(R.id.mangaTitle);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnHome = findViewById(R.id.btnHome);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnToggleUI = findViewById(R.id.btnToggleUI);
        upperPartLayout = findViewById(R.id.upperPartLayout);
        lowerPartLayout = findViewById(R.id.lowerPartLayout);

        recycleViewPage.setLayoutManager(new LinearLayoutManager(this));
        chapterPageAdapter = new ChapterPageAdapter(chapters, this, "");
        recycleViewPage.setAdapter(chapterPageAdapter);

        // --- Get Intent Data ---
        String chapterId = getIntent().getStringExtra("chapterId");
        String chapterTitle = getIntent().getStringExtra("chapterTitle");
        String chapterUrl = getIntent().getStringExtra("chapterUrl");
        String mangaId = getIntent().getStringExtra("mangaId");

        setCurrentChapterId(chapterId);
        setCurrentChapterTitle(chapterTitle);
        tvChapterNumber.setText(chapterTitle);

        // --- Load chapter pages ---
        SwitchFeedPages(chapterId, chapterUrl);

        // --- Setup UI ---
        setupRecyclerScroll();
        ChapterRefresh();
        NextChapter();
        PrevChapter();
        SwitchInsertHistoryChapter();
        SetMangaTitle(mangaId);
        GoToHomePage();
        switch (HomePage.serviceFeed){
            case "MangaDex":
                updateLoadChapterListMangaDex(0);
                break;
            case "AsuraScans":
                updateLoadChapterListAsuraScans();
                break;
        }
    }

    private void SwitchInsertHistoryChapter(){
        switch (HomePage.serviceFeed){
            case "MangaDex":
                InsertChapterIntoHistoryMangaDex();
                break;
            case "AsuraScans":
                InsertChapterIntoHistoryAsuraScans();
                break;
        }
    }

    private void SwitchFeedPages(String chapterId, String chapterUrl) {
        switch (HomePage.serviceFeed) {
            case "MangaDex":
                GetChapterPagesMangaDex(chapterId);
                break;
            case "AsuraScans":
                GetChapterPagesAsuraScans(chapterUrl);
                break;
        }
    }

    private void GetChapterPagesMangaDex(String chapterId) {
        ChaptersService.fetchChapterPages(chapterId, new ChaptersService.PagesCallback() {
            @Override
            public void onSuccess(List<String> fetchPages) {
                runOnUiThread(() -> {
                    chapters.clear();
                    chapters.addAll(fetchPages);
                    SwitchInsertHistoryChapter();
                    chapterPageAdapter.notifyDataSetChanged();
                    Log.e("ChapterPage", "Pages fetched successfully: " + chapters.size());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(ChapterPage.this, "Error: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void GetChapterPagesAsuraScans(String chapterUrl) {
        AsuraScansChapterPages scraper = new AsuraScansChapterPages();
        scraper.GetChapterPages(this, chapterUrl, new AsuraScansChapterPages.PagesCallback() {
            @Override
            public void onSuccess(List<String> pages) {
                runOnUiThread(() -> {
                    chapters.clear();
                    chapters.addAll(pages);
                    SwitchInsertHistoryChapter();
                    chapterPageAdapter.notifyDataSetChanged();
                    Log.e("ChapterPage", "Pages fetched successfully: " + chapters.size());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(ChapterPage.this, "Error: " + message, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void InsertChapterIntoHistoryMangaDex() {
        String mangaId = getIntent().getStringExtra("mangaId");
        FeedMangaService.fetchMangaById(mangaId, new FeedMangaService.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) {
                AppDatabase db = AppDatabase.getInstance(ChapterPage.this);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    HistoryEntity historyItem = new HistoryEntity(
                            manga.getMangaId(),
                            getCurrentChapterId(),
                            getCurrentChapterTitle(),
                            manga.getCoverImageUrl(),
                            manga.getDescription(),
                            System.currentTimeMillis(),
                            manga.getTitle()
                    );
                    db.historyDao().insertHistoryItem(historyItem);
                    Log.e("ChapterPage", "Chapter inserted into history: " + getCurrentChapterTitle());
                });
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void InsertChapterIntoHistoryAsuraScans() {
        String mangaUrl = getIntent().getStringExtra("mangaUrl");
        AsuraScraperTask.getMangaInfoAsuraScans(mangaUrl, new AsuraScraperTask.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) {
                AppDatabase db = AppDatabase.getInstance(ChapterPage.this);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    HistoryEntity historyItem = new HistoryEntity(
                            manga.getMangaId(),
                            getCurrentChapterId(),
                            getCurrentChapterTitle(),
                            manga.getCoverImageUrl(),
                            manga.getDescription(),
                            System.currentTimeMillis(),
                            manga.getTitle()
                    );
                    db.historyDao().insertHistoryItem(historyItem);
                    Log.e("ChapterPage", "Chapter inserted into history: " + getCurrentChapterTitle());
                });
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void NextChapter() {
        btnNext.setOnClickListener(v -> {
            AppDatabase db = AppDatabase.getInstance(this);
            String mangaId = getIntent().getStringExtra("mangaId");
            Executors.newSingleThreadExecutor().execute(() -> {
                ChapterItemEntity nextChapter = db.chapterDao().getNextChapter(mangaId, getCurrentChapterId());
                if (nextChapter != null) {
                    runOnUiThread(() -> loadChapter(nextChapter.getChapterId(), nextChapter.getTitle(), nextChapter.getChapterUrl()));
                } else runOnUiThread(() -> Toast.makeText(this, "No next chapter found", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void PrevChapter() {
        btnPrevious.setOnClickListener(v -> {
            AppDatabase db = AppDatabase.getInstance(this);
            String mangaId = getIntent().getStringExtra("mangaId");
            Executors.newSingleThreadExecutor().execute(() -> {
                ChapterItemEntity prevChapter = db.chapterDao().getPrevChapter(mangaId, getCurrentChapterId());
                if (prevChapter != null) {
                    runOnUiThread(() -> loadChapter(prevChapter.getChapterId(), prevChapter.getTitle(), prevChapter.getChapterUrl()));
                } else runOnUiThread(() -> Toast.makeText(this, "No previous chapter found", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void loadChapter(String chapterId, String chapterTitle, String chapterUrl) {
        setCurrentChapterId(chapterId);
        setCurrentChapterTitle(chapterTitle);
        tvChapterNumber.setText(chapterTitle);
        chapters.clear();
        chapterPageAdapter.notifyDataSetChanged();
        recycleViewPage.scrollToPosition(0);
        SwitchFeedPages(chapterId, chapterUrl);
        hideUI();
    }

    private void ChapterRefresh() {
        btnRefresh.setOnClickListener(v ->
                SwitchFeedPages(getCurrentChapterId(), getIntent().getStringExtra("chapterUrl"))
        );
    }

    private void SetMangaTitle(String mangaId) {
        AppDatabase db = AppDatabase.getInstance(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            MangaItemEntity manga = db.mangaItemDao().getMangaById(mangaId);
            if (manga != null) runOnUiThread(() -> tvMangaTitle.setText(manga.getTitle()));
        });
        tvMangaTitle.setOnClickListener(v -> GoToMangaItem(mangaId));
    }

    private void GoToMangaItem(String mangaId) {
        Intent intent = new Intent(this, MangaPage.class);
        intent.putExtra("mangaId", mangaId);
        startActivity(intent);
    }

    private void GoToHomePage() {
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ChapterPage.this, HomePage.class);
            startActivity(intent);
        });
    }

    private void updateLoadChapterListMangaDex(int offset) {
        String mangaId = getIntent().getStringExtra("mangaId");
        int LIMIT = 100;
        AppDatabase db = AppDatabase.getInstance(this);

        ChaptersService.fetchAllChapters(mangaId, "desc", offset, LIMIT, new ChaptersService.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> fetchedChapters) {
                if (fetchedChapters.isEmpty()) return;
                Executors.newSingleThreadExecutor().execute(() -> {
                    List<ChapterItemEntity> entities = new ArrayList<>();
                    for (ChapterModel c : fetchedChapters) {
                        entities.add(new ChapterItemEntity(
                                c.getChapterId(),
                                mangaId,
                                c.getTitle(),
                                c.getNumber(),
                                c.getChapterUrl()
                        ));
                    }
                    db.chapterDao().insertChapters(entities);
                });
                if (fetchedChapters.size() == LIMIT) updateLoadChapterListMangaDex(offset + LIMIT);
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(ChapterPage.this, "Error loading chapters: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateLoadChapterListAsuraScans() {
        AppDatabase db = AppDatabase.getInstance(this);
        String mangaUrl = getIntent().getStringExtra("mangaUrl");
        Log.e("ChapterPage", "mangaUrl: " + mangaUrl);
        AsuraScraperTask.getMangaChaptersAsuraScans(mangaUrl, new AsuraScraperTask.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> fetchedChapters) {
                if (fetchedChapters.isEmpty()) return;

                Executors.newSingleThreadExecutor().execute(() -> {
                    List<ChapterItemEntity> entities = new ArrayList<>();
                    for (ChapterModel c : fetchedChapters) {
                        entities.add(new ChapterItemEntity(
                                c.getChapterId(),      // Primary key
                                getIntent().getStringExtra("mangaId"),
                                c.getTitle(),
                                c.getNumber(),
                                c.getChapterUrl()
                        ));
                    }

                    db.chapterDao().insertChapters(entities);
                });

                // Optional: If you want pagination for Asura feed (not really needed usually)
                // AsuraScraperTask.getMangaChaptersAsuraScans could support a page parameter in future
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(ChapterPage.this, "Error loading chapters: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void setupRecyclerScroll() {
        // Toggle UI with button
        btnToggleUI.setOnClickListener(v -> toggleUiVisibility());

        // Toggle UI with tap on RecyclerView
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleUiVisibility();
                return true;
            }
        });
        recycleViewPage.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        // Show UI when scrolling to bottom
        recycleViewPage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy > 0 && !rv.canScrollVertically(1)) setUiVisibility(true);
            }
        });

        // Start hidden
        setUiVisibility(false);
    }

    private void toggleUiVisibility() {
        setUiVisibility(upperPartLayout.getVisibility() != View.VISIBLE);
    }

    private void setUiVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        upperPartLayout.setVisibility(visibility);
        lowerPartLayout.setVisibility(visibility);
        btnToggleUI.setImageResource(visible ? R.drawable.ic_visibility_on : R.drawable.ic_visibility_off);
    }

    private void hideUI() { setUiVisibility(false); }
}
