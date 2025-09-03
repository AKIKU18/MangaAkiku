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

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {

    private final List<MangaItemEntity> mangaList;
    private final Context context;
    private final TextView textTotalSize;

    public SettingsAdapter(List<MangaItemEntity> mangaList, Context context, TextView textTotalSize) {
        this.mangaList = mangaList;
        this.context = context;
        this.textTotalSize = textTotalSize;
    }

    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_settings_item, parent, false);
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
                .into(holder.image_cover);

        calculateMetadataSize(holder.text_size, manga.getMangaId());

        holder.button_delete.setOnClickListener(v -> {
            // Safe parsing with comma replacement
            double total_size = parseTextSize(textTotalSize.getText().toString());
            double holder_size = parseTextSize(holder.text_size.getText().toString());
            double calculate = Math.max(total_size - holder_size, 0);

            textTotalSize.setText(String.format("%.3f MB", calculate));
            deleteManga(manga);
        });
    }

    private double parseTextSize(String text) {
        try {
            return Double.parseDouble(text.replace(" MB", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void calculateMetadataSize(TextView holder, String mangaId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<ChapterItemEntity> chapters = db.chapterDao().getChaptersByMangaId(mangaId);
            MangaItemEntity manga= db.mangaItemDao().getMangaById(mangaId);
            long totalBytes = 0;
            totalBytes += manga.getCoverUrl() != null ? manga.getCoverUrl().getBytes(StandardCharsets.UTF_8).length : 0;
            totalBytes += manga.getTitle() != null ? manga.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
            totalBytes += manga.getMangaId() != null ? manga.getMangaId().getBytes(StandardCharsets.UTF_8).length : 0;
            totalBytes += manga.getDescription() != null ? manga.getDescription().getBytes(StandardCharsets.UTF_8).length : 0;

            for (ChapterItemEntity chapter : chapters) {
                totalBytes += chapter.getChapterId() != null ? chapter.getChapterId().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += chapter.getNumber() != null ? chapter.getNumber().getBytes(StandardCharsets.UTF_8).length : 0;
                totalBytes += chapter.getTitle() != null ? chapter.getTitle().getBytes(StandardCharsets.UTF_8).length : 0;
            }

            double sizeMB = totalBytes / 1024.0 / 1024.0;
            new Handler(Looper.getMainLooper()).post(() -> holder.setText(String.format("%.3f MB", sizeMB)));
        });
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
