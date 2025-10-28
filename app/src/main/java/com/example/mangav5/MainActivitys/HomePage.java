package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.ServicesAsuraScans.AsuraScansChapterPages;
import com.example.mangav5.ServicesAsuraScans.AsuraScraperTask;
import com.example.mangav5.ServicesMangaDex.FeedMangaService;
import com.example.mangav5.ServicesMangaDex.SearchService;

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
    private Button button_mangadex;
    private Button button_asurascans;
    private ImageButton settingsPageButton;
    private View bookmarkBlurBackground;
    private View historyBlurBackground;
    private ImageView recycler_bg_blur;
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
    public static String serviceFeed = "MangaDex";
    private int asuraScansOffset = 0;

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
        bookmarkBlurBackground = findViewById(R.id.bookmarkBlurBackground);
        historyBlurBackground = findViewById(R.id.historyBlurBackground);
        recycler_bg_blur = findViewById(R.id.recycler_bg_blur);
        button_mangadex = findViewById(R.id.button_mangadex);
        button_asurascans = findViewById(R.id.button_asurascans);
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
        mangaDexLoadList();
        loadMangaOffset();
        BookmarkButtonGoTo();
        HistoryButtonGoTo();
        SettingsButtonGoTo();
        SwitchFeed();
    }

    private void SwitchFeed() {
        button_asurascans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceFeed = "AsuraScans";
                Toast.makeText(HomePage.this, "AsuraScans", Toast.LENGTH_SHORT).show();

                mangaList.clear();
                offset = 0;
                asuraScansOffset = 0;
                hasMore = true;
                homeListAdapter.notifyDataSetChanged();
                mangaListView.scrollToPosition(0);

                asuraScanLoadList();
            }
        });

        button_mangadex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceFeed = "MangaDex";
                Toast.makeText(HomePage.this, "MangaDex", Toast.LENGTH_SHORT).show();

                // Clear current list and reset offset
                mangaList.clear();
                offset = 0;
                asuraScansOffset = 0;
                homeListAdapter.notifyDataSetChanged();

                // Load MangaDex feed
                mangaDexLoadList();
            }
        });
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
                        searchResultAdapter.refreshBookmarkStates();
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

    private void BlurEffect(View button){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            button.setRenderEffect(
                    RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)
            );
        }
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
                Intent intent = new Intent(HomePage.this, AsuraScansChapterPages.class);
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
                        recycler_bg_blur.setVisibility(View.VISIBLE);
                    }
                    if (!isSearchListAnimated) {
                        Animation slideDown = AnimationUtils.loadAnimation(HomePage.this, R.anim.slide_down);
                        searchResultView.startAnimation(slideDown);
                        isSearchListAnimated = true;  // prevent running again until hidden
                    }
                    performSearch(newText);
                } else {
                    searchResultView.setVisibility(View.GONE);
                    recycler_bg_blur.setVisibility(View.GONE);
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
                        searchResultAdapter.refreshBookmarkStates();
                        searchResultAdapter.notifyDataSetChanged();
                    });
                }

                @Override
                public void onError(String message) {

                }
            });
        }
    }

    private void loadMangaOffset() {
        mangaListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null || isLoading || !hasMore) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                    if (serviceFeed.equals("MangaDex")) {
                        mangaDexLoadList();
                    } else if (serviceFeed.equals("AsuraScans")) {
                        asuraScanLoadList();
                    }
                }
            }
        });
    }


    private void mangaDexLoadList() {
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

    private void asuraScanLoadList() {
        if (isLoading) return;  // safety check
        isLoading = true;

        int currentPage = asuraScansOffset;
        asuraScansOffset += 1;  // increment immediately

        AsuraScraperTask.getAsuraScansMangaFeed(currentPage, new AsuraScraperTask.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) {
                runOnUiThread(() -> {
                    if (currentPage == 0) {
                        mangaList.clear();
                        mangaList.addAll(mangas);
                        homeListAdapter.notifyDataSetChanged();
                    } else {
                        int start = mangaList.size();
                        mangaList.addAll(mangas);
                        homeListAdapter.notifyItemRangeInserted(start, mangas.size());
                    }
                    isLoading = false;
                    homeListAdapter.refreshBookmarkStates();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(HomePage.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    isLoading = false;
                });
            }
        });
    }

}