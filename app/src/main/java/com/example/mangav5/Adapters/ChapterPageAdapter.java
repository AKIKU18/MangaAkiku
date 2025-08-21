package com.example.mangav5.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangav5.MainActivitys.MangaPage;
import com.example.mangav5.Models.MangaItemModel;
import com.example.mangav5.R;

import java.util.List;

public class ChapterPageAdapter extends RecyclerView.Adapter<ChapterPageAdapter.ChapterPageViewer> {
    private List<String> pages;
    private final Context context;
    private String chapterNumber;


    public ChapterPageAdapter(List<String> pages, Context context, String chapterNumber) {
        this.pages = pages;
        this.context = context;
        this.chapterNumber = chapterNumber;
    }

    @NonNull
    @Override
    public ChapterPageViewer onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_page, parent, false);
        return new ChapterPageViewer(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull ChapterPageViewer holder, int position) {
        String imageUrl = pages.get(position);
        Glide.with(context)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(holder.imageContainer);


    }



    @Override
    public int getItemCount() {
        return pages.size();
    }

    public static class ChapterPageViewer extends RecyclerView.ViewHolder {
       ImageView imageContainer;
       TextView tvChapterNumber;
       TextView tvMangaTitle;

       public ChapterPageViewer(@NonNull View itemView) {
           super(itemView);
           imageContainer = itemView.findViewById(R.id.imageContainer);
           tvChapterNumber = itemView.findViewById(R.id.chapterNumber);
           tvMangaTitle = itemView.findViewById(R.id.mangaTitle);
       }

   }

}
