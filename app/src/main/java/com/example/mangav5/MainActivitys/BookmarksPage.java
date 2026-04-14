package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.BookmarksAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.ServiceMaster.ServiceController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Activity responsible for displaying the user's bookmarked manga.
 * It retrieves bookmark data from the Room database and displays it in a RecyclerView.
 * It also handles navigation and updates the bookmark list if changes occur in other activities.
 */
public class BookmarksPage extends AppCompatActivity {

    /**
     * Launcher to handle results from other activities, specifically to check if a bookmark has been changed.
     * This allows the page to refresh its content when returning from a manga details page.
     */
    public static ActivityResultLauncher<Intent> mangaPageLauncher;
    AppDatabase db = AppDatabase.getInstance(this);
    // UI Elements
    private RecyclerView bookmarkRecyclerView; // Displays the list of bookmarked manga.
    private Button homePageButton;             // Navigates the user back to the HomePage.
    private TextView emptyText;                // A text view shown when the bookmark list is empty.
    private TextView bookmark_loading_text;                // A text view shown when the bookmark list is empty.
    private View bookmarkBlurBackground;       // A background view that can have effects applied.
    private SearchView search_bookmarks;        // search through the list
    // Data and Adapters
    private BookmarksAdapter bookmarkAdapter;
    private List<BookmarkEntity> bookmarkList = new ArrayList<>();
    private BookmarkDao bookmarkDao; // Data Access Object for bookmark-related database operations.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hides the default action bar for a custom UI.
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Set the layout for this activity.
        setContentView(R.layout.activity_bookmarks_page);

        // Initialize UI components from the layout.
        bookmarkRecyclerView = findViewById(R.id.recycler_bookmarks);
        homePageButton = findViewById(R.id.btn_home);
        bookmarkBlurBackground = findViewById(R.id.bookmarkBlurBackground);
        emptyText = findViewById(R.id.tv_empty);
        bookmark_loading_text = findViewById(R.id.bookmark_loading_text);
        search_bookmarks = findViewById(R.id.search_bookmarks);
        // Get an instance of the Room database and the BookmarkDAO.
        this.bookmarkDao = db.bookmarkDao();

        // Initialize the adapter with the (currently empty) bookmark list.
        bookmarkAdapter = new BookmarksAdapter(bookmarkList, this);
        bookmarkRecyclerView.setAdapter(bookmarkAdapter);
        bookmarkRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        search_bookmarks.setQueryHint("Search bookmarks...");
        search_bookmarks.setIconifiedByDefault(false);
        // Call initialization methods.
        loadBookmarks();
        IntentHandleGoBack();
        OnClickGoToHomePage();
        CheckIfStillBookmarked();
        SearchThroughBookmarks();
    }

    private void SearchThroughBookmarks() {
        search_bookmarks.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search_bookmarks.clearFocus();
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    loadBookmarks();
                    search_bookmarks.setQueryHint("Search bookmarks...");
                    bookmark_loading_text.setVisibility(View.GONE);
                } else {
                    search_bookmarks.setQueryHint("");
                    performSearch(newText);
                }
                return true;
            }
        });
    }

    private void performSearch(String query) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<BookmarkEntity> allBookmarks = db.bookmarkDao().getAllBookmarks();

            // Filter bookmarks by title matching the query
            List<BookmarkEntity> filteredList = new ArrayList<>();
            for (BookmarkEntity bookmark : allBookmarks) {
                if (bookmark.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(bookmark);
                }
            }

            runOnUiThread(() -> {
                if (filteredList.isEmpty()) {
                    bookmark_loading_text.setVisibility(View.VISIBLE);  // Show empty message
                    bookmarkList.clear();
                    bookmarkList.addAll(filteredList);
                    bookmarkAdapter.notifyDataSetChanged();
                } else {
                    bookmark_loading_text.setVisibility(View.GONE);
                    bookmarkList.clear();
                    bookmarkList.addAll(filteredList);
                    bookmarkAdapter.notifyDataSetChanged();
                }

                // Update your RecyclerView adapter
            });
        });
    }

        /**
         * Registers an ActivityResultLauncher to listen for results from the MangaPage.
         * If the result indicates that a bookmark status has changed, it reloads the bookmark list.
         */
        private void CheckIfStillBookmarked () {
            mangaPageLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        // Check if the result is successful.
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                // Check the "bookmarkChanged" extra to see if a refresh is needed.
                                boolean bookmarkChanged = data.getBooleanExtra("bookmarkChanged", false);
                                if (bookmarkChanged) {
                                    loadBookmarks(); // Reload the data.
                                }
                            }
                        }
                    }
            );
        }

        /**
         * Sets up the click listener for the "Home" button to navigate back to the HomePage.
         */
        private void OnClickGoToHomePage () {
            homePageButton.setOnClickListener(v -> {
                Intent intent = new Intent(BookmarksPage.this, HomePage.class);
                setResult(RESULT_OK, intent); // Set result to OK.
                finish(); // Close this activity.
            });
        }

        /**
         * Overrides the default behavior of the system back button.
         * Ensures a proper result is sent back to the calling activity before finishing.
         */
        private void IntentHandleGoBack () {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    Log.d("BookmarksPage", "Back button pressed, finishing with RESULT_OK.");
                    finish(); // Finish the activity.
                }
            });
        }

        /**
         * Fetches all bookmarks from the database on a background thread.
         * Updates the RecyclerView and UI on the main thread once the data is loaded.
         */
        private void loadBookmarks () {
            Executors.newSingleThreadExecutor().execute(() -> {
                // Fetch all bookmark records from the database.
                List<BookmarkEntity> bookmarks = bookmarkDao.getAllBookmarks();

                // Switch back to the main thread to update the UI.
                runOnUiThread(() -> {
                    bookmarkList.clear();
                    bookmarkList.addAll(bookmarks);
                    Log.d("BookmarksPage", "Loaded " + bookmarks.size() + " bookmarks.");

                    // Show or hide the "empty" message based on whether the list has items.
                    if (bookmarks.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                    }

                    // Notify the adapter that the data set has changed, so it can redraw the list.
                    bookmarkAdapter.notifyDataSetChanged();
                });
            });
        }

        /**
         * Activity lifecycle method called when the activity is becoming visible to the user.
         * It's a good place to refresh data that might have changed while the activity was paused.
         */
        @Override
        protected void onResume () {
            super.onResume();
            // Reload bookmarks every time the user returns to this page.
            loadBookmarks();
        }
    }
