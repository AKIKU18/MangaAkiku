package com.example.mangav5.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.ChapterItemEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.R;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {
    private final List<MangaItemEntity> dbManga;
    private final Context context;
    private final TextView textTotalSize;
    public SettingsAdapter(List<MangaItemEntity> dbManga, Context context, TextView textTotalSize) {
        this.dbManga = dbManga;
        this.context = context;
        this.textTotalSize = textTotalSize;
    }

    @NonNull
    @Override
    public SettingsAdapter.SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_settings_item, parent, false);
        return new SettingsAdapter.SettingsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsAdapter.SettingsViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MangaItemEntity manga = dbManga.get(position);
        holder.text_title.setText(manga.getTitle());
        // Load cover
        Glide.with(context)
                .load(manga.getCoverUrl())
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.image_cover);

        holder.button_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                double total_size = Double.parseDouble(textTotalSize.getText().toString().replace(" MB", ""));
                double holder_size = Double.parseDouble(holder.text_size.getText().toString().replace(" MB", ""));
                double calculate = total_size - holder_size;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if(calculate <=0){
                        textTotalSize.setText(String.format("%.3f MB", 0.0));

                    }else{
                        textTotalSize.setText(String.format("%.3f MB", calculate));
                    }
                });
                DeleteManga(manga);
            }
        });
        calculateMetadataSize(holder.text_size,context, manga.getMangaId());

    }

    public static void calculateMetadataSize(TextView holder, Context context, String mangaId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            List<ChapterItemEntity> chapters = db.chapterDao().getChaptersByMangaId(mangaId);

            long totalBytes = 0;

            for (ChapterItemEntity chapter : chapters) {
                // Chapter ID size (if stored as string)
                if (chapter.getChapterId() != null)
                    totalBytes += chapter.getChapterId().getBytes(StandardCharsets.UTF_8).length;

                // Manga ID size (int)
                totalBytes += mangaId.getBytes(StandardCharsets.UTF_8).length;;

                // Title size
                if (chapter.getTitle() != null)
                    totalBytes += chapter.getTitle().getBytes(StandardCharsets.UTF_8).length;

                // Number size (int)
                totalBytes += chapter.getNumber().getBytes(StandardCharsets.UTF_8).length;;
            }

            double sizeKB = totalBytes / 1024.0;
            double sizeMB = sizeKB / 1024.0;
            // <-- POST TO MAIN THREAD
            new Handler(Looper.getMainLooper()).post(() -> {
                holder.setText(String.format("%.3f MB", sizeMB));
            });
            Log.d("ROOM_SIZE", "Manga ID " + mangaId + " metadata size: " + String.format("%.2f MB", sizeMB));
        });
    }


    private void DeleteManga(MangaItemEntity manga){
        AppDatabase db = AppDatabase.getInstance(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            // Delete from DB
            db.mangaItemDao().deleteManga(manga);
            db.chapterDao().deleteChaptersByMangaId(manga.getMangaId());

            // UI updates on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                int index = dbManga.indexOf(manga);
                if (index != -1) {
                    dbManga.remove(index);
                    notifyItemRemoved(index);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return dbManga.size();
    }

    public static class SettingsViewHolder extends RecyclerView.ViewHolder{
        ImageView image_cover;
        ImageButton button_delete;
        TextView text_title;
        TextView text_size;

        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);

            image_cover = itemView.findViewById(R.id.image_cover);
            button_delete = itemView.findViewById(R.id.button_delete);
            text_title = itemView.findViewById(R.id.text_title);
            text_size = itemView.findViewById(R.id.text_size);
        }
    }
}
