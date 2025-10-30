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
import com.example.mangav5.ServiceMaster.ServiceController;
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
        GetChapterPages();

        // --- Setup UI ---
        setupRecyclerScroll();
        ChapterRefresh();
        NextChapter();
        PrevChapter();
        SetMangaTitle(mangaId);
        GoToHomePage();
        updateLoadChapterList(0);
        InsertChapterIntoHistory();
    }

    private void GetChapterPages(){
        String source = getIntent().getStringExtra("source");
        String chapterUrlOrId;

        if ("MangaDex".equals(source)) {
            chapterUrlOrId = getCurrentChapterId(); // use current chapter ID
        } else {
            chapterUrlOrId = getIntent().getStringExtra("chapterUrl"); // fallback
        }

        final String chapterUrlOrUrlFinal = chapterUrlOrId;
        ServiceController.getChapterPages(this,source,chapterUrlOrUrlFinal, new ServiceController.PagesCallback() {
            @Override
            public void onSuccess(List<String> pages) {
                Log.e("ChapterPageAdapter", "Pages: " + pages);
                runOnUiThread(() -> {
                    chapters.clear();
                    chapters.addAll(pages);
                    InsertChapterIntoHistory();
                    chapterPageAdapter.notifyDataSetChanged();
                    Log.e("ChapterPage", "Pages fetched successfully: " + pages.size());
                });
            }
            @Override
            public void onError(String message) {

            }
        });
    }

    private void InsertChapterIntoHistory(){
        String mangaUrlOrId =getIntent().getStringExtra("mangaId"); // always start with mangaId
        String source = getIntent().getStringExtra("source");

        if ("MangaDex".equals(source)) {
            // Use only the ID for MangaDex
            mangaUrlOrId = getIntent().getStringExtra("mangaId");
        } else {
            // For other sources, fallback to mangaUrl
            if (getIntent().getStringExtra("mangaUrl") != null) {
                mangaUrlOrId = getIntent().getStringExtra("mangaUrl");
            }
        }

        Log.d("MangaPageLog", "Fetching manga: source=" + source + " , id/url=" + mangaUrlOrId);
        final String mangaIdOrUrlFinal = mangaUrlOrId; // create final copy
        ServiceController.fetchMangaDetails(source, mangaUrlOrId, new ServiceController.MangaCallback() {
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
                            manga.getTitle(),
                            manga.getMangaUrl(),
                            getIntent().getStringExtra("chapterUrl"),
                            manga.getSource()
                    );
                    db.historyDao().insertHistoryItem(historyItem);
                    Log.e("ChapterPage", "Chapter inserted into history: " + getCurrentChapterTitle());
                });
            }

            public void onError(String errorMessage) {}

        });
    }

    private void NextChapter() {
        btnNext.setOnClickListener(v -> {
            AppDatabase db = AppDatabase.getInstance(this);

            String mangaUrlOrId = getIntent().getStringExtra("mangaId");
            String source = getIntent().getStringExtra("source");

            if ("MangaDex".equals(source)) {
                // Use only the ID for MangaDex
                mangaUrlOrId = getIntent().getStringExtra("mangaId");
            } else {
                // For other sources, fallback to mangaUrl
                if (getIntent().getStringExtra("mangaUrl") != null) {
                    mangaUrlOrId = getIntent().getStringExtra("mangaUrl");
                }
            }
            final String mangaIdOrUrlFinal = mangaUrlOrId; // create final copy

            Executors.newSingleThreadExecutor().execute(() -> {
                ChapterItemEntity nextChapter = db.chapterDao().getNextChapter(mangaIdOrUrlFinal, getCurrentChapterId());
                if (nextChapter != null) {
                    runOnUiThread(() -> loadChapter(nextChapter.getChapterId(), nextChapter.getTitle(), nextChapter.getChapterUrl()));
                } else runOnUiThread(() -> Toast.makeText(this, "No next chapter found", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void PrevChapter() {
        btnPrevious.setOnClickListener(v -> {
            AppDatabase db = AppDatabase.getInstance(this);
            String mangaUrlOrId = getIntent().getStringExtra("mangaId");
            String source = getIntent().getStringExtra("source");

            if ("MangaDex".equals(source)) {
                // Use only the ID for MangaDex
                mangaUrlOrId = getIntent().getStringExtra("mangaId");
            } else {
                // For other sources, fallback to mangaUrl
                if (getIntent().getStringExtra("mangaUrl") != null) {
                    mangaUrlOrId = getIntent().getStringExtra("mangaUrl");
                }
            }
            final String mangaIdOrUrlFinal = mangaUrlOrId; // create final copy
            Executors.newSingleThreadExecutor().execute(() -> {
                ChapterItemEntity prevChapter = db.chapterDao().getPrevChapter(mangaIdOrUrlFinal, getCurrentChapterId());
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
        GetChapterPages();
        hideUI();
    }

    private void ChapterRefresh() {
        btnRefresh.setOnClickListener(v ->
                GetChapterPages()
        );

        btnRefresh.setOnClickListener(v ->
                Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
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
        intent.putExtra("source", getIntent().getStringExtra("source"));
        intent.putExtra("mangaUrl", getIntent().getStringExtra("mangaUrl"));
        startActivity(intent);
    }

    private void GoToHomePage() {
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ChapterPage.this, HomePage.class);
            startActivity(intent);
        });
    }

    private void updateLoadChapterList(int offset){
        String mangaUrlOrId = getIntent().getStringExtra("mangaId");
        String source = getIntent().getStringExtra("source");

        if ("MangaDex".equals(source)) {
            // Use only the ID for MangaDex
            mangaUrlOrId = getIntent().getStringExtra("mangaId");
        } else {
            // For other sources, fallback to mangaUrl
            if (getIntent().getStringExtra("mangaUrl") != null) {
                mangaUrlOrId = getIntent().getStringExtra("mangaUrl");
            }
        }
        final String mangaIdOrUrlFinal = mangaUrlOrId; // create final copy
        int LIMIT = 100;
        AppDatabase db = AppDatabase.getInstance(this);
        ServiceController.fetchChapterListController(source,mangaIdOrUrlFinal, mangaUrlOrId, 0, 100, "desc", new ServiceController.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) {


                if (chapters.isEmpty()) return;
                Executors.newSingleThreadExecutor().execute(() -> {
                    List<ChapterItemEntity> entities = new ArrayList<>();
                    for (ChapterModel c : chapters) {
                        entities.add(new ChapterItemEntity(
                                c.getChapterId(),
                                mangaIdOrUrlFinal,
                                c.getTitle(),
                                c.getNumber(),
                                c.getChapterUrl(),
                                c.getSource()
                        ));
                    }
                    db.chapterDao().insertChapters(entities);
                });
                if (chapters.size() == LIMIT) updateLoadChapterList(offset + LIMIT);
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
