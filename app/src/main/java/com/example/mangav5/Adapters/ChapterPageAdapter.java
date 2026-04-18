package com.example.mangav5.Adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.mangav5.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChapterPageAdapter extends RecyclerView.Adapter<ChapterPageAdapter.ChapterPageViewer> {

    public interface OnImageStateListener {
        void onFirstImageLoadSuccess();
        void onFirstImageLoadFailed(String failedUrl);
    }

    private final Context context;
    private final List<String> pages;
    private String chapterNumber;

    private OnImageStateListener imageStateListener;

    private boolean firstImageSuccessDispatched = false;
    private boolean firstImageFailureDispatched = false;
    private final Set<String> loadedUrls = new HashSet<>();
    private final Set<String> failedUrls = new HashSet<>();

    public ChapterPageAdapter(List<String> pages, Context context, String chapterNumber) {
        this.pages = pages;
        this.context = context;
        this.chapterNumber = chapterNumber;
    }

    public void setOnImageStateListener(OnImageStateListener listener) {
        this.imageStateListener = listener;
    }

    public void resetLoadingState() {
        firstImageSuccessDispatched = false;
        firstImageFailureDispatched = false;
        loadedUrls.clear();
        failedUrls.clear();
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
                .override(1080, Target.SIZE_ORIGINAL)
                .dontAnimate()
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.loading)
                .error(R.drawable.image_error)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        failedUrls.add(imageUrl);

                        if (position == 0 && !firstImageSuccessDispatched && !firstImageFailureDispatched) {
                            firstImageFailureDispatched = true;
                            if (imageStateListener != null) {
                                imageStateListener.onFirstImageLoadFailed(imageUrl);
                            }
                        }

                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        loadedUrls.add(imageUrl);

                        if (!firstImageSuccessDispatched) {
                            firstImageSuccessDispatched = true;
                            if (imageStateListener != null) {
                                imageStateListener.onFirstImageLoadSuccess();
                            }
                        }

                        return false;
                    }
                })
                .into(holder.imageContainer);
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