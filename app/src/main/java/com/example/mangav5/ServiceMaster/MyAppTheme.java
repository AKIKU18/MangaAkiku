package com.example.mangav5.ServiceMaster;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.SettingsItemEntity;

import java.util.concurrent.Executors;

public class MyAppTheme extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Load theme from DB
        AppDatabase db = AppDatabase.getInstance(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            SettingsItemEntity saved = db.settingsDao().getSetting("theme");
            String theme = (saved != null) ? saved.getValue() : "System Default";

            // Apply theme on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                switch (theme) {
                    case "Light":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    case "Dark":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    default:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
            });
        });
    }
}
