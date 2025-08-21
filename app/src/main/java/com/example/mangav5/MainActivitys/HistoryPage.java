package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.HistoryAdapter;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.R;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryPage extends AppCompatActivity {

    private HistoryAdapter historyAdapter;
    private RecyclerView historyRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_page);

        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // adapter starts empty
        historyAdapter = new HistoryAdapter(this);
        historyRecyclerView.setAdapter(historyAdapter);

        Button btnHome = findViewById(R.id.btnHome);

        AppDatabase db = AppDatabase.getInstance(this);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<HistoryEntity> historyList = db.historyDao().getAllHistory();

            runOnUiThread(() -> {
                historyAdapter.setHistoryList(historyList); // update adapter here
            });
        });

        btnHome.setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppDatabase db = AppDatabase.getInstance(this);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<HistoryEntity> historyList = db.historyDao().getAllHistory();

            runOnUiThread(() -> {
                historyAdapter.setHistoryList(historyList); // update adapter here
            });
        });
    }
}
