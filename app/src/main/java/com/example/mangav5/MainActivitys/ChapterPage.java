package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterPage extends AppCompatActivity {

    private static final String TAG = "ChapterPage";
    private static final long IMAGE_LOADING_TIMEOUT_MS = 15000;

    private final List<String> chapters = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    AppDatabase db;

    private RecyclerView recycleViewPage;
    private ChapterPageAdapter chapterPageAdapter;
    private TextView tvChapterNumber, tvMangaTitle;
    private Button btnPrevious, btnNext, btnHome;
    private ImageButton btnRefresh, btnToggleUI;
    private ConstraintLayout upperPartLayout, lowerPartLayout;
    private FrameLayout overlayRefreshContainer;
    private Button overlayRefreshButton;
    private TextView overlayRefreshText;
    private ProgressBar overlayLoadingProgress;

    private String currentChapterId, currentChapterUrl, currentChapterTitle;
    private boolean bookmarkChanged = false;
    private String mangaId, source, mangaUrl, chapterId, chapterUrl, chapterTitle, chapterNumber;
    private boolean uiVisible = true;
    private int scrollPosition;

    // image loading state
    private boolean firstImageLoaded = false;
    private boolean firstImageFailed = false;
    private Runnable imageTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_page);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = AppDatabase.getInstance(this);

        initViews();
        setupRecyclerView();
        handleBackPress();
        setupRecyclerScroll();
        getIntentData();

        setData(chapterId, chapterUrl, chapterTitle);

        String chapterUrlOrIdFinal = ServiceController.getChapterIdOrChapterUrl(source, chapterId, chapterUrl);

        showLoadingOverlay("Loading chapter...");
        GetChapterPages(chapterUrlOrIdFinal);

        ChapterRefresh();
        NextChapter();
        PrevChapter();
        SetMangaTitle(mangaId);
        GoToHomePage();
        updateLoadChapterList(0);
        FullScreenMode();
        OverlayRefreshedButton();
        setupRecyclerScrollListener();
    }

    private int GetChapterNumber(String chapterTitle) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(chapterTitle);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }

        return 0;
    }

    private void setData(String chapterId, String chapterUrl, String chapterTitle) {
        setCurrentChapterId(chapterId);
        setCurrentChapterUrl(chapterUrl);
        setCurrentChapterTitle(chapterTitle);
        tvChapterNumber.setText(chapterTitle);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "Missing chapter data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mangaId = intent.getStringExtra("mangaId");
        source = intent.getStringExtra("source");
        mangaUrl = intent.getStringExtra("mangaUrl");
        chapterId = intent.getStringExtra("chapterId");
        chapterTitle = intent.getStringExtra("chapterTitle");
        chapterUrl = intent.getStringExtra("chapterUrl");
        chapterNumber = intent.getStringExtra("chapterNumber");

        if (chapterId == null || chapterUrl == null) {
            Toast.makeText(this, "Chapter data incomplete", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loaded Intent → MangaId: " + mangaId + " | ChapterId: " + chapterId);
    }

    private void initViews() {
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
        overlayRefreshContainer = findViewById(R.id.overlay_refresh_container);
        overlayRefreshButton = findViewById(R.id.btn_overlay_refresh);
        overlayRefreshText = findViewById(R.id.overlay_refresh_text);
        overlayLoadingProgress = findViewById(R.id.overlay_loading_progress);
    }

    private void setupRecyclerView() {
        recycleViewPage.setLayoutManager(new LinearLayoutManager(this));

        chapterPageAdapter = new ChapterPageAdapter(chapters, this, "");
        chapterPageAdapter.setOnImageStateListener(new ChapterPageAdapter.OnImageStateListener() {
            @Override
            public void onFirstImageLoadSuccess() {
                runOnUiThread(() -> {
                    firstImageLoaded = true;
                    firstImageFailed = false;
                    cancelImageLoadingTimeout();
                    hideOverlay();
                    Log.d(TAG, "First image loaded successfully");
                });
            }

            @Override
            public void onFirstImageLoadFailed(String failedUrl) {
                runOnUiThread(() -> {
                    if (!firstImageLoaded) {
                        firstImageFailed = true;
                        cancelImageLoadingTimeout();
                        showRetryOverlay("Failed to load chapter image.\nTap below to retry.");
                        Log.e(TAG, "First image failed: " + failedUrl);
                    }
                });
            }
        });

        recycleViewPage.setAdapter(chapterPageAdapter);
    }

    private void FullScreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    private void resetImageLoadingFlags() {
        firstImageLoaded = false;
        firstImageFailed = false;
        chapterPageAdapter.resetLoadingState();
        cancelImageLoadingTimeout();
    }

    private void startImageLoadingTimeout() {
        cancelImageLoadingTimeout();

        imageTimeoutRunnable = () -> {
            if (!firstImageLoaded && !isDestroyed() && !isFinishing()) {
                showRetryOverlay("Loading took too long.\nTap below to retry.");
                Log.e(TAG, "Image loading timeout");
            }
        };

        uiHandler.postDelayed(imageTimeoutRunnable, IMAGE_LOADING_TIMEOUT_MS);
    }

    private void cancelImageLoadingTimeout() {
        if (imageTimeoutRunnable != null) {
            uiHandler.removeCallbacks(imageTimeoutRunnable);
            imageTimeoutRunnable = null;
        }
    }

    private boolean isSupportedImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif")
                || lower.contains(".jpg?")
                || lower.contains(".jpeg?")
                || lower.contains(".png?")
                || lower.contains(".webp?")
                || lower.contains(".gif?");
    }

    private void reloadCurrentChapter() {
        String idOrUrl = ServiceController.getChapterIdOrChapterUrl(source, currentChapterId, currentChapterUrl);
        showLoadingOverlay("Reloading chapter...");
        GetChapterPages(idOrUrl);
    }

    private void GetChapterPages(String chapterUrlOrId) {
        Log.d(TAG, "Fetching pages for: " + chapterUrlOrId);

        resetImageLoadingFlags();
        showLoadingOverlay("Loading chapter pages...");

        ServiceController.getChapterPages(this, source, chapterUrlOrId, new ServiceController.PagesCallback() {
            @Override
            public void onSuccess(List<String> pages) {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;

                    chapters.clear();

                    if (pages != null && !pages.isEmpty()) {
                        for (String url : pages) {
                            if (isSupportedImageUrl(url)) {
                                chapters.add(url);
                            }
                        }
                    }

                    if (chapters.isEmpty()) {
                        showRetryOverlay("No valid chapter images found.\nTap below to retry.");
                        Log.e(TAG, "No valid image URLs after filtering");
                        return;
                    }

                    chapterPageAdapter.notifyDataSetChanged();
                    startImageLoadingTimeout();
                    InsertChapterIntoHistory(chapterUrlOrId);

                    Log.d(TAG, "Valid image URLs count: " + chapters.size());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    Toast.makeText(ChapterPage.this, "Failed to load pages", Toast.LENGTH_SHORT).show();
                    showRetryOverlay("Failed to fetch chapter pages.\nTap below to retry.");
                    Log.e(TAG, "Error fetching pages: " + message);
                });
            }
        });
    }

    private void InsertChapterIntoHistory(String chapterUrlOrId) {
        final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(source, mangaId, mangaUrl);
        ServiceController.fetchMangaDetails(this, source, mangaIdOrUrlFinal, new ServiceController.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) {
                int scrollY = recycleViewPage.getScrollY();
                safeExecute(() -> {
                    HistoryEntity historyItem = new HistoryEntity(
                            manga.getMangaId(),
                            getCurrentChapterId(),
                            getCurrentChapterTitle(),
                            manga.getCoverImageUrl(),
                            manga.getDescription(),
                            System.currentTimeMillis(),
                            manga.getTitle(),
                            manga.getMangaUrl(),
                            chapterUrlOrId,
                            manga.getSource(),
                            scrollY
                    );
                    db.historyDao().insertHistoryItem(historyItem);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to fetch manga for history: " + errorMessage);
            }
        });
    }

    private void showLoadingOverlay(String text) {
        overlayRefreshContainer.setVisibility(View.VISIBLE);
        overlayLoadingProgress.setVisibility(View.VISIBLE);
        overlayRefreshButton.setVisibility(View.GONE);
        overlayRefreshText.setText(text);
    }

    private void showRetryOverlay(String text) {
        overlayRefreshContainer.setVisibility(View.VISIBLE);
        overlayLoadingProgress.setVisibility(View.GONE);
        overlayRefreshButton.setVisibility(View.VISIBLE);
        overlayRefreshText.setText(text);
    }

    private void hideOverlay() {
        overlayRefreshContainer.setVisibility(View.GONE);
    }

    private void OverlayRefreshedButton() {
        overlayRefreshButton.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing chapter...", Toast.LENGTH_SHORT).show();
            reloadCurrentChapter();
        });
    }

    private void ChapterRefresh() {
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing chapter...", Toast.LENGTH_SHORT).show();
            reloadCurrentChapter();
        });
    }

    private void NextChapter() {
        btnNext.setOnClickListener(v -> {
            final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(source, mangaId, mangaUrl);
            safeExecute(() -> {
                ChapterItemEntity next = db.chapterDao().getNextChapter(mangaIdOrUrlFinal, currentChapterId);
                runOnUiThread(() -> {
                    if (next != null) {
                        chapterId = next.chapterId;
                        chapterUrl = next.chapterUrl;
                        chapterTitle = next.title;
                        loadChapter(next.getChapterId(), next.getTitle(), next.getChapterUrl(), next.getMangaId(), next.getSource(), mangaUrl);
                        Toast.makeText(this, "Next: " + next.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "This is the latest chapter", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    private void PrevChapter() {
        btnPrevious.setOnClickListener(v -> {
            final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(source, mangaId, mangaUrl);
            safeExecute(() -> {
                ChapterItemEntity prev = db.chapterDao().getPrevChapter(mangaIdOrUrlFinal, currentChapterId);
                runOnUiThread(() -> {
                    if (prev != null) {
                        loadChapter(prev.getChapterId(), prev.getTitle(), prev.getChapterUrl(), prev.getMangaId(), prev.getSource(), mangaUrl);
                        chapterId = prev.chapterId;
                        chapterUrl = prev.chapterUrl;
                        chapterTitle = prev.title;
                        Toast.makeText(this, "Previous: " + prev.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "This is the first chapter", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    private void loadChapter(String chapterId, String chapterTitle, String chapterUrl, String _mangaId, String _source, String _mangaUrl) {
        hideUI();
        showLoadingOverlay("Loading chapter...");

        setCurrentChapterId(chapterId);
        setCurrentChapterTitle(chapterTitle);
        setCurrentChapterUrl(chapterUrl);
        tvChapterNumber.setText(chapterTitle);

        Intent intent = new Intent(this, ChapterPage.class);
        intent.putExtra("chapterId", getCurrentChapterId());
        intent.putExtra("chapterTitle", getCurrentChapterTitle());
        intent.putExtra("mangaId", mangaId);
        intent.putExtra("mangaUrl", mangaUrl);
        intent.putExtra("chapterUrl", chapterUrl);
        intent.putExtra("source", source);
        this.startActivity(intent);

        runOnUiThread(() -> {
            mangaId = _mangaId;
            source = _source;
            mangaUrl = _mangaUrl;
            chapters.clear();
            chapterPageAdapter.notifyDataSetChanged();
        });

        String chapterUrlOrIdFinal = ServiceController.getChapterIdOrChapterUrl(source, chapterId, chapterUrl);
        GetChapterPages(chapterUrlOrIdFinal);
    }

    private void SetMangaTitle(String mangaId) {
        safeExecute(() -> {
            MangaItemEntity manga = db.mangaItemDao().getMangaById(mangaId);

            if (manga == null) {
                Log.e(TAG, "Manga not found in DB for mangaId: " + mangaId);
                return;
            }

            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                tvMangaTitle.setText(manga.getTitle());
                tvMangaTitle.setOnClickListener(v -> GoToMangaItem(mangaId));
            });
        });
    }

    private void GoToMangaItem(String mangaId) {
        Intent intent = new Intent(this, MangaPage.class);
        intent.putExtra("mangaId", mangaId);
        intent.putExtra("source", source);
        intent.putExtra("mangaUrl", mangaUrl);
        startActivity(intent);
    }

    private void GoToHomePage() {
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, HomePage.class)));
    }

    private void updateLoadChapterList(int offset) {
        String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(source, mangaId, mangaUrl);
        int LIMIT = 100;

        ServiceController.fetchChapterListController(this, source, mangaIdOrUrlFinal, offset, LIMIT, "desc",
                new ServiceController.ChapterListCallback() {
                    @Override
                    public void onSuccess(List<ChapterModel> chapterList) {
                        if (chapterList.isEmpty()) return;

                        safeExecute(() -> {
                            List<ChapterItemEntity> entities = new ArrayList<>();
                            for (ChapterModel c : chapterList) {
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
                            Log.d(TAG, "Inserted " + entities.size() + " chapters");
                        });

                        if (chapterList.size() == LIMIT) updateLoadChapterList(offset + LIMIT);
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() ->
                                Toast.makeText(ChapterPage.this, "Error loading chapters: " + message, Toast.LENGTH_SHORT).show());
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

    private void setupRecyclerScroll() {
        btnToggleUI.setOnClickListener(v -> toggleUiVisibility());

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

        recycleViewPage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy > 0 && !rv.canScrollVertically(1)) setUiVisibility(true);
            }
        });

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
        uiVisible = visible;
    }

    private void hideUI() {
        setUiVisibility(false);
    }

    private void setupRecyclerScrollListener() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recycleViewPage.getLayoutManager();

        recycleViewPage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (layoutManager == null) return;

                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                View firstVisibleItemView = layoutManager.findViewByPosition(firstVisibleItemPosition);

                int offset = 0;
                if (firstVisibleItemView != null) {
                    offset = -firstVisibleItemView.getTop();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        cancelImageLoadingTimeout();

        try {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    private void safeExecute(Runnable task) {
        if (!isDestroyed() && !isFinishing() && !executor.isShutdown() && !executor.isTerminated()) {
            executor.execute(task);
        }
    }

    private void setCurrentChapterUrl(String url) {
        this.currentChapterUrl = url;
    }

    private String getCurrentChapterId() {
        return currentChapterId;
    }

    private void setCurrentChapterId(String id) {
        this.currentChapterId = id;
    }

    private String getCurrentChapterTitle() {
        return currentChapterTitle;
    }

    private void setCurrentChapterTitle(String title) {
        this.currentChapterTitle = title;
    }
}