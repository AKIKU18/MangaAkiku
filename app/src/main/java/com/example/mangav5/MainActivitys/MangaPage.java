package com.example.mangav5.MainActivitys;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Adapters.MangaPageAdapter;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;
import com.example.mangav5.Services.ChaptersService;
import com.example.mangav5.Services.FeedMangaService;

import java.util.ArrayList;
import java.util.List;

public class MangaPage extends AppCompatActivity {
    RecyclerView chapterRecyclerView;
    MangaPageAdapter mangaPageAdapter;
    List<ChapterModel> chapterList;
    ImageView cover;
    ImageView bookmarkStar;
    TextView title, description;
    private MangaItemModel mangaItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manga_page);
        chapterList = new ArrayList<>();
        chapterRecyclerView = findViewById(R.id.chaptersRecyclerViewPage);
        chapterRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mangaPageAdapter = new MangaPageAdapter(chapterList, this);
        chapterRecyclerView.setAdapter(mangaPageAdapter);

        cover = findViewById(R.id.mangaCoverImagePage);
        bookmarkStar = findViewById(R.id.bookmarkButtonPage);
        title = findViewById(R.id.mangaTitlePage);
        description = findViewById(R.id.mangaDescriptionPage);





        loadMangaItem();
        loadChapterList();
    }

    private void loadMangaItem() {
        String mangaId = getIntent().getStringExtra("mangaId");

        FeedMangaService.fetchMangaById(mangaId, new FeedMangaService.MangaCallback() {

            @Override
            public void onSuccess(MangaItemModel manga) {
                runOnUiThread(() -> {
                    mangaItem = manga;
                    if (mangaItem != null && mangaItem.getCoverImageUrl() != null) {
                        title.setText(mangaItem.getTitle());
                        description.setText(mangaItem.getDescription());
                        Glide.with(MangaPage.this)
                                .load(mangaItem.getCoverImageUrl())
                                .placeholder(android.R.drawable.ic_dialog_info)
                                .error(android.R.drawable.ic_dialog_alert)
                                .into(cover);
                    } else {
                        cover.setImageResource(android.R.drawable.picture_frame);
                    }
                });;
            }

            @Override
            public void onError(String errorMessage) {

            };
        });
    }

    private void loadChapterList() {
        String mangaId = getIntent().getStringExtra("mangaId");
        ChaptersService.fetchChapterList(mangaId, new ChaptersService.ChapterListCallback(){
            @Override
            public void onSuccess(List<ChapterModel> chapters) {
                runOnUiThread(() -> {
                    chapterList.clear();
                    chapterList.addAll(chapters);
                    mangaPageAdapter.notifyDataSetChanged();
                    Log.e("MangaPage", "Chapters fetched successfully:" + chapters.size());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MangaPage.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
