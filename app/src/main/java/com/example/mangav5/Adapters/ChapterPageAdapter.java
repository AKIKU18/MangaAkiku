package com.example.mangav5.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.mangav5.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class ChapterPageAdapter extends RecyclerView.Adapter<ChapterPageAdapter.ChapterPageViewer> {
    // ✅ Replace with your loading + error image URLs
    private static final String LOADING_GIF = "https://i.imgur.com/llF5iyg.gif"; // working loading animation
    private static final String ERROR_IMAGE = "https://i.imgur.com/qkPM0Ez.png"; // “No image” placeholder
    private final Context context;
    private List<String> pages;
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
                .asBitmap()
                .load(imageUrl)
                // reduce lag
                .override(1080, Target.SIZE_ORIGINAL)
                .dontAnimate()
                .format(DecodeFormat.PREFER_RGB_565)
                // cache optim
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                // placeholder
                .placeholder(R.drawable.loading)
                .error(R.drawable.image_error)
                .into(holder.imageContainer)
        ;
    }


    @Override
    public int getItemCount() {
        return pages.size();
    }



    public static class ChapterPageViewer extends RecyclerView.ViewHolder {
        PhotoView imageContainer;
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
