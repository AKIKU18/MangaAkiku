package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.ChapterPageAdapter;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.R;
import com.example.mangav5.Services.ChaptersService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ChapterPage extends AppCompatActivity {
    private RecyclerView recycleViewPage;
    private TextView tvChapterNumber;
    private Button btnPrevious;
    private Button btnNext;
    ChapterPageAdapter chapterPageAdapter;
    private List<String> chapters = new ArrayList<>();

    private String currentChapterId;

    public String getCurrentChapterId() {
        return currentChapterId;
    }

    public void setCurrentChapterId(String chapterId) {
        this.currentChapterId = chapterId;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String chapterTittle = getIntent().getStringExtra("chapterTitle");
        String mangaId = getIntent().getStringExtra("mangaId");
        String chapterId = getIntent().getStringExtra("chapterId"); // initial chapter
        Log.e("ChapterPage", "Manga ID: " + mangaId);
        Log.e("ChapterPage", "Chapter ID: " + chapterId);
        Log.e("ChapterPage", "Chapter Title: " + chapterTittle);
        setCurrentChapterId(chapterId);

        setContentView(R.layout.activity_chapter_page);
        recycleViewPage = findViewById(R.id.recyclerPages);
        recycleViewPage.setLayoutManager(new LinearLayoutManager(this));
        chapterPageAdapter = new ChapterPageAdapter(chapters ,ChapterPage.this,chapterTittle);
        recycleViewPage.setAdapter(chapterPageAdapter);

        tvChapterNumber = findViewById(R.id.chapterNumber);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        tvChapterNumber.setText(chapterTittle);
        GetChapterPages(chapterId);
        NextChapter();
        PrevChapter();
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


}
