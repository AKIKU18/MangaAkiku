package com.example.mangav5.Adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.R;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

public class StorageUsageAdapter extends RecyclerView.Adapter<StorageUsageAdapter.SettingsViewHolder> {

    private final List<MangaItemEntity> mangaList;
    private final Context context;
    private final TextView textTotalSize;

    public StorageUsageAdapter(List<MangaItemEntity> mangaList, Context context, TextView textTotalSize) {
        this.mangaList = mangaList;
        this.context = context;
        this.textTotalSize = textTotalSize;
    }

    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_storage_usage_item, parent, false);
        return new SettingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        MangaItemEntity manga = mangaList.get(position);
        holder.text_title.setText(manga.getTitle());

        Glide.with(context)
                .load(manga.getCoverUrl())
                .placeholder(R.drawable.ic_launcher_foreground)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontTransform()
                .into(holder.image_cover);

        calculateMetadataSize(holder.text_size, manga.getMangaId());

        holder.button_delete.setOnClickListener(v -> {
            double totalSizeBytes = parseSizeToBytes(textTotalSize.getText().toString());
            double itemSizeBytes = parseSizeToBytes(holder.text_size.getText().toString());
            double newTotal = Math.max(totalSizeBytes - itemSizeBytes, 0);

            textTotalSize.setText(formatSizeFromBytes(newTotal));
            deleteManga(manga);
        });
    }

    private void calculateMetadataSize(TextView holder, String mangaId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            MangaItemEntity manga = db.mangaItemDao().getMangaById(mangaId);

            long totalBytes = 0;

            // Manga info
            totalBytes += manga.getCoverUrl() != null ? manga.getCoverUrl().getBytes(StandardCharsets.UTF_8).length : 0;
            totalBytes += manga.getTitle() != null ? manga.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
            totalBytes += manga.getMangaId() != null ? manga.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
            totalBytes += manga.getDescription() != null ? manga.getDescription().getBytes(StandardCharsets.UTF_8).length : 0;
            totalBytes += manga.getLastChapter() != null ? manga.getLastChapter().getBytes(StandardCharsets.UTF_8).length : 0;
            totalBytes += manga.getSource() != null ? manga.getSource().getBytes(StandardCharsets.UTF_8).length : 0;

            // Chapters
            List<ChapterItemEntity> chapters = db.chapterDao().getChaptersByMangaId(manga.getMangaId());
            for (ChapterItemEntity chapter : chapters) {
                totalBytes += chapter.getChapterId() != null ? chapter.getChapterId().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += chapter.getNumber() != null ? chapter.getNumber().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += chapter.getTitle() != null ? chapter.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += chapter.getChapterUrl() != null ? chapter.getChapterUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += chapter.getMangaId() != null ? chapter.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += chapter.getSource() != null ? chapter.getSource().getBytes(StandardCharsets.UTF_8).length : 0;
            }

            //History
            List<com.example.mangav5.Entity.HistoryEntity> history = db.historyDao().getAllHistory();
            for (com.example.mangav5.Entity.HistoryEntity h : history) {
                totalBytes += h.getMangaId() != null ? h.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += h.getChapterId() != null ? h.getChapterId().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += h.getChapterTitle() != null ? h.getChapterTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += h.getCoverUrl() != null ? h.getCoverUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += h.getDescription() != null ? h.getDescription().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += String.valueOf(h.getTimestamp()).getBytes(StandardCharsets.UTF_8).length;
                totalBytes += h.getMangaTitle() != null ? h.getMangaTitle().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += h.getMangaUrl() != null ? h.getMangaUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += h.getChapterUrl() != null ? h.getChapterUrl().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += h.getSource() != null ? h.getSource().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += String.valueOf(h.getScrollPosition()).getBytes(StandardCharsets.UTF_8).length;
            }

            String sizeText = formatSizeFromBytes(totalBytes);

            new Handler(Looper.getMainLooper()).post(() -> holder.setText(sizeText));
        });
    }

    // Parsează un text ca "3.02 MB" sau "512 KB" în bytes
    private double parseSizeToBytes(String text) {
        text = text.trim().toUpperCase().replace(",", ".");
        double value = 0.0;

        try {
            if (text.endsWith("GB")) {
                value = Double.parseDouble(text.replace("GB", "").trim()) * 1024 * 1024 * 1024;
            } else if (text.endsWith("MB")) {
                value = Double.parseDouble(text.replace("MB", "").trim()) * 1024 * 1024;
            } else if (text.endsWith("KB")) {
                value = Double.parseDouble(text.replace("KB", "").trim()) * 1024;
            } else if (text.endsWith("B")) {
                value = Double.parseDouble(text.replace("B", "").trim());
            } else {
                value = Double.parseDouble(text.trim());
            }
        } catch (NumberFormatException e) {
            value = 0.0;
        }

        return value;
    }

    private String formatSizeFromBytes(double bytes) {
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;

        if (gb >= 1) return String.format("%.2f GB", gb);
        else if (mb >= 1) return String.format("%.2f MB", mb);
        else if (kb >= 1) return String.format("%.2f KB", kb);
        else return String.format("%.2f B", bytes);
    }

    private void deleteManga(MangaItemEntity manga) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            db.chapterDao().deleteChaptersByMangaId(manga.getMangaId());
            db.mangaItemDao().deleteManga(manga);

            new Handler(Looper.getMainLooper()).post(() -> {
                int index = mangaList.indexOf(manga);
                if (index != -1) {
                    mangaList.remove(index);
                    notifyItemRemoved(index);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    static class SettingsViewHolder extends RecyclerView.ViewHolder {
        ImageView image_cover;
        ImageButton button_delete;
        TextView text_title, text_size;

        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);
            image_cover = itemView.findViewById(R.id.image_cover);
            button_delete = itemView.findViewById(R.id.button_delete);
            text_title = itemView.findViewById(R.id.text_title);
            text_size = itemView.findViewById(R.id.text_size);
        }
    }
}
