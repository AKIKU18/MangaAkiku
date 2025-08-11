package com.example.mangav5.MainActivitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
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

public class BookmarksPage extends AppCompatActivity {

    private RecyclerView bookmarkRecyclerView;
    private BookmarksAdapter bookmarkAdapter;
    private List<BookmarkEntity> bookmarkList = new ArrayList<>();
    private BookmarkDao bookmarkDao;
    private Button homePageButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bookmarks_page);
        bookmarkRecyclerView = findViewById(R.id.recycler_bookmarks);
        homePageButton = findViewById(R.id.btn_home);

        AppDatabase db = AppDatabase.getInstance(this);
        this.bookmarkDao =db.bookmarkDao();

        bookmarkAdapter = new BookmarksAdapter(bookmarkList, this);

        bookmarkRecyclerView.setAdapter(bookmarkAdapter);

        bookmarkRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadBookmarks();
        CheckBookmarked();
        IntentHandleGoBack();
        OnClickGoToHomePage();
    }

    private void OnClickGoToHomePage(){
        homePageButton.setOnClickListener(v -> {
            Intent intent = new Intent(BookmarksPage.this, HomePage.class);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    private void IntentHandleGoBack(){
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void loadBookmarks() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<BookmarkEntity> bookmarks = bookmarkDao.getAllBookmarks();
            runOnUiThread(() -> {
                bookmarkList.clear();
                bookmarkList.addAll(bookmarks);
                Log.e("BookmarksPage", "Number of bookmarks: " + bookmarks.size());

                bookmarkAdapter.notifyDataSetChanged();
            });
        });
    }

    public void CheckBookmarked() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("unbookmarked", true); // or false if nothing changed
                setResult(RESULT_OK, resultIntent);
                finish(); // close the activity
            }
        });

    }
}