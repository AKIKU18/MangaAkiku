package com.example.mangav5.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;

import java.util.ArrayList;
import java.util.List;

public class MangaPageAdapter extends RecyclerView.Adapter<MangaPageAdapter.MangaViewHolder> {
    private List<ChapterModel> chapters;
    private final Context context;

    public MangaPageAdapter(List<ChapterModel> chapters, Context context) {
        this.chapters = chapters;
        this.context = context;
    }

    @NonNull
    @Override
    public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_chapter, parent, false);
        return new MangaViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MangaViewHolder holder, int position) {
        ChapterModel chapter = chapters.get(position);
        holder.chapterTitle.setText(chapter.getTitle());
    }

    @Override
    public int getItemCount() {
        return chapters == null ? 0 : chapters.size();
    }

    public static class MangaViewHolder extends RecyclerView.ViewHolder {
        TextView chapterTitle;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterTitle = itemView.findViewById(R.id.chapterTitle);
        }
    }
}
