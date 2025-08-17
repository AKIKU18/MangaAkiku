package com.example.mangav5.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangav5.Entity.HistoryEntity;
import com.example.mangav5.MainActivitys.ChapterPage;
import com.example.mangav5.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryEntity> historyList;
    private final Context context;

    // start with empty list
    public HistoryAdapter(Context context) {
        this.historyList = new ArrayList<>();
        this.context = context;
    }

    public void setHistoryList(List<HistoryEntity> historyList) {
        this.historyList = historyList;
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
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, @SuppressLint("RecyclerView") int position) {
        HistoryEntity item = historyList.get(position);

        // Manga title: right now we don’t have a dedicated field → using description as placeholder
        holder.mangaTitleTextView.setText(item.getMangaTitle());

        holder.chapterTitleTextView.setText(item.getChapterTitle());

        // format timestamp
        String formattedTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(item.getTimestamp()));
        holder.timestampTextView.setText(formattedTime);

        // Load cover image
        if (item.getCoverUrl() != null && !item.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getCoverUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(holder.coverImageView);
        } else {
            holder.coverImageView.setImageResource(android.R.drawable.picture_frame);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ChapterPage.class);
                intent.putExtra("chapterId", historyList.get(position).getChapterId());
                intent.putExtra("chapterTitle", historyList.get(position).getChapterTitle());
                intent.putExtra("mangaId", historyList.get(position).getMangaId());
                Log.e("MangaPageAdapter", "chapterTitle " + historyList.get(position).getChapterTitle());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView coverImageView;
        private final TextView mangaTitleTextView;
        private final TextView chapterTitleTextView;
        private final TextView timestampTextView;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.historyCover);
            mangaTitleTextView = itemView.findViewById(R.id.historyMangaTitle);
            chapterTitleTextView = itemView.findViewById(R.id.historyChapterTitle);
            timestampTextView = itemView.findViewById(R.id.historyTimestamp);
        }
    }
}
