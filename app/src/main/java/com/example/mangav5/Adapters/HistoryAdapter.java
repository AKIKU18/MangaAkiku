package com.example.mangav5.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.MainActivitys.ChapterPage;
import com.example.mangav5.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryEntity> historyList;
    private List<HistoryEntity> prevHistoryList;
    private final Context context;

    public HistoryAdapter(Context context) {
        this.historyList = new ArrayList<>();
        this.context = context;
        this.prevHistoryList = historyList;
    }

    public void setHistoryList(List<HistoryEntity> historyList) {
        // Keep all chapters for "Show more"
        this.prevHistoryList = new ArrayList<>(historyList);

        // Keep only latest chapter per manga
        Map<String, HistoryEntity> latestPerManga = new HashMap<>();
        for (HistoryEntity h : historyList) {
            if (!latestPerManga.containsKey(h.getMangaId()) ||
                    h.getTimestamp() > latestPerManga.get(h.getMangaId()).getTimestamp()) {
                latestPerManga.put(h.getMangaId(), h);
            }
        }
        this.historyList = new ArrayList<>(latestPerManga.values());

        // Sort latest chapters descending
        this.historyList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        notifyDataSetChanged();
    }



    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_history, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryEntity item = historyList.get(position);

        holder.mangaTitleTextView.setText(item.getMangaTitle());
        holder.chapterTitleTextView.setText(item.getChapterTitle());
        holder.mangaSource.setText(item.getSource());

        // Format timestamp
        String formattedTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(item.getTimestamp()));
        holder.timestampTextView.setText(formattedTime);

        // Load cover image
        if (item.getCoverUrl() != null && !item.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getCoverUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .error(android.R.drawable.ic_dialog_alert)
                    .dontTransform()
                    .into(holder.coverImageView);
        } else {
            holder.coverImageView.setImageResource(android.R.drawable.picture_frame);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChapterPage.class);
            intent.putExtra("chapterId", item.getChapterId());
            intent.putExtra("chapterTitle", item.getChapterTitle());
            intent.putExtra("mangaId", item.getMangaId());
            intent.putExtra("mangaUrl", item.getMangaUrl());
            intent.putExtra("chapterUrl", item.getChapterUrl());
            intent.putExtra("source", item.getSource());
            context.startActivity(intent);
        });

        ShowMoreOrLessHistory(holder,position);
    }

    private void ShowMoreOrLessHistory(HistoryViewHolder holder, int position){
        // Get the current manga item (latest chapter) for this RecyclerView position
        HistoryEntity currentItem = historyList.get(position);

        // Collect all previous chapters for the same manga, excluding the latest one
        List<HistoryEntity> previousChaptersList = new ArrayList<>();
        for (HistoryEntity h : prevHistoryList) {
            if (h.getMangaId().equals(currentItem.getMangaId()) &&
                    !h.getChapterId().equals(currentItem.getChapterId())) {
                previousChaptersList.add(h);
            }
        }
        // Sort previous chapters descending by timestamp (newest first)
        previousChaptersList.sort((a,b)-> Long.compare(b.getTimestamp(), a.getTimestamp()));

        // Prepare inflater to dynamically create views for each previous chapter
        LayoutInflater inflater = LayoutInflater.from(context);
        // Clear any old views from previous binding
        holder.moreChaptersLayout.removeAllViews();

        // If no previous chapters exist, hide "Show more" button and exit
        if (previousChaptersList.isEmpty()) {
            holder.showMoreTextView.setVisibility(View.GONE);
            return;
        }

        // Track how many previous chapters are currently visible
        final int[] visibleCount = {0};
        // Define the maximum number of chapters shown per click
        final int MAX_AT_ONCE = 5;

        // Initially, hide the expandable layout and show "Show more" button
        holder.moreChaptersLayout.setVisibility(View.GONE);
        holder.showMoreTextView.setVisibility(View.VISIBLE);
        holder.showMoreTextView.setText("Show more");

        // Set click listener for "Show more / Show less"
        holder.showMoreTextView.setOnClickListener(v -> {
            // First click: make the layout visible
            if (holder.moreChaptersLayout.getVisibility() == View.GONE) {
                holder.moreChaptersLayout.setVisibility(View.VISIBLE);
            }

            // Add the next batch of chapters (up to MAX_AT_ONCE)
            int added = 0;
            while (visibleCount[0] < previousChaptersList.size() && added < MAX_AT_ONCE) {
                HistoryEntity chapter = previousChaptersList.get(visibleCount[0]);

                // Inflate a new view for each chapter
                View view = inflater.inflate(R.layout.item_history_more_chapter, holder.moreChaptersLayout, false);
                TextView chapterTitle = view.findViewById(R.id.historyChapterTitle);
                TextView historyTimeStamp = view.findViewById(R.id.historyTimestamp);

                // Set chapter title and timestamp
                chapterTitle.setText(chapter.getChapterTitle());
                String formattedTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        .format(new Date(chapter.getTimestamp()));
                historyTimeStamp.setText(formattedTime);

                // Open the chapter when clicked
                chapterTitle.setOnClickListener(v2 -> openChapter(chapter));

                // Add this view to the expandable layout
                holder.moreChaptersLayout.addView(view);

                visibleCount[0]++; // increment total visible chapters
                added++;           // increment this batch count
            }

            // Update the "Show more / Show less" button text
            if (visibleCount[0] >= previousChaptersList.size()) {
                // All previous chapters are visible → show "Show less"
                holder.showMoreTextView.setText("Show less");
            } else {
                // Still more chapters to show → keep "Show more"
                holder.showMoreTextView.setText("Show more");
            }

            // Handle collapsing when user clicks "Show less"
            if ("Show less".equals(holder.showMoreTextView.getText().toString()) && visibleCount[0] >= previousChaptersList.size()) {
                holder.showMoreTextView.setOnClickListener(v3 -> {
                    // Remove all previous chapter views
                    holder.moreChaptersLayout.removeAllViews();
                    // Reset visible count
                    visibleCount[0] = 0;
                    // Hide the expandable layout
                    holder.moreChaptersLayout.setVisibility(View.GONE);
                    // Reset button text
                    holder.showMoreTextView.setText("Show more");
                    // Re-attach listener so next click shows next 5 chapters
                    ShowMoreOrLessHistory(holder, position);
                });
            }
        });
    }



    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    private void openChapter(HistoryEntity item) {
        Intent intent = new Intent(context, ChapterPage.class);
        intent.putExtra("chapterId", item.getChapterId());
        intent.putExtra("chapterTitle", item.getChapterTitle());
        intent.putExtra("mangaId", item.getMangaId());
        intent.putExtra("mangaUrl", item.getMangaUrl());
        intent.putExtra("chapterUrl", item.getChapterUrl());
        intent.putExtra("source", item.getSource());
        context.startActivity(intent);
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView mangaTitleTextView, chapterTitleTextView, timestampTextView, mangaSource, showMoreTextView;
        LinearLayout moreChaptersLayout;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.historyCover);
            mangaTitleTextView = itemView.findViewById(R.id.historyMangaTitle);
            chapterTitleTextView = itemView.findViewById(R.id.historyChapterTitle);
            timestampTextView = itemView.findViewById(R.id.historyTimestamp);
            mangaSource = itemView.findViewById(R.id.historyMangaSource);
            showMoreTextView = itemView.findViewById(R.id.historyShowMore);
            moreChaptersLayout = itemView.findViewById(R.id.historyMoreChaptersLayout);
        }
    }
}
