package com.example.mangav5.MainActivitys;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.HistoryAdapter;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryPage extends AppCompatActivity {

    private HistoryAdapter historyAdapter;
    private RecyclerView historyRecyclerView;
    private SearchView search_history;
    private TextView history_loading_text;
    private List<HistoryEntity> historyList;
    AppDatabase db = AppDatabase.getInstance(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_page);

        // Remove title bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        search_history = findViewById(R.id.search_history);
        history_loading_text = findViewById(R.id.history_loading_text);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);

        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // adapter starts empty
        historyAdapter = new HistoryAdapter(this);
        historyRecyclerView.setAdapter(historyAdapter);

        Button btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            finish();
        });
        LoadHistoryList();
        SearchThroughBookmarks();
    }


    private void LoadHistoryList(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            historyList = db.historyDao().getAllHistory();

            runOnUiThread(() -> {
                historyAdapter.setHistoryList(historyList); // update adapter here
            });
        });
    }

    private void SearchThroughBookmarks() {
        search_history.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search_history.clearFocus();
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    LoadHistoryList();
                    search_history.setQueryHint("Search history...");
                    history_loading_text.setVisibility(View.GONE);
                } else {
                    search_history.setQueryHint("");
                    performSearch(newText);
                }
                return true;
            }
        });
    }

    private void performSearch(String query) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<HistoryEntity> allHistoryList = db.historyDao().getAllHistory();

            // Filter history by title matching the query
            List<HistoryEntity> filteredList = new ArrayList<>();
            for (HistoryEntity history : allHistoryList) {
                if (history.getMangaTitle().toLowerCase().contains(query.toLowerCase())) {
                    Log.e("History Search: ", history.getMangaTitle());
                    filteredList.add(history);
                }
            }

            runOnUiThread(() -> {
                if (filteredList.isEmpty()) {
                    history_loading_text.setVisibility(View.VISIBLE);  // Show empty message
                    historyList.clear();
                    historyList.addAll(filteredList);
                    historyAdapter.setHistoryList(filteredList);
                } else {
                    history_loading_text.setVisibility(View.GONE);
                    historyList.clear();
                    historyList.addAll(filteredList);
                    historyAdapter.setHistoryList(filteredList);

                }

                // Update your RecyclerView adapter
            });
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
