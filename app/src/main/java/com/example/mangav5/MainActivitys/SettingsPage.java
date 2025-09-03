package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangav5.Adapters.SettingsAdapter;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.R;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SettingsPage extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<MangaItemEntity> mangaList = new ArrayList<>();
    private Button button_home, button_clear_all;
    private TextView text_total_size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_settings_page);

        recyclerView = findViewById(R.id.recycler_settings_manga);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        text_total_size = findViewById(R.id.text_total_size);

        adapter = new SettingsAdapter(mangaList, this, text_total_size);
        recyclerView.setAdapter(adapter);

        button_home = findViewById(R.id.button_home);
        button_clear_all = findViewById(R.id.button_clear_all);

        loadDataFromDB();
        setupHomeButton();
        setupDeleteAllButton();
    }

    private void setupHomeButton() {
        button_home.setOnClickListener(v -> {
            startActivity(new Intent(SettingsPage.this, HomePage.class));
        });
    }

    private void setupDeleteAllButton() {
        button_clear_all.setOnClickListener(v -> Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(SettingsPage.this);
            db.mangaItemDao().deleteAllManga();
            db.chapterDao().deleteChapters();
            mangaList.clear();
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.notifyDataSetChanged();
                text_total_size.setText("0.000 KB");
            });
        }));
    }

    private void loadDataFromDB() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<MangaItemEntity> data = db.mangaItemDao().getAllManga();

            new Handler(Looper.getMainLooper()).post(() -> {
                mangaList.clear();
                mangaList.addAll(data);
                adapter.notifyDataSetChanged();
                calculateTotalSize();
            });
        });
    }

    private void calculateTotalSize() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            double totalBytes = 0;

            for (MangaItemEntity manga : mangaList) {
                // Manga info
                totalBytes += manga.getCoverUrl() != null ? manga.getCoverUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getTitle() != null ? manga.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getMangaId() != null ? manga.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getDescription() != null ? manga.getDescription().getBytes(StandardCharsets.UTF_8).length : 0;

                // Chapters
                List<ChapterItemEntity> chapters = db.chapterDao().getChaptersByMangaId(manga.getMangaId());
                for (ChapterItemEntity chapter : chapters) {
                    totalBytes += chapter.getChapterId() != null ? chapter.getChapterId().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += chapter.getNumber() != null ? chapter.getNumber().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += chapter.getTitle() != null ? chapter.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                }
            }

            String sizeText = formatSizeFromBytes(totalBytes);

            new Handler(Looper.getMainLooper()).post(() -> text_total_size.setText(sizeText));
        });
    }

    private String formatSizeFromBytes(double bytes) {
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;

        if (gb >= 1) return String.format("%.2f GB", gb);
        else if (mb >= 1) return String.format("%.2f MB", mb);
        else if (kb >= 1) return String.format("%.2f KB", kb);
        else return String.format("%.2f B", bytes);
    }
}
