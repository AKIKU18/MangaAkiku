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
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.R;

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
                text_total_size.setText("0.000 MB");
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
            long totalBytes = mangaList.stream().flatMap(manga -> db.chapterDao().getChaptersByMangaId(manga.getMangaId()).stream())
                    .mapToLong(chapter -> {
                        long size = 0;
                        size += chapter.getChapterId() != null ? chapter.getChapterId().getBytes().length : 0;
                        size += chapter.getNumber() != null ? chapter.getNumber().getBytes().length : 0;
                        size += chapter.getTitle() != null ? chapter.getTitle().getBytes().length : 0;
                        return size;
                    }).sum();

            double sizeMB = totalBytes / 1024.0 / 1024.0;
            new Handler(Looper.getMainLooper()).post(() -> text_total_size.setText(String.format("%.3f MB", sizeMB)));
        });
    }
}
