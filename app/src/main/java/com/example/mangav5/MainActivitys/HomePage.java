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
import android.widget.TextView;
import android.widget.Toast;

import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.ServiceManhuaFast.ManhuaFastFeedService;
import com.example.mangav5.ServiceManhuaFast.ManhuaFastSearchService;
import com.example.mangav5.ServiceManhuas.ManhuausFeedService;
import com.example.mangav5.ServiceMaster.ServiceController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The main screen of the application. It displays a feed of manga,
 * provides search functionality, and allows navigation to other parts of the app
 * like bookmarks, history, and settings. It also manages data fetching from different sources.
 */
public class HomePage extends AppCompatActivity {

    private static final int LIMIT = 10;          // Number of items to fetch per page for MangaDex.
    public static String serviceFeed = "AsuraScans"; // The current selected data source. Defaults to AsuraScans.
    // UI Components
    private RecyclerView searchResultView; // RecyclerView for displaying search results.
    private RecyclerView mangaListView;      // RecyclerView for the main manga feed.
    private HomePageAdapter searchResultAdapter; // Adapter for the search results RecyclerView.
    private HomePageAdapter homeListAdapter;     // Adapter for the main feed RecyclerView.
    private SearchView searchView;           // Input field for searching manga.
    private Button bookmarkPageButton;       // Button to navigate to the Bookmarks page.
    private Button historyPageButton;        // Button to navigate to the History page.
    private Button button_mangadex;          // Button in the drawer to select MangaDex as the source.
    private Button button_asurascans;        // Button in the drawer to select AsuraScans as the source.
    private Button button_manhuaus;          // Button in the drawer to select Manhuaus as the source.
    private Button button_manhuaPlus; // Button in the drawer to select ManhuaPlus as the source.
    private Button button_demonicScans; // Button in the drawer to select DemonicScans as the source.
    private Button button_manhuaFast;   // Button in the drawer to select ManhuaFast as the source.
    private ImageButton settingsPageButton;  // Button to navigate to the Settings page.
    private TextView loadingText;             // Text view for displaying loading state.
    private ImageView recycler_bg_blur;      // Background view for blur effect behind search results.
    // Data and State Management
    private List<MangaItemModel> mangaList = new ArrayList<>(); // Data source for the main manga feed.
    private List<MangaItemModel> searchMangaList = new ArrayList<>(); // Data source for search results.
    private boolean isSearchListAnimated = false; // Flag to check if search results animation has run.
    private boolean isLoading = false;            // Flag to prevent multiple simultaneous data loads (pagination).
    private int offset = 1;                       // Current offset for MangaDex pagination.
    private int asuraScansOffset = 0;             // Current page number for AsuraScans pagination.
    private int manhuaPlusOffset = 0;              // Current page number for ManhuaPlus pagination.
    private int demonicScansOffset = 0;            // Current page number for DemonicScans pagination.
    private int manhuaFastOffset = 0;              // Current page number for ManhuaFast pagination.
    // Activity Result Launchers
    private ActivityResultLauncher<Intent> bookmarkLauncher; // Handles results from the Bookmarks page.
    private ActivityResultLauncher<Intent> mangaPageLauncher;  // Handles results from the MangaPage.
    // Database and Services
    private BookmarkDao bookmarkDao;              // DAO for accessing bookmark data.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize all UI views from the layout file.
        initializeViews();

        // Get database instance and DAO.
        AppDatabase db = AppDatabase.getInstance(this);
        bookmarkDao = db.bookmarkDao();

        // Set up the result launchers to handle data returned from other activities.
        setupResultLaunchers();

        // Configure adaMangaPageDebugpters and layout managers for both RecyclerViews.
        setupRecyclerViews();

        // Remove the default title bar.
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set up listeners and initial state.
        setupSearchView();
        loadFeed(offset, LIMIT); // Load the initial data for the main feed.
        setupPagination();
        setupNavigationButtons();
        setupSourceSelectionDrawer();
        showInitialSourceGlow(); // Visually indicate the default source.
    }

    /**
     * Initializes all the view components from the activity's layout.
     */
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
        loadingText = findViewById(R.id.loading_text);

    }

    /**
     * Sets up the RecyclerViews with their respective adapters and layout managers.
     */
    private void setupRecyclerViews() {
        searchResultAdapter = new HomePageAdapter(searchMangaList, this, mangaPageLauncher);
        homeListAdapter = new HomePageAdapter(mangaList, this, mangaPageLauncher);

        searchResultView.setAdapter(searchResultAdapter);
        mangaListView.setAdapter(homeListAdapter);

        searchResultView.setLayoutManager(new LinearLayoutManager(this));
        mangaListView.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Configures the navigation drawer and the listeners for source selection buttons.
     */
    private void setupSourceSelectionDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ImageButton menuButton = findViewById(R.id.button_menu);

        // Open the drawer when the menu button is clicked.
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Map your buttons to their sources
        Map<Button, String> sourceButtons = Map.of(
                button_mangadex, "MangaDex",
                button_asurascans, "AsuraScans",
                button_manhuaus, "Manhuaus",
                button_manhuaPlus, "ManhuaPlus",
                button_demonicScans,"DemonicScans",
                button_manhuaFast,"ManhuaFast"
        );

        // Set up all listeners in one loop
        for (Map.Entry<Button, String> entry : sourceButtons.entrySet()) {
            entry.getKey().setOnClickListener(v -> {
                switchSource(entry.getValue());
                serviceFeed = entry.getValue();
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }
    }

    /**
     * Switches the data source, clears existing data, and reloads the feed from the new source.
     *
     * @param newSource The name of the new source to switch to (e.g., "MangaDex").
     */
    private void switchSource(String newSource) {
        serviceFeed = newSource;
        Toast.makeText(HomePage.this, "Source: " + newSource, Toast.LENGTH_SHORT).show();

        // Clear current list and reset pagination counters.
        mangaList.clear();
        offset = (newSource.equals("MangaDex")) ? 0 : 1; // MangaDex is 0-based, others are 1-based.
        asuraScansOffset = 1;
        manhuaPlusOffset = 1;
        demonicScansOffset = 1;
        manhuaFastOffset = 1;

        homeListAdapter.notifyDataSetChanged();
        mangaListView.scrollToPosition(0);

        // Load data from the new source.
        loadFeed(offset, LIMIT);
        updateSourceGlow(); // Update the visual indicator for the selected source.
    }

    /**
     * Updates the glowing animation to highlight the currently selected data source in the drawer.
     */
    private void updateSourceGlow() {
        // Map sources to their glow view IDs
        Map<String, Integer> glowMap = Map.of(
                "AsuraScans", R.id.drawer_asurascans_glow,
                "MangaDex", R.id.drawer_mangadex_glow,
                "Manhuaus", R.id.drawer_manhuaus_glow,
                "ManhuaPlus", R.id.drawer_manhuaPlus_glow,
                "DemonicScans", R.id.drawer_demonicScans_glow,
                "ManhuaFast", R.id.drawer_manhuaFast_glow
        );

        // Hide all glows first
        for (int id : glowMap.values()) {
            View glow = findViewById(id);
            glow.clearAnimation();
            glow.setVisibility(View.GONE);
        }

        // Find the target glow based on the current serviceFeed
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

    /**
     * Shows the glow animation for the default source on app start.
     */
    private void showInitialSourceGlow() {
        updateSourceGlow();
    }


    /**
     * Initializes ActivityResultLaunchers to handle data returned from other activities,
     * such as bookmark status changes.
     */
    private void setupResultLaunchers() {
        // Launcher for the bookmarks page. Refreshes bookmark states if anything changed.
        bookmarkLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        homeListAdapter.refreshBookmarkStates();
                        searchResultAdapter.refreshBookmarkStates();
                        Log.d("HomePage", "Returned from Bookmarks, refreshing states.");
                    }
                });

        // Launcher for the manga details page. Refreshes if a bookmark was added/removed.
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

    /**
     * Sets up click listeners for the main navigation buttons (Settings, Bookmarks, History).
     */
    private void setupNavigationButtons() {
        settingsPageButton.setOnClickListener(v -> startActivity(new Intent(HomePage.this, SettingsPage.class)));

        bookmarkPageButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, BookmarksPage.class);
            bookmarkLauncher.launch(intent);
        });

        historyPageButton.setOnClickListener(v -> startActivity(new Intent(HomePage.this, HistoryPage.class)));
    }


    /**
     * Configures the SearchView, including its listeners for text changes and submissions.
     * Handles the visibility and animation of the search results view.
     */
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (searchResultView.getVisibility() != View.VISIBLE) {
                    searchResultView.setVisibility(View.VISIBLE);
                    recycler_bg_blur.setVisibility(View.VISIBLE);
                    Animation slideDown = AnimationUtils.loadAnimation(HomePage.this, R.anim.slide_down);
                    searchResultView.startAnimation(slideDown);
                }
                searchMangaList.clear();   // Clear **once** here
                searchResultAdapter.notifyDataSetChanged();
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    searchMangaList.clear();
                    searchResultAdapter.notifyDataSetChanged();
                    runOnUiThread(() ->{
                        loadingText.setVisibility(View.GONE);
                        loadingText.setText("Loading...");
                    });
                    if (searchResultView != null)
                        searchResultView.setVisibility(View.GONE);

                    if (recycler_bg_blur != null)
                        recycler_bg_blur.setVisibility(View.GONE);
                }
                return true;
            }

        });

    }

    /**
     * Executes a search query using the appropriate service controller based on the selected source.
     * Updates the search results adapter with the fetched data.
     *
     * @param query The search term entered by the user.
     */


    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchMangaList.clear();
            searchResultAdapter.notifyDataSetChanged();
            runOnUiThread(() ->{
                loadingText.setVisibility(View.GONE);
            });
            return;
        }
        runOnUiThread(() ->{
            loadingText.setVisibility(View.VISIBLE);
        });
        // Call the central service controller to perform the search.
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


    /**
     * Sets up the scroll listener on the main RecyclerView to handle infinite scrolling/pagination.
     */
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

                // Load more items when the user is near the end of the list.
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && totalItemCount > 0) {
                    if (serviceFeed.equals("MangaDex")) {
                        loadFeed(offset, LIMIT);
                    } else if (serviceFeed.equals("AsuraScans")) {
                        loadFeed(asuraScansOffset, LIMIT);
                    } else if (serviceFeed.equals("ManhuaPlus")) {
                        loadFeed(manhuaPlusOffset, LIMIT);
                    }else if (serviceFeed.equals("DemonicScans")){
                        loadFeed(demonicScansOffset, LIMIT);
                    }else if (serviceFeed.equals("ManhuaFast")){
                        loadFeed(manhuaFastOffset, LIMIT);
                    }
                    // Add other sources here if they support pagination.
                }
            }
        });
    }

    /**
     * Fetches a list of manga from the currently selected service and appends it to the main list.
     * Handles loading state and pagination offsets.
     *
     * @param pageOrOffset The page number or offset for the API request.
     * @param limit        The number of items to load.
     */
    private void loadFeed(int pageOrOffset, int limit) {
        if (isLoading) return; // Prevent concurrent loads.
        isLoading = true;

        ServiceController.fetchMangaListController(serviceFeed, pageOrOffset, limit, new ServiceController.MangaListCallback() {
            @Override
            public void onSuccess(List<MangaItemModel> mangas) {
                runOnUiThread(() -> {
                    // For the first page, clear the list. For subsequent pages, append.
                    if (pageOrOffset == 0 || pageOrOffset == 1) {
                        mangaList.clear();
                        mangaList.addAll(mangas);

                        homeListAdapter.notifyDataSetChanged();
                    } else {
                        int startPosition = mangaList.size();
                        mangaList.addAll(mangas);
                        homeListAdapter.notifyItemRangeInserted(startPosition, mangas.size());
                    }

                    isLoading = false;
                    homeListAdapter.refreshBookmarkStates(); // Update bookmark icons for new items.

                    // Increment the correct offset for the next page load.
                    if (serviceFeed.equals("MangaDex")) {
                        HomePage.this.offset += limit;
                    } else if (serviceFeed.equals("AsuraScans")) {
                        HomePage.this.asuraScansOffset += 1;
                    } else if (serviceFeed.equals("ManhuaPlus")) {
                        HomePage.this.manhuaPlusOffset += 1;
                    }else if (serviceFeed.equals("DemonicScans")){
                        HomePage.this.demonicScansOffset += 1;
                    }else if (serviceFeed.equals("ManhuaFast")){
                        HomePage.this.manhuaFastOffset += 1;
                    }
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
        setIntent(intent); // update the current intent
        refreshBookmarks(); // refresh when Home button sends intent
    }

    private void refreshBookmarks() {
        if (homeListAdapter != null) homeListAdapter.refreshBookmarkStates();
        if (searchResultAdapter != null) searchResultAdapter.refreshBookmarkStates();
        Log.d("HomePage", "Bookmarks refreshed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBookmarks(); // refresh whenever activity comes to foreground
    }


}
