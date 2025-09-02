package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.ChapterPageAdapter;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.Services.ChaptersService;
import com.example.mangav5.Services.FeedMangaService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChapterPage extends AppCompatActivity {
    private RecyclerView recycleViewPage;
    private TextView tvChapterNumber;
    private Button btnPrevious;
    private Button btnHome;
    private Button btnNext;
    private TextView tvMangaTitle;
    ChapterPageAdapter chapterPageAdapter;
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
        chapterPageAdapter = new ChapterPageAdapter(chapters ,ChapterPage.this,chapterTitle);
        recycleViewPage.setAdapter(chapterPageAdapter);

        tvChapterNumber = findViewById(R.id.chapterNumber);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        tvMangaTitle = findViewById(R.id.mangaTitle);
        btnHome = findViewById(R.id.btnHome);



        tvChapterNumber.setText(chapterTitle);
        GetChapterPages(chapterId);
        NextChapter();
        PrevChapter();
        InsertChapterIntoHistory();
        SetMangaTitle();
        GoToHomePage();
    }


    private void SetMangaTitle(){
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

    public void GoToMangaItem(String mangaId){
        Intent intent = new Intent(this, MangaPage.class);
        intent.putExtra("mangaId",mangaId);
        this.startActivity(intent);
    }

    private void GoToHomePage(){
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ChapterPage.this, HomePage.class);
            startActivity(intent);
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
                    });
                    Log.e("ChapterPage", "Prev chapter fetched successfully: " + prevChapter.getTitle());
                } else {
                    Log.e("ChapterPage", "No prev chapter found");
                }
            });
        });
    }




    private void GetChapterPages(String chapterId){
        ChaptersService.fetchChapterPages(chapterId, new ChaptersService.PagesCallback(){
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

    private void InsertChapterIntoHistory(){
        String mangaId = getIntent().getStringExtra("mangaId");
        String chapterId = getIntent().getStringExtra("chapterId");
        String chapterTitle = getIntent().getStringExtra("chapterTitle");

        FeedMangaService.fetchMangaById(mangaId, new FeedMangaService.MangaCallback() {

            @Override
            public void onSuccess(MangaItemModel manga) {
                AppDatabase db = AppDatabase.getInstance(ChapterPage.this);

                Executors.newSingleThreadExecutor().execute(() -> {
                    HistoryEntity historyItem = new HistoryEntity(manga.getMangaId(),getCurrentChapterId(), getCurrentChapterTitle(), manga.getCoverImageUrl(), manga.getDescription(), System.currentTimeMillis(), manga.getTitle());
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
