package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SearchView;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.BookmarksAdapter;
import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Models.ChapterListModel;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.Services.ChaptersService;
import com.example.mangav5.Services.FeedMangaService;
import com.example.mangav5.Services.SearchService;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private RecyclerView searchResultView;
    private RecyclerView mangaListView;
    private HomePageAdapter searchResultAdapter;
    private HomePageAdapter homeListAdapter;
    private SearchView searchView;
    private Button bookmarkPageButton;
    private Button historyPageButton;
    private ImageButton settingsPageButton;
    private List<MangaItemModel> mangaList = new ArrayList<>();
    private List<MangaItemModel> searchMangaList = new ArrayList<>();
    private boolean isSearchListAnimated = false;
    private boolean isLoading = false;
    private int offset = 0;
    private static final int LIMIT = 10;
    private boolean hasMore = true; // Optional, in case the API tells you there are no more items
    private ActivityResultLauncher<Intent> bookmarkLauncher;
    private ActivityResultLauncher<Intent> mangaPageLauncher;
    private BookmarkDao bookmarkDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);


        // Initialize views and DB
        searchResultView = findViewById(R.id.recycler_search_results);
        mangaListView = findViewById(R.id.recycler_main);
        searchView = findViewById(R.id.search_bar);
        bookmarkPageButton = findViewById(R.id.button_bookmarks);
        historyPageButton = findViewById(R.id.button_history);
        settingsPageButton = findViewById(R.id.button_settings);
        CheckIfStillBookmarked();


        searchResultAdapter = new HomePageAdapter(searchMangaList, this,mangaPageLauncher);

        homeListAdapter = new HomePageAdapter(mangaList, this,mangaPageLauncher);
        searchResultView.setAdapter(searchResultAdapter);
        mangaListView.setAdapter(homeListAdapter);

        searchResultView.setLayoutManager(new LinearLayoutManager(this));
        mangaListView.setLayoutManager(new LinearLayoutManager(this));

        AppDatabase db = AppDatabase.getInstance(this);
        this.bookmarkDao =db.bookmarkDao();

        // Remove title bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupSearchView();
        loadMangaList();
        loadMangaOffset();
        BookmarkButtonGoTo();
        HistoryButtonGoTo();
        SettingsButtonGoTo();

    }

    private void CheckIfStillBookmarked(){
        bookmarkLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        boolean bookmarkChanged = false;
                        if (data != null) {
                            bookmarkChanged = data.getBooleanExtra("bookmarkChanged", false);
                        }
                        // Always refresh bookmarks to reflect current DB
                        homeListAdapter.refreshBookmarkStates();
                        if (bookmarkChanged) {
                            Log.e("HomePage", "Bookmark changed detected!");
                        }
                    }
                }
        );

        mangaPageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            boolean bookmarkChanged = data.getBooleanExtra("bookmarkChanged", false);
                            if (bookmarkChanged) {
                                homeListAdapter.refreshBookmarkStates();
                            }
                        }
                    }
                }
        );

    }

    private void SettingsButtonGoTo(){
        settingsPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, SettingsPage.class);
                startActivity(intent);
            }
        });
    }

    private void BookmarkButtonGoTo(){
        bookmarkPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, BookmarksPage.class);
                bookmarkLauncher.launch(intent);
            }
        });
    }
    private void HistoryButtonGoTo(){
        historyPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, HistoryPage.class);
                startActivity(intent);
            }
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.trim().isEmpty()) {
                    if (searchResultView.getVisibility() != View.VISIBLE) {
                        searchResultView.setVisibility(View.VISIBLE);
                    }
                    if (!isSearchListAnimated) {
                        Animation slideDown = AnimationUtils.loadAnimation(HomePage.this, R.anim.slide_down);
                        searchResultView.startAnimation(slideDown);
                        isSearchListAnimated = true;  // prevent running again until hidden
                    }
                    performSearch(newText);
                } else {
                    searchResultView.setVisibility(View.GONE);
                    isSearchListAnimated = false;  // reset flag when hidden
                }
                return true;
            }
        });
    }




    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchMangaList.clear();
            offset = 0;
        } else {
            SearchService.searchManga(query.trim(), 0, 50, new SearchService.MangaListCallback() {
                @Override
                public void onSuccess(List<MangaItemModel> results) {
                    runOnUiThread(() -> {
                        searchMangaList.clear();
                        searchMangaList.addAll(results);
                        searchResultAdapter.notifyDataSetChanged();
                    });
                }

                @Override
                public void onError(String message) {

                }
            });
        }
    }

    private void loadMangaOffset(){
        mangaListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null || isLoading || !hasMore) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Trigger when scrolled near the end
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                    loadMangaList(); // Load next batch
                }
            }
        });
    }

    private void loadMangaList() {
        isLoading = true;
        FeedMangaService.fetchMangaList(offset, LIMIT, new FeedMangaService.MangaListCallback() {
        @Override
        public void onSuccess(List<MangaItemModel> mangas) {
            runOnUiThread(() -> {
                if (offset == 0) {
                    mangaList.clear();
                    mangaList.addAll(mangas);
                    homeListAdapter.notifyDataSetChanged();
                } else {
                    int start = mangaList.size();
                    mangaList.addAll(mangas);
                    homeListAdapter.notifyItemRangeInserted(start, mangas.size());
                }
                offset += LIMIT;
                isLoading = false;
                homeListAdapter.refreshBookmarkStates();

            });
        }

        @Override
        public void onError(String message) {
            runOnUiThread(() -> {
                Toast.makeText(HomePage.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            });
        }
    });
    }

}