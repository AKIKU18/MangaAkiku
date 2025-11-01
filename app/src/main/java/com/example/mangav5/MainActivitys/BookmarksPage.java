package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Adapters.BookmarksAdapter;
import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Activity responsible for displaying the user's bookmarked manga.
 * It retrieves bookmark data from the Room database and displays it in a RecyclerView.
 * It also handles navigation and updates the bookmark list if changes occur in other activities.
 */
public class BookmarksPage extends AppCompatActivity {

    // UI Elements
    private RecyclerView bookmarkRecyclerView; // Displays the list of bookmarked manga.
    private Button homePageButton;             // Navigates the user back to the HomePage.
    private TextView emptyText;                // A text view shown when the bookmark list is empty.
    private View bookmarkBlurBackground;       // A background view that can have effects applied.

    // Data and Adapters
    private BookmarksAdapter bookmarkAdapter;
    private List<BookmarkEntity> bookmarkList = new ArrayList<>();
    private BookmarkDao bookmarkDao; // Data Access Object for bookmark-related database operations.

    /**
     * Launcher to handle results from other activities, specifically to check if a bookmark has been changed.
     * This allows the page to refresh its content when returning from a manga details page.
     */
    public static ActivityResultLauncher<Intent> mangaPageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hides the default action bar for a custom UI.
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set the layout for this activity.
        setContentView(R.layout.activity_bookmarks_page);

        // Initialize UI components from the layout.
        bookmarkRecyclerView = findViewById(R.id.recycler_bookmarks);
        homePageButton = findViewById(R.id.btn_home);
        bookmarkBlurBackground = findViewById(R.id.bookmarkBlurBackground);
        emptyText = findViewById(R.id.tv_empty);

        // Get an instance of the Room database and the BookmarkDAO.
        AppDatabase db = AppDatabase.getInstance(this);
        this.bookmarkDao = db.bookmarkDao();

        // Initialize the adapter with the (currently empty) bookmark list.
        bookmarkAdapter = new BookmarksAdapter(bookmarkList, this);
        bookmarkRecyclerView.setAdapter(bookmarkAdapter);
        bookmarkRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Call initialization methods.
        loadBookmarks();
        IntentHandleGoBack();
        OnClickGoToHomePage();
        CheckIfStillBookmarked();

        // Note: The CheckBookmarked() method seems to have overlapping functionality with IntentHandleGoBack().
        // Consider consolidating them to avoid registering two callbacks for the same back press event.
    }

    /**
     * Applies a blur effect to a given view.
     * This effect is only available on Android 12 (API 31) and higher.
     * @param view The view to which the blur effect will be applied.
     */
    private void BlurEffect(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(
                    RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
            );
        }
    }

    /**
     * Registers an ActivityResultLauncher to listen for results from the MangaPage.
     * If the result indicates that a bookmark status has changed, it reloads the bookmark list.
     */
    private void CheckIfStillBookmarked() {
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
    private void OnClickGoToHomePage() {
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
    private void IntentHandleGoBack() {
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
    private void loadBookmarks() {
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
     * This method seems redundant as it registers another back press callback.
     * The functionality of sending a result back is already handled in IntentHandleGoBack().
     * Consider removing this method to avoid conflicts and simplify the code.
     */
    public void CheckBookmarked() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("unbookmarked", true); // This might be used for specific logic.
                Log.d("BookmarksPage", "Back pressed from CheckBookmarked callback.");
                setResult(RESULT_OK, resultIntent);
                finish(); // Close the activity.
            }
        });
    }

    /**
     * Activity lifecycle method called when the activity is becoming visible to the user.
     * It's a good place to refresh data that might have changed while the activity was paused.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Reload bookmarks every time the user returns to this page.
        loadBookmarks();
    }
}
