package com.example.mangav5.MainActivitys;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mangav5.Adapters.HomePageAdapter;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.R;

public class MangaPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manga_page);
    }
}
