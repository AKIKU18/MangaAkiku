package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.ServiceManhuas.ManhuausFeedService;
import com.example.mangav5.ServiceManhuas.ManhuausSearchService;
import com.example.mangav5.ServiceMaster.ServiceController;

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
    private Button button_manhuaus;
    private ImageButton settingsPageButton;
    private ImageView recycler_bg_blur;
    private List<MangaItemModel> mangaList = new ArrayList<>();
    private List<MangaItemModel> searchMangaList = new ArrayList<>();
    private boolean isSearchListAnimated = false;
    private boolean isLoading = false;
    private int offset = 1;
    private static final int LIMIT = 10;
    private ActivityResultLauncher<Intent> bookmarkLauncher;
    private ActivityResultLauncher<Intent> mangaPageLauncher;
    private BookmarkDao bookmarkDao;
    public static String serviceFeed = "AsuraScans";
    private int asuraScansOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize views
        searchResultView = findViewById(R.id.recycler_search_results);
        mangaListView = findViewById(R.id.recycler_main);
        searchView = findViewById(R.id.search_bar);
        bookmarkPageButton = findViewById(R.id.button_bookmarks);
        historyPageButton = findViewById(R.id.button_history);
        settingsPageButton = findViewById(R.id.button_settings);
        recycler_bg_blur = findViewById(R.id.recycler_bg_blur);
        button_mangadex = findViewById(R.id.source_mangadex);
        button_asurascans = findViewById(R.id.source_asurascans);
        button_manhuaus = findViewById(R.id.source_manhuaus);
        AppDatabase db = AppDatabase.getInstance(this);
        bookmarkDao = db.bookmarkDao();

        CheckIfStillBookmarked();

        searchResultAdapter = new HomePageAdapter(searchMangaList, this, mangaPageLauncher);
        homeListAdapter = new HomePageAdapter(mangaList, this, mangaPageLauncher);

        searchResultView.setAdapter(searchResultAdapter);
        mangaListView.setAdapter(homeListAdapter);

        searchResultView.setLayoutManager(new LinearLayoutManager(this));
        mangaListView.setLayoutManager(new LinearLayoutManager(this));

        // Remove title bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupSearchView();
        loadFeed(offset, LIMIT); // Load initial feed
        loadMangaOffset();
        BookmarkButtonGoTo();
        HistoryButtonGoTo();
        SettingsButtonGoTo();
        SelectSourceDrawer();
        ShowGlowButtonsSource();//Glow directly for Asurascans as it is the primary source

    }

    private void SelectSourceDrawer(){
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ImageButton menuButton = findViewById(R.id.button_menu);

        // Open drawer when hamburger is clicked
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Use the same buttons that SwitchFeed() uses
        button_mangadex.setOnClickListener(v -> {
            serviceFeed = "MangaDex";
            Toast.makeText(HomePage.this, "MangaDex", Toast.LENGTH_SHORT).show();

            mangaList.clear();
            offset = 0;
            asuraScansOffset = 0;
            homeListAdapter.notifyDataSetChanged();
            mangaListView.scrollToPosition(0);

            loadFeed(offset, LIMIT);

            // Drawer glow animation
            View glowAsura = findViewById(R.id.drawer_asurascans_glow);
            View glowMangaDex = findViewById(R.id.drawer_mangadex_glow);
            View glowManhuaus = findViewById(R.id.drawer_manhuaus_glow);

            glowAsura.clearAnimation();
            glowManhuaus.clearAnimation();
            glowAsura.setVisibility(View.GONE);
            glowManhuaus.setVisibility(View.GONE);

            glowMangaDex.setVisibility(View.VISIBLE);
            AlphaAnimation pulse = new AlphaAnimation(0.3f, 1f);
            pulse.setDuration(1000);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            glowMangaDex.startAnimation(pulse);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        button_asurascans.setOnClickListener(v -> {
            serviceFeed = "AsuraScans";
            Toast.makeText(HomePage.this, "AsuraScans", Toast.LENGTH_SHORT).show();

            mangaList.clear();
            offset = 0;
            asuraScansOffset = 0;
            homeListAdapter.notifyDataSetChanged();
            mangaListView.scrollToPosition(0);

            loadFeed(asuraScansOffset, LIMIT);

            View glowAsura = findViewById(R.id.drawer_asurascans_glow);
            View glowMangaDex = findViewById(R.id.drawer_mangadex_glow);
            View glowManhuaus = findViewById(R.id.drawer_manhuaus_glow);

            glowMangaDex.clearAnimation();
            glowManhuaus.clearAnimation();
            glowMangaDex.setVisibility(View.GONE);
            glowManhuaus.setVisibility(View.GONE);

            glowAsura.setVisibility(View.VISIBLE);
            AlphaAnimation pulse = new AlphaAnimation(0.3f, 1f);
            pulse.setDuration(1000);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            glowAsura.startAnimation(pulse);

            drawerLayout.closeDrawer(GravityCompat.START);
        });

        button_manhuaus.setOnClickListener(v -> {
            serviceFeed = "Manhuaus";
            Toast.makeText(HomePage.this, "Manhuaus", Toast.LENGTH_SHORT).show();

            mangaList.clear();
            offset = 0;
            asuraScansOffset = 0;
            homeListAdapter.notifyDataSetChanged();
            mangaListView.scrollToPosition(0);

            loadFeed(asuraScansOffset, LIMIT);

            View glowAsura = findViewById(R.id.drawer_asurascans_glow);
            View glowMangaDex = findViewById(R.id.drawer_mangadex_glow);
            View glowManhuaus = findViewById(R.id.drawer_manhuaus_glow);

            glowAsura.clearAnimation();
            glowMangaDex.clearAnimation();
            glowAsura.setVisibility(View.GONE);
            glowMangaDex.setVisibility(View.GONE);

            glowManhuaus.setVisibility(View.VISIBLE);
            AlphaAnimation pulse = new AlphaAnimation(0.3f, 1f);
            pulse.setDuration(1000);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            glowManhuaus.startAnimation(pulse);

            drawerLayout.closeDrawer(GravityCompat.START);
        });

    }

    private void ShowGlowButtonsSource(){
        View glowAsura = findViewById(R.id.drawer_asurascans_glow);
        View glowMangaDex = findViewById(R.id.drawer_mangadex_glow);
        View glowManhuaus = findViewById(R.id.drawer_manhuaus_glow);

        glowMangaDex.clearAnimation();
        glowManhuaus.clearAnimation();
        glowMangaDex.setVisibility(View.GONE);
        glowManhuaus.setVisibility(View.GONE);

        glowAsura.setVisibility(View.VISIBLE);
        AlphaAnimation pulse = new AlphaAnimation(0.3f, 1f);
        pulse.setDuration(1000);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        glowAsura.startAnimation(pulse);
    }





    private void CheckIfStillBookmarked() {
        bookmarkLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        boolean bookmarkChanged = false;
                        if (data != null) {
                            bookmarkChanged = data.getBooleanExtra("bookmarkChanged", false);
                        }
                        homeListAdapter.refreshBookmarkStates();
                        searchResultAdapter.refreshBookmarkStates();
                        if (bookmarkChanged) Log.e("HomePage", "Bookmark changed detected!");
                    }
                });

        mangaPageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("bookmarkChanged", false)) {
                            homeListAdapter.refreshBookmarkStates();
                        }
                    }
                });
    }

    private void SettingsButtonGoTo() {
        settingsPageButton.setOnClickListener(v -> startActivity(new Intent(HomePage.this, SettingsPage.class)));
    }

    private void BookmarkButtonGoTo() {
        bookmarkPageButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, BookmarksPage.class);
            bookmarkLauncher.launch(intent);
        });
    }

    private void HistoryButtonGoTo() {
        historyPageButton.setOnClickListener(v -> startActivity(new Intent(HomePage.this, HistoryPage.class)));
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
                        isSearchListAnimated = true;
                    }
                    performSearch(newText);
                } else {
                    searchResultView.setVisibility(View.GONE);
                    recycler_bg_blur.setVisibility(View.GONE);
                    isSearchListAnimated = false;
                }
                return true;
            }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchMangaList.clear();
            return;
        }
        ServiceController.fetchSearchMangas(query, serviceFeed, new ServiceController.MangaListCallback() {
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
                Log.e("HomePageSearch", "Error: " + message);
            }
        });
    }

    private void loadMangaOffset() {
        mangaListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null || isLoading) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                    if (serviceFeed.equals("MangaDex")) loadFeed(offset, LIMIT);
                    else if (serviceFeed.equals("AsuraScans")) loadFeed(asuraScansOffset, LIMIT);
                }
            }
        });
    }

    private void loadFeed(int offset, int limit) {
        if (isLoading) return;
        isLoading = true;

        ServiceController.fetchMangaListController(serviceFeed, offset, limit, new ServiceController.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) {
                runOnUiThread(() -> {
                    if (offset == 1) {
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

                    if (serviceFeed.equals("MangaDex")) HomePage.this.offset += limit;
                    else if (serviceFeed.equals("AsuraScans")) HomePage.this.asuraScansOffset += 1;
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
