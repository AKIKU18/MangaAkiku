package com.example.mangav5.MainActivitys;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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
    private Button button_home;
    private Button button_clear_all;
    private TextView text_total_size;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_page);

        recyclerView = findViewById(R.id.recycler_settings_manga);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        text_total_size = findViewById(R.id.text_total_size);

        adapter = new SettingsAdapter(mangaList, this, text_total_size);
        recyclerView.setAdapter(adapter);

        button_home = findViewById(R.id.button_home);
        button_clear_all = findViewById(R.id.button_clear_all);

        loadDataFromDB();
        HomeButtonGoTo();
        DeleteAllManga();

    }

    private void CalculateTotalSize() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            long totalBytes = 0;

            for (MangaItemEntity manga : mangaList) {
                String mangaId = manga.getMangaId();
                List<ChapterItemEntity> chapters = db.chapterDao().getChaptersByMangaId(mangaId);


                for (ChapterItemEntity chapter : chapters) {
                    // Chapter ID size (if stored as string)
                    if (chapter.getChapterId() != null)
                        totalBytes += chapter.getChapterId().getBytes(StandardCharsets.UTF_8).length;

                    // Manga ID size (int)
                    totalBytes += mangaId.getBytes(StandardCharsets.UTF_8).length;;

                    // Title size
                    if (chapter.getTitle() != null)
                        totalBytes += chapter.getTitle().getBytes(StandardCharsets.UTF_8).length;

                    // Number size (int)
                    totalBytes += chapter.getNumber().getBytes(StandardCharsets.UTF_8).length;;

                }


            }

            double sizeMB = totalBytes / 1024.0 / 1024.0;
            // <-- POST TO MAIN THREAD
            new Handler(Looper.getMainLooper()).post(() -> {
                text_total_size.setText(String.format("%.3f MB", sizeMB));
            });
            Log.d("TOTAL_SIZE", "Total metadata size: " + sizeMB + " MB");

        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void DeleteAllManga(){
        button_clear_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppDatabase db = AppDatabase.getInstance(SettingsPage.this);
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.mangaItemDao().deleteAllManga();
                    db.chapterDao().deleteChapters();
                    mangaList.clear();
                    runOnUiThread(() -> {
                        mangaList.clear();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            text_total_size.setText(String.format("%.2f MB", 0.0));
                        });
                        adapter.notifyDataSetChanged();
                    });


                });
            };
        });
    }

    private void HomeButtonGoTo(){
        button_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsPage.this, HomePage.class);
                startActivity(intent);
            }
        });
    }

    private void loadDataFromDB() {
        AppDatabase db = AppDatabase.getInstance(this);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<MangaItemEntity> data = db.mangaItemDao().getAllManga();

            // Switch to main thread for UI updates
            runOnUiThread(() -> {
                mangaList.clear();
                mangaList.addAll(data);
                CalculateTotalSize();
                adapter.notifyDataSetChanged();
            });
        });
    }
}
