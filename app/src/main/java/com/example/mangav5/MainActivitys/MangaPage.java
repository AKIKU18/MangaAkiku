package com.example.mangav5.MainActivitys;

import android.app.Service;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Adapters.MangaPageAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.ServiceMaster.ServiceController;
import com.example.mangav5.ServicesMangaDex.MangaDexFeedManga;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MangaPage extends AppCompatActivity {
    RecyclerView chapterRecyclerView;
    MangaPageAdapter mangaPageAdapter;
    List<ChapterModel> chapterList;
    ImageView cover, bookmarkStar;
    TextView title, description;
    Button firstChapterButtonPage, lastChapterButtonPage;
    ImageButton scrollToBottomButton;
    Button button_homeMangaPage;

    private Boolean isLoading = false;
    private final int LIMIT = 100;
    private MangaItemModel mangaItem;
    private String getMangaId;
    private String getMangaSource;
    private BookmarkDao bookmarkDao;
    private boolean bookmarkChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_manga_page);

        getMangaId = getIntent().getStringExtra("mangaId");
        getMangaSource = getIntent().getStringExtra("source");
        AppDatabase db = AppDatabase.getInstance(this);
        this.bookmarkDao = db.bookmarkDao();

        chapterList = new ArrayList<>();
        chapterRecyclerView = findViewById(R.id.chaptersRecyclerViewPage);
        chapterRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mangaPageAdapter = new MangaPageAdapter(chapterList, this, getMangaId, getIntent().getStringExtra("mangaUrl"));
        chapterRecyclerView.setAdapter(mangaPageAdapter);

        cover = findViewById(R.id.mangaCoverImagePage);
        bookmarkStar = findViewById(R.id.bookmarkButtonPage);
        title = findViewById(R.id.mangaTitlePage);
        description = findViewById(R.id.mangaDescriptionPage);
        firstChapterButtonPage = findViewById(R.id.firstChapterButtonPage);
        lastChapterButtonPage = findViewById(R.id.lastChapterButtonPage);
        scrollToBottomButton = findViewById(R.id.scrollToBottomButtonPage);
        button_homeMangaPage = findViewById(R.id.button_homeMangaPage);

        PrevOrNextChapter();
        handleBackPress();
        OnClickToggleMangaPage(bookmarkStar, bookmarkDao);
        CheckIfStillBookmarked();
        ScrollButton();
        HomePageGoTo();
        LoadMangaInfo();
    }

    private void ScrollButton(){
        scrollToBottomButton.setOnClickListener(v -> {
            int itemCount = chapterRecyclerView.getAdapter() != null ? chapterRecyclerView.getAdapter().getItemCount() : 0;
            if (itemCount > 0) chapterRecyclerView.smoothScrollToPosition(itemCount - 1);
        });
    }

    private void CheckIfStillBookmarked() {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean isBookmarked = bookmarkDao.isBookmarked(getMangaId);
            new Handler(Looper.getMainLooper()).post(() -> {
                bookmarkStar.setImageResource(isBookmarked ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
            });
        });
    }

    private void OnClickToggleMangaPage(ImageView holder, BookmarkDao bookmarkDao){

        final String mangaUrlorId = ServiceController.getMangaIdOrMangaUrl(getMangaSource, getMangaId, getIntent().getStringExtra("mangaUrl"));
        ServiceController.getMangaItem(getMangaSource, mangaUrlorId, new ServiceController.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) {
                runOnUiThread(() -> {
                    holder.setOnClickListener(v -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            ToggleBookmarkMangaPage(manga, bookmarkDao);
                            boolean isBookmarked = bookmarkDao.isBookmarked(mangaUrlorId);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                holder.setImageResource(isBookmarked ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
                            });
                        });
                    });
                    mangaItem = manga;
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MangaPage.this, "Error loading manga: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
                Log.e("MangaPage", "Error fetching manga: " + errorMessage);
            }

        });
    }

    private void ToggleBookmarkMangaPage(MangaItemModel manga, BookmarkDao bookmarkDao) {
        BookmarkEntity bookmark = new BookmarkEntity(manga.getMangaId(), manga.getTitle(), manga.getCoverImageUrl(), manga.getDescription(), manga.getMangaUrl(),manga.getSource());
        if (bookmarkDao.isBookmarked(manga.getMangaId())) {
            bookmarkDao.delete(bookmark);
            manga.setIsBookmarked(false);
        } else {
            bookmarkDao.insert(bookmark);
            manga.setIsBookmarked(true);
        }
        bookmarkChanged = true;
    }

    private void PrevOrNextChapter(){
        firstChapterButtonPage.setOnClickListener(v -> GetFirstOrLastChapter("asc"));
        lastChapterButtonPage.setOnClickListener(v -> GetFirstOrLastChapter("desc"));
    }

    private void GetFirstOrLastChapter(String descAsc){
        String mangaUrl = getIntent().getStringExtra("mangaUrl");
        String source = getIntent().getStringExtra("source");
        Log.e("SourceManga", source);

        final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(source,getMangaId, mangaUrl); // create final copy

        ServiceController.fetchChapterListController(source,mangaIdOrUrlFinal, 0, 1, descAsc, new ServiceController.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> fetchedChapters) {
                if (fetchedChapters.isEmpty()) return;

                ChapterModel firstChapter = fetchedChapters.get(0);

                // Safely pass mangaUrl
                String mangaUrl = getIntent().getStringExtra("mangaUrl");
                if (mangaUrl == null) mangaUrl = "";

                Intent intent = new Intent(MangaPage.this, ChapterPage.class);
                intent.putExtra("chapterId", firstChapter.getChapterId());
                intent.putExtra("chapterTitle", firstChapter.getTitle());
                intent.putExtra("mangaId", getMangaId);
                intent.putExtra("mangaUrl", mangaUrl);
                intent.putExtra("chapterUrl", firstChapter.getChapterUrl());
                intent.putExtra("source", source);
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(MangaPage.this, "Error: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void LoadMangaInfo(){
        String source = getIntent().getStringExtra("source");
        String mangaUrl = getIntent().getStringExtra("mangaUrl");
        Log.e("MangaPage", "mangaUrl: " + ServiceController.getMangaIdOrMangaUrl(source,getMangaId, mangaUrl));
        final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(source,getMangaId, mangaUrl); // create final copy

        ServiceController.fetchMangaDetails(source, mangaIdOrUrlFinal, new ServiceController.MangaCallback() {
            @Override
            public void onSuccess(MangaItemModel manga) {
                if (manga == null) return;

                AppDatabase db = AppDatabase.getInstance(MangaPage.this);
                Executors.newSingleThreadExecutor().execute(() -> {
                    MangaItemEntity mangaEntity = new MangaItemEntity(
                            manga.getMangaId(), manga.getTitle(), manga.getCoverImageUrl(), manga.getDescription(), manga.getMangaUrl(), manga.getLastChapter()
                    ,manga.getSource());

                    Log.d("MangaPage", "manga.getSource(): " + manga.getSource());
                    db.mangaItemDao().insertManga(mangaEntity);
                    List<ChapterItemEntity> savedChapters = db.chapterDao().getChaptersByMangaId(mangaIdOrUrlFinal);
                    runOnUiThread(() -> {
                        for (ChapterItemEntity c : savedChapters) {
                            if (chapterList.stream().noneMatch(ch -> ch.getChapterId().equals(c.getChapterId()))) {
                                chapterList.add(new ChapterModel(c.getChapterId(), c.getTitle(), c.getNumber(), c.getChapterUrl(),c.getSource()));
                            }
                        }
                        mangaPageAdapter.notifyDataSetChanged();
                    });

                    updateLoadChapterListInfo(0); // fetch remaining
                });

                runOnUiThread(() -> {
                    mangaItem = manga;
                    title.setText(manga.getTitle());
                    description.setText(manga.getDescription());
                    Glide.with(MangaPage.this)
                            .load(manga.getCoverImageUrl())
                            .placeholder(android.R.drawable.ic_dialog_info)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .dontTransform()
                            .error(android.R.drawable.ic_dialog_alert)
                            .into(cover);
                });
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void updateLoadChapterListInfo(int offset){
        String source = getIntent().getStringExtra("source");

        String mangaUrl = getIntent().getStringExtra("mangaUrl");
        final String mangaIdOrUrlFinal = ServiceController.getMangaIdOrMangaUrl(source,getMangaId, mangaUrl); // create final copy

        ServiceController.fetchChapterListController(source,mangaIdOrUrlFinal, offset, LIMIT, "desc", new ServiceController.ChapterListCallback() {
            @Override
            public void onSuccess(List<ChapterModel> chapters) {
                if (chapters.isEmpty()) return;

                AppDatabase db = AppDatabase.getInstance(MangaPage.this);
                Executors.newSingleThreadExecutor().execute(() -> {
                    List<ChapterItemEntity> entities = new ArrayList<>();
                    for (ChapterModel c : chapters) {
                        entities.add(new ChapterItemEntity(c.getChapterId(), mangaIdOrUrlFinal, c.getTitle(), c.getNumber(), c.getChapterUrl(),c.getSource()));
                    }
                    db.chapterDao().insertChapters(entities);
                });

                runOnUiThread(() -> {
                    for (ChapterModel c : chapters) {
                        if (chapterList.stream().noneMatch(ch -> ch.getChapterId().equals(c.getChapterId()))) {
                            chapterList.add(c);
                        }
                    }
                    mangaPageAdapter.notifyDataSetChanged();
                });

                if (chapters.size() == LIMIT) updateLoadChapterListInfo(offset + LIMIT);
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
            Intent resultIntent = new Intent(this, HomePage.class);
            resultIntent.putExtra("bookmarkChanged", bookmarkChanged);
            setResult(RESULT_OK, resultIntent);
            startActivity(resultIntent);
            finish();
        });
    }
}
