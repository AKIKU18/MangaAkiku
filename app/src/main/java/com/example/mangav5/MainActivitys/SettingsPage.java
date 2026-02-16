package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Entity.SettingsItemEntity;
import com.example.mangav5.R;
import com.example.mangav5.ServiceMaster.GitHubUpdateManager;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

public class SettingsPage extends AppCompatActivity {
    AppDatabase db = AppDatabase.getInstance(this);
    TextView text_total_size;
    Button btn_home;
    FrameLayout item_storage_usage;
    FrameLayout item_clear_cache;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_page);
        text_total_size = findViewById(R.id.text_storage_size);
        btn_home = findViewById(R.id.button_home);
        item_storage_usage = findViewById(R.id.item_storage_usage);
        item_clear_cache = findViewById(R.id.item_clear_cache);
        calculateTotalSize();

        // Remove the default title bar.
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ThemeSpinner();
        SetUpButtons();
        UpateApp();
    }

    private void SetUpButtons(){
        btn_home.setOnClickListener(v -> startActivity(new Intent(SettingsPage.this, HomePage.class)));
        item_storage_usage.setOnClickListener(v -> startActivity(new Intent(SettingsPage.this, StorageUsagePage.class)));
        item_clear_cache.setOnClickListener(v ->
                setupDeleteAllButton()
        );
    }

    private void setupDeleteAllButton() {
        item_clear_cache.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(SettingsPage.this)
                    .setTitle("Clear All Data")
                    .setMessage("Are you sure you want to delete all manga, chapters, bookmarks, and history? This action cannot be undone.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Delete in background
                        Executors.newSingleThreadExecutor().execute(() -> {
                            db = AppDatabase.getInstance(SettingsPage.this);
                            db.mangaItemDao().deleteAllManga();
                            db.chapterDao().deleteChapters();
                            db.bookmarkDao().deleteAllBookmarks();
                            db.historyDao().deleteAllHistory();

                            new Handler(Looper.getMainLooper()).post(() -> {
                                text_total_size.setText("0.00 KB");
                            });
                        });
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }


    private void calculateTotalSize() {
        Executors.newSingleThreadExecutor().execute(() -> {
            db = AppDatabase.getInstance(this);
            double totalBytes = 0;
            List<MangaItemEntity> data = db.mangaItemDao().getAllManga();
            for (MangaItemEntity manga : data) {
                // Manga info
                totalBytes += manga.getCoverUrl() != null ? manga.getCoverUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getTitle() != null ? manga.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getMangaId() != null ? manga.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getDescription() != null ? manga.getDescription().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getLastChapter() != null ? manga.getLastChapter().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += manga.getSource() != null ? manga.getSource().getBytes(StandardCharsets.UTF_8).length : 0;

                // Chapters
                List<ChapterItemEntity> chapters = db.chapterDao().getChaptersByMangaIdDesc(manga.getMangaId());
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

    private void ThemeSpinner() {
        Spinner spinnerTheme = findViewById(R.id.spinner_theme);
        String[] themes = {"System Default", "Light", "Dark"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, themes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(adapter);

        // Load saved theme from DB
        Executors.newSingleThreadExecutor().execute(() -> {
            SettingsItemEntity saved = db.settingsDao().getSetting("theme");
            String currentTheme = (saved != null) ? saved.getValue() : "System Default";

            int pos = 0;
            for (int i = 0; i < themes.length; i++) {
                if (themes[i].equals(currentTheme)) {
                    pos = i;
                    break;
                }
            }

            int finalPos = pos;
            new Handler(Looper.getMainLooper()).post(() -> {
                spinnerTheme.setSelection(finalPos);

                // Apply theme immediately on main thread
                switch (themes[finalPos]) {
                    case "Light":
                        runOnUiThread(() -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        });
                        break;
                    case "Dark":
                        runOnUiThread(() -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        });
                        break;
                    default:
                        runOnUiThread(() -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        });
                }
            });
        });

        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTheme = themes[position];

                // Apply theme immediately on main thread
                switch (selectedTheme) {
                    case "Light":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    case "Dark":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    default:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }

                // Save to DB in background
                Executors.newSingleThreadExecutor().execute(() ->
                        db.settingsDao().insertSetting(new SettingsItemEntity("theme", selectedTheme))
                );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void UpateApp() {
        Button checkUpdateButton = findViewById(R.id.button_check_update);
        checkUpdateButton.setOnClickListener(v -> {
            // Check install permission first
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1234);
                    return;
                }
            }

            // Permission granted or not required, check for update
            GitHubUpdateManager updater = new GitHubUpdateManager(this, "AKIKU18", "MangaAkiku");
            updater.checkForUpdate();
        });

        TextView textAppVersion = findViewById(R.id.text_app_version);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName; // e.g., "1.0.4"
            textAppVersion.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            textAppVersion.setText("Version unknown");
        }

    }


    // Optional: handle result if user grants permission
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            // User returned from install permission settings
            GitHubUpdateManager updater = new GitHubUpdateManager(this, "AKIKU18", "MangaAkiku");
            updater.checkForUpdate();
        }
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
