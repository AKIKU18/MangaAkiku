package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mangav5.Adapters.BookmarksAdapter;
import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.Entity.SourceEntity;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.ScriptHelper.RecreateMangaId;
import com.example.mangav5.ServiceMaster.ServiceController;
import com.example.mangav5.Sources.MangaSource;
import com.example.mangav5.UI.SourceDrawerHandler;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansChaptersService;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansFeedService;
import com.example.mangav5.ServicesMangaWebsites.VortexScans.VortexScansSearchService;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class HomePage extends AppCompatActivity {

    private static final int LIMIT = 10;
    public static String serviceFeed = "DemonicScans";


    private RecyclerView searchResultView;
    private RecyclerView mangaListView;
    private HomePageAdapter searchResultAdapter;
    private HomePageAdapter homeListAdapter;
    private SearchView searchView;
    private Button bookmarkPageButton;
    private Button historyPageButton;

    private ImageButton settingsPageButton;
    private TextView loadingText;
    private ImageView recycler_bg_blur;

    private List<MangaItemModel> mangaList = new ArrayList<>();
    private List<MangaItemModel> searchMangaList = new ArrayList<>();
    private boolean isSearchListAnimated = false;
    private boolean isLoading = false;

    // MangaDex = offset based
    private int offset = 0;

    // Other sources = page based (start from 1)
    private int asuraScansOffset = 1;
    private int manhuaPlusOffset = 1;
    private int demonicScansOffset = 1;
    private int manhuaFastOffset = 1;
    private int mgekoOffset = 1;
    private int comixOffset = 1;
    private int vortexScansOffset = 1;

    private ActivityResultLauncher<Intent> bookmarkLauncher;
    private ActivityResultLauncher<Intent> mangaPageLauncher;
    private BookmarkDao bookmarkDao;
    private SourceDrawerHandler drawerHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GetTheme();
        GetSource();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        initializeViews();

        AppDatabase db = AppDatabase.getInstance(this);
        bookmarkDao = db.bookmarkDao();

        setupResultLaunchers();
        setupRecyclerViews();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupSearchView();
        loadCurrentSourceFirstPage();
        setupPagination();
        setupNavigationButtons();
        setupSourceSelectionDrawer();
        showInitialSourceGlow();
        checkSourcesAndDisableButtons();
        drawerHandler.updateMainSourceIcon(serviceFeed);

        // Run migration for manga IDs to new Hex UUID format
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("manga_id_recreated", false)) {
            RecreateMangaId.execute(this);
            prefs.edit().putBoolean("manga_id_recreated", true).apply();
        }
    }
    private void GetTheme() {
        AppDatabase db = AppDatabase.getInstance(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (db.settingsDao().getSetting("theme") != null) {
                    SetTheme(db.settingsDao().getSetting("theme").getValue());
                }
                if (db.sourceDao().getSource() != null) {
                    serviceFeed = db.sourceDao().getSource().mainSource;
                }
            } catch (Exception e) {
                Log.e("HomePage", "GetTheme error", e);
            }
        });
    }

    private void GetSource() {
        AppDatabase db = AppDatabase.getInstance(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (db.sourceDao().getSource() != null) {
                    serviceFeed = db.sourceDao().getSource().mainSource;
                }
            } catch (Exception e) {
                Log.e("HomePage", "GetSource error", e);
            }
        });
    }

    private void SetTheme(String selectedTheme) {
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
    }

    private void initializeViews() {
        searchResultView = findViewById(R.id.recycler_search_results);
        mangaListView = findViewById(R.id.recycler_main);
        searchView = findViewById(R.id.search_bar);
        bookmarkPageButton = findViewById(R.id.button_bookmarks);
        historyPageButton = findViewById(R.id.button_history);
        settingsPageButton = findViewById(R.id.button_settings_button);
        recycler_bg_blur = findViewById(R.id.recycler_bg_blur);
        loadingText = findViewById(R.id.loading_text);
    }

    private void setupRecyclerViews() {

        searchResultAdapter = new HomePageAdapter(searchMangaList, this, mangaPageLauncher);
        homeListAdapter = new HomePageAdapter(mangaList, this, mangaPageLauncher);

        searchResultView.setLayoutManager(new LinearLayoutManager(this));
        mangaListView.setLayoutManager(new LinearLayoutManager(this));

        searchResultView.setAdapter(searchResultAdapter);
        mangaListView.setAdapter(homeListAdapter);

        searchView.setQueryHint("Search...");
        searchView.setIconifiedByDefault(false);
    }
    private void setupSourceSelectionDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerHandler = new SourceDrawerHandler(this, drawerLayout, this::switchSource);
        
        // One line per source! 
        drawerHandler.addSource("MangaDex", R.id.source_mangadex, R.id.set_main_mangadex, R.id.drawer_mangadex_glow);
        drawerHandler.addSource("AsuraScans", R.id.source_asurascans, R.id.set_main_asurascans, R.id.drawer_asurascans_glow);
        drawerHandler.addSource("Manhuaus", R.id.source_manhuaus, R.id.set_main_manhuaus, R.id.drawer_manhuaus_glow);
        drawerHandler.addSource("ManhuaPlus", R.id.source_manhuaPlus, R.id.set_main_manhuaPlus, R.id.drawer_manhuaPlus_glow);
        drawerHandler.addSource("DemonicScans", R.id.source_demonicScans, R.id.set_main_demonicScans, R.id.drawer_demonicScans_glow);
        drawerHandler.addSource("ManhuaFast", R.id.source_manhuaFast, R.id.set_main_manhuaFast, R.id.drawer_manhuaFast_glow);
        drawerHandler.addSource("FlameComics", R.id.source_flameComics, R.id.set_main_flameComics, R.id.drawer_flameComics_glow);
        drawerHandler.addSource("Rizzfables", R.id.source_rizzfables, R.id.set_main_rizzfables, R.id.drawer_rizzfables_glow);
        drawerHandler.addSource("Mgeko", R.id.source_mgeko, R.id.set_main_mgeko, R.id.drawer_mgeko_glow);
        drawerHandler.addSource("Comix", R.id.source_comix, R.id.set_main_comix, R.id.drawer_comix_glow);
        drawerHandler.addSource("VortexScans", R.id.source_vortexScans, R.id.set_main_vortexScans, R.id.drawer_vortexScans_glow);

        drawerHandler.setup();
    }

    private void switchSource(String newSource) {
        serviceFeed = newSource;
        Toast.makeText(HomePage.this, "Source: " + newSource, Toast.LENGTH_SHORT).show();

        mangaList.clear();
        homeListAdapter.notifyDataSetChanged();
        mangaListView.scrollToPosition(0);

        resetOffsetsForSource(newSource);
        loadCurrentSourceFirstPage();
        drawerHandler.updateSourceGlow(newSource);
    }

    private void resetOffsetsForSource(String source) {
        if (source.equals("MangaDex")) {
            offset = 0;
        } else if (source.equals("AsuraScans")) {
            asuraScansOffset = 1;
        } else if (source.equals("ManhuaPlus")) {
            manhuaPlusOffset = 1;
        } else if (source.equals("DemonicScans")) {
            demonicScansOffset = 1;
        } else if (source.equals("ManhuaFast")) {
            manhuaFastOffset = 1;
        } else if (source.equals("Mgeko")) {
            mgekoOffset = 1;
        } else if (source.equals("Comix")) {
            comixOffset = 1;
        }else if (source.equals("VortexScans")) {
            vortexScansOffset = 1;
        }
    }

    private void loadCurrentSourceFirstPage() {
        MangaSource source = com.example.mangav5.Sources.SourceManager.getInstance().getSource(serviceFeed);
        int startPage = (source != null) ? source.getStartingPage() : 1;
        
        // Custom overrides if needed (though getStartingPage should handle it)
        if (serviceFeed.equals("MangaDex")) startPage = offset;
        else if (serviceFeed.equals("AsuraScans")) startPage = asuraScansOffset;
        else if (serviceFeed.equals("ManhuaPlus")) startPage = manhuaPlusOffset;
        else if (serviceFeed.equals("DemonicScans")) startPage = demonicScansOffset;
        else if (serviceFeed.equals("ManhuaFast")) startPage = manhuaFastOffset;
        else if (serviceFeed.equals("Mgeko")) startPage = mgekoOffset;
        else if (serviceFeed.equals("Comix")) startPage = comixOffset;
        else if (serviceFeed.equals("VortexScans")) startPage = vortexScansOffset;

        loadFeed(startPage, LIMIT);
    }

    private void showInitialSourceGlow() {
        drawerHandler.updateSourceGlow(serviceFeed);
    }

    private void setupResultLaunchers() {
        bookmarkLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        homeListAdapter.refreshBookmarkStates();
                        searchResultAdapter.refreshBookmarkStates();
                        Log.d("HomePage", "Returned from Bookmarks, refreshing states.");
                    }
                });

        mangaPageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("bookmarkChanged", false)) {
                            homeListAdapter.refreshBookmarkStates();
                            searchResultAdapter.refreshBookmarkStates();
                        }
                    }
                });
    }

    private void setupNavigationButtons() {
        settingsPageButton.setOnClickListener(v -> startActivity(new Intent(HomePage.this, SettingsPage.class)));

        bookmarkPageButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, BookmarksPage.class);
            bookmarkLauncher.launch(intent);
        });

        historyPageButton.setOnClickListener(v -> startActivity(new Intent(HomePage.this, HistoryPage.class)));
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (searchResultView.getVisibility() != View.VISIBLE) {
                    searchResultView.setVisibility(View.VISIBLE);
                    recycler_bg_blur.setVisibility(View.VISIBLE);
                    Animation slideDown = AnimationUtils.loadAnimation(HomePage.this, R.anim.slide_down);
                    searchResultView.startAnimation(slideDown);
                    searchView.clearFocus();
                }
                searchMangaList.clear();
                searchResultAdapter.notifyDataSetChanged();
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    searchMangaList.clear();
                    searchView.setQueryHint("Search...");
                    searchResultAdapter.notifyDataSetChanged();
                    runOnUiThread(() -> {
                        loadingText.setVisibility(View.GONE);
                        loadingText.setText("Loading...");
                    });
                    if (searchResultView != null)
                        searchResultView.setVisibility(View.GONE);

                    if (recycler_bg_blur != null)
                        recycler_bg_blur.setVisibility(View.GONE);
                } else {
                    searchView.setQueryHint("");
                }
                return true;
            }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchMangaList.clear();
            searchResultAdapter.notifyDataSetChanged();
            runOnUiThread(() -> loadingText.setVisibility(View.GONE));
            return;
        }

        runOnUiThread(() -> loadingText.setVisibility(View.VISIBLE));

        ServiceController.searchThroughAllSources(query, new ServiceController.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> results) {
                runOnUiThread(() -> {
                    searchMangaList.clear();
                    searchMangaList.addAll(results);
                    searchResultAdapter.refreshBookmarkStates();
                    refreshBookmarks();
                    searchResultAdapter.notifyDataSetChanged();
                    loadingText.setVisibility(View.GONE);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(HomePage.this, "Search failed: " + message, Toast.LENGTH_SHORT).show();
                    loadingText.setVisibility(View.VISIBLE);
                    loadingText.setText("Nothing found...");
                });
            }
        });
    }

    private void setupPagination() {
        mangaListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null || isLoading) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && totalItemCount > 0) {
                    int currentOffset = offset; // fallback
                    if (serviceFeed.equals("AsuraScans")) currentOffset = asuraScansOffset;
                    else if (serviceFeed.equals("ManhuaPlus")) currentOffset = manhuaPlusOffset;
                    else if (serviceFeed.equals("DemonicScans")) currentOffset = demonicScansOffset;
                    else if (serviceFeed.equals("ManhuaFast")) currentOffset = manhuaFastOffset;
                    else if (serviceFeed.equals("Mgeko")) currentOffset = mgekoOffset;
                    else if (serviceFeed.equals("Comix")) currentOffset = comixOffset;
                    else if (serviceFeed.equals("VortexScans")) currentOffset = vortexScansOffset;
                    
                    loadFeed(currentOffset, LIMIT);
                }
            }
        });
    }

    private void loadFeed(int pageOrOffset, int limit) {
        if (isLoading) return;
        isLoading = true;

        Log.d("HomePage", "loadFeed -> source=" + serviceFeed + " pageOrOffset=" + pageOrOffset);

        ServiceController.fetchMangaListController(serviceFeed, pageOrOffset, limit, new ServiceController.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) {
                runOnUiThread(() -> {
                    MangaSource source = com.example.mangav5.Sources.SourceManager.getInstance().getSource(serviceFeed);
                    boolean isFirstLoad = (source != null && pageOrOffset == source.getStartingPage() && mangaList.isEmpty())
                            || (serviceFeed.equals("MangaDex") && pageOrOffset == 0);

                    if (isFirstLoad) {
                        mangaList.clear();
                        mangaList.addAll(mangas);
                        homeListAdapter.notifyDataSetChanged();
                    } else {
                        int startPosition = mangaList.size();
                        mangaList.addAll(mangas);
                        homeListAdapter.notifyItemRangeInserted(startPosition, mangas.size());
                    }

                    homeListAdapter.refreshBookmarkStates();
                    isLoading = false;

                    // Update offsets
                    if (serviceFeed.equals("MangaDex")) {
                        HomePage.this.offset = pageOrOffset + limit;
                    } else if (serviceFeed.equals("AsuraScans")) {
                        HomePage.this.asuraScansOffset = pageOrOffset + 1;
                    } else if (serviceFeed.equals("ManhuaPlus")) {
                        HomePage.this.manhuaPlusOffset = pageOrOffset + 1;
                    } else if (serviceFeed.equals("DemonicScans")) {
                        HomePage.this.demonicScansOffset = pageOrOffset + 1;
                    } else if (serviceFeed.equals("ManhuaFast")) {
                        HomePage.this.manhuaFastOffset = pageOrOffset + 1;
                    } else if (serviceFeed.equals("Mgeko")) {
                        HomePage.this.mgekoOffset = pageOrOffset + 1;
                    } else if (serviceFeed.equals("Comix")) {
                        HomePage.this.comixOffset = pageOrOffset + 1;
                    } else if (serviceFeed.equals("VortexScans")) {
                        HomePage.this.vortexScansOffset = pageOrOffset + 1;
                    }

                    Log.d("HomePage", "loadFeed success -> source=" + serviceFeed + " returned=" + mangas.size());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(HomePage.this, "Error loading feed: " + message, Toast.LENGTH_SHORT).show();
                    isLoading = false;
                });
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        refreshBookmarks();
    }

    private void refreshBookmarks() {
        if (homeListAdapter != null) homeListAdapter.refreshBookmarkStates();
        if (searchResultAdapter != null) searchResultAdapter.refreshBookmarkStates();
        Log.d("HomePage", "Bookmarks refreshed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBookmarks();
    }

    private boolean isSourceAccessible(String url) {
        try {
            Jsoup.connect(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();
            return true;
        } catch (HttpStatusException e) {
            return e.getStatusCode() != 403;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkSourcesAndDisableButtons() {
        Map<Button, String> sourceUrls = new HashMap<>();

        sourceUrls.put(findViewById(R.id.source_mangadex), "https://mangadex.org/");
        sourceUrls.put(findViewById(R.id.source_asurascans), "https://asuracomic.net");
        sourceUrls.put(findViewById(R.id.source_manhuaus), "https://manhuaus.com");
        sourceUrls.put(findViewById(R.id.source_manhuaPlus), "https://manhuaplus.org/home");
        sourceUrls.put(findViewById(R.id.source_demonicScans), "https://demonicscans.org/");
        sourceUrls.put(findViewById(R.id.source_manhuaFast), "https://manhuafast.com/");
        sourceUrls.put(findViewById(R.id.source_flameComics), "https://flamecomics.xyz/");
        sourceUrls.put(findViewById(R.id.source_rizzfables), "https://rizzfables.com/");
        sourceUrls.put(findViewById(R.id.source_mgeko), "https://mgeko.cc/");
        sourceUrls.put(findViewById(R.id.source_comix), "https://comix.to/");
        sourceUrls.put(findViewById(R.id.source_vortexScans), "https://vortexscans.org/");

        Executors.newSingleThreadExecutor().execute(() -> {
            for (Map.Entry<Button, String> entry : sourceUrls.entrySet()) {
                if (entry.getKey() == null) continue;
                boolean accessible = isSourceAccessible(entry.getValue());
                runOnUiThread(() -> {
                    if (!accessible) {
                        entry.getKey().setEnabled(false);
                        entry.getKey().setAlpha(0.4f);
                    } else {
                        entry.getKey().setEnabled(true);
                        entry.getKey().setAlpha(1f);
                    }
                });
            }
        });
    }
}
