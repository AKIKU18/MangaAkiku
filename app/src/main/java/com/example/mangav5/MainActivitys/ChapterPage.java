package com.example.mangav5.MainActivitys;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.ChapterPageAdapter;
import com.example.mangav5.R;
import com.example.mangav5.Services.ChaptersService;

import java.util.ArrayList;
import java.util.List;

public class ChapterPage extends AppCompatActivity {
    private RecyclerView recycleViewPage;
    private TextView tvChapterNumber;
    private Button btnPrevious;
    private Button btnNext;
    ChapterPageAdapter chapterPageAdapter;
    private List<String> chapters = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String chapterTittle = getIntent().getStringExtra("chapterTitle");

        setContentView(R.layout.activity_chapter_page);
        recycleViewPage = findViewById(R.id.recyclerPages);
        recycleViewPage.setLayoutManager(new LinearLayoutManager(this));
        chapterPageAdapter = new ChapterPageAdapter(chapters ,ChapterPage.this,chapterTittle);
        recycleViewPage.setAdapter(chapterPageAdapter);

        tvChapterNumber = findViewById(R.id.chapterNumber);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        String chapterId = getIntent().getStringExtra("chapterId");
        tvChapterNumber.setText(chapterTittle);
        GetChapterPages(chapterId);
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
