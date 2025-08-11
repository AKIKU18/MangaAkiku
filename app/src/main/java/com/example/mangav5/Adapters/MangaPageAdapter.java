package com.example.mangav5.Adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.R;

public class MangaPageAdapter extends RecyclerView.Adapter<MangaPageAdapter.MangaViewHolder> {
    @NonNull
    @Override
    public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull MangaViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class MangaViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        ImageView bookmarkStar;
        TextView title, description;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.mangaCoverImage);
            bookmarkStar = itemView.findViewById(R.id.bookmarkStar);
            title = itemView.findViewById(R.id.mangaTitle);
            description = itemView.findViewById(R.id.mangaDescription);
        }
    }
}
