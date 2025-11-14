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
    AppDatabase db = AppDatabase.getInstance(this);

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
        goToHomePage();
        setupDeleteAllButton();
    }

    private void goToHomePage() {
        button_home.setOnClickListener(v -> {
            startActivity(new Intent(SettingsPage.this, HomePage.class));
        });
    }

    private void setupDeleteAllButton() {
        button_clear_all.setOnClickListener(v -> Executors.newSingleThreadExecutor().execute(() -> {
            db = AppDatabase.getInstance(SettingsPage.this);
            db.mangaItemDao().deleteAllManga();
            db.chapterDao().deleteChapters();
            db.bookmarkDao().deleteAllBookmarks();
            db.historyDao().deleteAllHistory();
            mangaList.clear();
            new Handler(Looper.getMainLooper()).post(() -> {
                text_total_size.setText("0.000 KB");
                adapter.notifyDataSetChanged();
            });
        }));
    }

    private void loadDataFromDB() {
        Executors.newSingleThreadExecutor().execute(() -> {
            db = AppDatabase.getInstance(this);
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
            db = AppDatabase.getInstance(this);
            double totalBytes = 0;

            for (MangaItemEntity manga : mangaList) {
                // Manga info
                totalBytes += manga.getCoverUrl() != null ? manga.getCoverUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getTitle() != null ? manga.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getMangaId() != null ? manga.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getDescription() != null ? manga.getDescription().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getLastChapter() != null ? manga.getLastChapter().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getSource() != null ? manga.getSource().getBytes(StandardCharsets.UTF_8).length : 0;

                // Chapters
                List<ChapterItemEntity> chapters = db.chapterDao().getChaptersByMangaId(manga.getMangaId());
                for (ChapterItemEntity chapter : chapters) {
                    totalBytes += chapter.getChapterId() != null ? chapter.getChapterId().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += chapter.getNumber() != null ? chapter.getNumber().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += chapter.getTitle() != null ? chapter.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += chapter.getChapterUrl() != null ? chapter.getChapterUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += chapter.getMangaId() != null ? chapter.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += chapter.getSource() != null ? chapter.getSource().getBytes(StandardCharsets.UTF_8).length : 0;
                }

                //History
                List<com.example.mangav5.Entity.HistoryEntity> history = db.historyDao().getAllHistory();
                for (com.example.mangav5.Entity.HistoryEntity h : history) {
                    totalBytes += h.getMangaId() != null ? h.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += h.getChapterId() != null ? h.getChapterId().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += h.getChapterTitle() != null ? h.getChapterTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += h.getCoverUrl() != null ? h.getCoverUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += h.getDescription() != null ? h.getDescription().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += String.valueOf(h.getTimestamp()).getBytes(StandardCharsets.UTF_8).length;
                    totalBytes += h.getMangaTitle() != null ? h.getMangaTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += h.getMangaUrl() != null ? h.getMangaUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += h.getChapterUrl() != null ? h.getChapterUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += h.getSource() != null ? h.getSource().getBytes(StandardCharsets.UTF_8).length : 0;
                    totalBytes += String.valueOf(h.getScrollPosition()).getBytes(StandardCharsets.UTF_8).length;
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
