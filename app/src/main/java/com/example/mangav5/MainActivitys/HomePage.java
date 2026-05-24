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
    private Button button_mangadex;
    private Button button_asurascans;
    private Button button_manhuaus;
    private Button button_manhuaPlus;
    private Button button_demonicScans;
    private Button button_manhuaFast;
    private Button button_flameComics;
    private Button button_rizzfables;
    private Button button_mgeko;
    private Button button_comix;

    private Button button_vortexScans;

    private ImageButton set_main_mangadex;
    private ImageButton set_main_asurascans;
    private ImageButton set_main_manhuaus;
    private ImageButton set_main_manhuaPlus;
    private ImageButton set_main_demonicScans;
    private ImageButton set_main_manhuaFast;
    private ImageButton set_main_flameComics;
    private ImageButton set_main_rizzfables;
    private ImageButton set_main_mgeko;
    private ImageButton set_main_comix;
    private ImageButton set_main_vortexScans;

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
        setMainUpdateSourceIcon(serviceFeed);

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
        button_mangadex = findViewById(R.id.source_mangadex);
        button_asurascans = findViewById(R.id.source_asurascans);
        button_manhuaus = findViewById(R.id.source_manhuaus);
        button_manhuaPlus = findViewById(R.id.source_manhuaPlus);
        button_demonicScans = findViewById(R.id.source_demonicScans);
        button_manhuaFast = findViewById(R.id.source_manhuaFast);
        button_flameComics = findViewById(R.id.source_flameComics);
        loadingText = findViewById(R.id.loading_text);
        button_rizzfables = findViewById(R.id.source_rizzfables);
        button_mgeko = findViewById(R.id.source_mgeko);
        button_comix = findViewById(R.id.source_comix);
        button_vortexScans = findViewById(R.id.source_vortexScans);

        set_main_mangadex = findViewById(R.id.set_main_mangadex);
        set_main_asurascans = findViewById(R.id.set_main_asurascans);
        set_main_manhuaus = findViewById(R.id.set_main_manhuaus);
        set_main_manhuaPlus = findViewById(R.id.set_main_manhuaPlus);
        set_main_demonicScans = findViewById(R.id.set_main_demonicScans);
        set_main_manhuaFast = findViewById(R.id.set_main_manhuaFast);
        set_main_flameComics = findViewById(R.id.set_main_flameComics);
        set_main_rizzfables = findViewById(R.id.set_main_rizzfables);
        set_main_mgeko = findViewById(R.id.set_main_mgeko);
        set_main_comix = findViewById(R.id.set_main_comix);
        set_main_vortexScans = findViewById(R.id.set_main_vortexScans);
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
        ImageButton menuButton = findViewById(R.id.button_menu);

        menuButton.setOnClickListener(v -> {
            checkSourcesAndDisableButtons();
            drawerLayout.openDrawer(GravityCompat.START);
        });

        Map<Button, String> sourceButtons = new HashMap<>();

        sourceButtons.put(button_mangadex, "MangaDex");
        sourceButtons.put(button_asurascans, "AsuraScans");
        sourceButtons.put(button_manhuaus, "Manhuaus");
        sourceButtons.put(button_manhuaPlus, "ManhuaPlus");
        sourceButtons.put(button_demonicScans, "DemonicScans");
        sourceButtons.put(button_manhuaFast, "ManhuaFast");
        sourceButtons.put(button_flameComics, "FlameComics");
        sourceButtons.put(button_rizzfables, "Rizzfables");
        sourceButtons.put(button_mgeko, "Mgeko");
        sourceButtons.put(button_comix, "Comix");
        sourceButtons.put(button_vortexScans, "VortexScans");

        for (Map.Entry<Button, String> entry : sourceButtons.entrySet()) {
            entry.getKey().setOnClickListener(v -> {
                switchSource(entry.getValue());
                serviceFeed = entry.getValue();
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        Map<ImageButton, String> mainSourceButtons = new HashMap<>();

        mainSourceButtons.put(set_main_mangadex, "MangaDex");
        mainSourceButtons.put(set_main_asurascans, "AsuraScans");
        mainSourceButtons.put(set_main_manhuaus, "Manhuaus");
        mainSourceButtons.put(set_main_manhuaPlus, "ManhuaPlus");
        mainSourceButtons.put(set_main_demonicScans, "DemonicScans");
        mainSourceButtons.put(set_main_manhuaFast, "ManhuaFast");
        mainSourceButtons.put(set_main_flameComics, "FlameComics");
        mainSourceButtons.put(set_main_rizzfables, "Rizzfables");
        mainSourceButtons.put(set_main_mgeko, "Mgeko");
        mainSourceButtons.put(set_main_comix, "Comix");
        mainSourceButtons.put(set_main_vortexScans, "VortexScans");

        for (Map.Entry<ImageButton, String> entry : mainSourceButtons.entrySet()) {
            entry.getKey().setOnClickListener(v -> {
                String sourceToSetAsMain = entry.getValue();
                setMainUpdateSourceIcon(entry.getValue());

                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(HomePage.this);
                    SourceEntity source = new SourceEntity(entry.getValue());
                    db.sourceDao().addSource(source);
                });

                Toast.makeText(this, sourceToSetAsMain + " set as main source.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void switchSource(String newSource) {
        serviceFeed = newSource;
        Toast.makeText(HomePage.this, "Source: " + newSource, Toast.LENGTH_SHORT).show();

        mangaList.clear();
        homeListAdapter.notifyDataSetChanged();
        mangaListView.scrollToPosition(0);

        resetOffsetsForSource(newSource);
        loadCurrentSourceFirstPage();
        updateSourceGlow();
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
        if (serviceFeed.equals("MangaDex")) {
            loadFeed(offset, LIMIT);
        } else if (serviceFeed.equals("AsuraScans")) {
            loadFeed(asuraScansOffset, LIMIT);
        } else if (serviceFeed.equals("ManhuaPlus")) {
            loadFeed(manhuaPlusOffset, LIMIT);
        } else if (serviceFeed.equals("DemonicScans")) {
            loadFeed(demonicScansOffset, LIMIT);
        } else if (serviceFeed.equals("ManhuaFast")) {
            loadFeed(manhuaFastOffset, LIMIT);
        } else if (serviceFeed.equals("Mgeko")) {
            loadFeed(mgekoOffset, LIMIT);
        } else if (serviceFeed.equals("Comix")) {
            loadFeed(comixOffset, LIMIT);
        } else if (serviceFeed.equals("VortexScans")) {
            loadFeed(vortexScansOffset, LIMIT);
        } else {
            loadFeed(offset, LIMIT);
        }
    }

    private void updateSourceGlow() {
        Map<String, Integer> glowMap = new HashMap<>();

        glowMap.put("AsuraScans", R.id.drawer_asurascans_glow);
        glowMap.put("MangaDex", R.id.drawer_mangadex_glow);
        glowMap.put("Manhuaus", R.id.drawer_manhuaus_glow);
        glowMap.put("ManhuaPlus", R.id.drawer_manhuaPlus_glow);
        glowMap.put("DemonicScans", R.id.drawer_demonicScans_glow);
        glowMap.put("ManhuaFast", R.id.drawer_manhuaFast_glow);
        glowMap.put("FlameComics", R.id.drawer_flameComics_glow);
        glowMap.put("Rizzfables", R.id.drawer_rizzfables_glow);
        glowMap.put("Mgeko", R.id.drawer_mgeko_glow);
        glowMap.put("Comix", R.id.drawer_comix_glow);
        glowMap.put("VortexScans", R.id.drawer_vortexScans_glow);

        for (int id : glowMap.values()) {
            View glow = findViewById(id);
            glow.clearAnimation();
            glow.setVisibility(View.GONE);
        }

        Integer glowId = glowMap.get(serviceFeed);
        if (glowId == null) return;

        View targetGlow = findViewById(glowId);
        if (targetGlow != null) {
            targetGlow.setVisibility(View.VISIBLE);

            AlphaAnimation pulse = new AlphaAnimation(0.3f, 1f);
            pulse.setDuration(1000);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            targetGlow.startAnimation(pulse);
        }
    }

    private void setMainUpdateSourceIcon(String setMainButton) {
        Map<String, ImageButton> setMainChangeIcon = new HashMap<>();

        setMainChangeIcon.put("MangaDex", set_main_mangadex);
        setMainChangeIcon.put("AsuraScans", set_main_asurascans);
        setMainChangeIcon.put("Manhuaus", set_main_manhuaus);
        setMainChangeIcon.put("ManhuaPlus", set_main_manhuaPlus);
        setMainChangeIcon.put("DemonicScans", set_main_demonicScans);
        setMainChangeIcon.put("ManhuaFast", set_main_manhuaFast);
        setMainChangeIcon.put("FlameComics", set_main_flameComics);
        setMainChangeIcon.put("Rizzfables", set_main_rizzfables);
        setMainChangeIcon.put("Mgeko", set_main_mgeko);
        setMainChangeIcon.put("Comix", set_main_comix);
        setMainChangeIcon.put("VortexScans", set_main_vortexScans);

        for (ImageButton button : setMainChangeIcon.values()) {
            button.setImageResource(R.drawable.ic_sharingan_dark);
        }

        ImageButton activeButton = setMainChangeIcon.get(setMainButton);
        if (activeButton != null) {
            activeButton.setImageResource(R.drawable.ic_sharingan_light);
        }
    }

    private void showInitialSourceGlow() {
        updateSourceGlow();
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
                    if (serviceFeed.equals("MangaDex")) {
                        loadFeed(offset, LIMIT);
                    } else if (serviceFeed.equals("AsuraScans")) {
                        loadFeed(asuraScansOffset, LIMIT);
                    } else if (serviceFeed.equals("ManhuaPlus")) {
                        loadFeed(manhuaPlusOffset, LIMIT);
                    } else if (serviceFeed.equals("DemonicScans")) {
                        loadFeed(demonicScansOffset, LIMIT);
                    } else if (serviceFeed.equals("ManhuaFast")) {
                        loadFeed(manhuaFastOffset, LIMIT);
                    } else if (serviceFeed.equals("Mgeko")) {
                        loadFeed(mgekoOffset, LIMIT);
                    } else if (serviceFeed.equals("Comix")) {
                        loadFeed(comixOffset, LIMIT);
                    }else if (serviceFeed.equals("VortexScans")) {
                        loadFeed(vortexScansOffset, LIMIT);
                    }
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
                    boolean isFirstLoadForCurrentSource =
                            (serviceFeed.equals("MangaDex") && pageOrOffset == 0) ||
                                    (serviceFeed.equals("AsuraScans") && pageOrOffset == 1 && mangaList.isEmpty()) ||
                                    (serviceFeed.equals("ManhuaPlus") && pageOrOffset == 1 && mangaList.isEmpty()) ||
                                    (serviceFeed.equals("DemonicScans") && pageOrOffset == 1 && mangaList.isEmpty()) ||
                                    (serviceFeed.equals("ManhuaFast") && pageOrOffset == 1 && mangaList.isEmpty()) ||
                                    (serviceFeed.equals("Mgeko") && pageOrOffset == 1 && mangaList.isEmpty()) ||
                                    (serviceFeed.equals("Comix") && pageOrOffset == 1 && mangaList.isEmpty())||
                                    (serviceFeed.equals("VortexScans") && pageOrOffset == 1 && mangaList.isEmpty());


                    if (isFirstLoadForCurrentSource) {
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
                    }else if (serviceFeed.equals("VortexScans")) {
                        HomePage.this.vortexScansOffset = pageOrOffset + 1;
                    }

                    Log.d("HomePage", "loadFeed success -> source=" + serviceFeed +
                            " requested=" + pageOrOffset +
                            " returned=" + mangas.size() +
                            " nextOffset(MD)=" + offset +
                            " nextAsura=" + asuraScansOffset +
                            " nextManhuaPlus=" + manhuaPlusOffset +
                            " nextDemonic=" + demonicScansOffset +
                            " nextManhuaFast=" + manhuaFastOffset +
                            " nextMgeko=" + mgekoOffset +
                            " nextComix=" + comixOffset +
                            " nextVortex=" + vortexScansOffset);
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
        Map<Button, String> sourceButtons = new HashMap<>();

        sourceButtons.put(button_mangadex, "https://mangadex.org/");
        sourceButtons.put(button_asurascans, "https://asuracomic.net");
        sourceButtons.put(button_manhuaus, "https://manhuaus.com");
        sourceButtons.put(button_manhuaPlus, "https://manhuaplus.org/");
        sourceButtons.put(button_demonicScans, "https://demonicscans.org/");
        sourceButtons.put(button_manhuaFast, "https://manhuafast.com/");
        sourceButtons.put(button_flameComics, "https://flamecomics.xyz/");
        sourceButtons.put(button_rizzfables, "https://rizzfables.com/");
        sourceButtons.put(button_mgeko, "https://mgeko.cc/");
        sourceButtons.put(button_comix, "https://comix.to/");
        sourceButtons.put(button_vortexScans, "https://vortexscans.org/");

        Executors.newSingleThreadExecutor().execute(() -> {
            for (Map.Entry<Button, String> entry : sourceButtons.entrySet()) {
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