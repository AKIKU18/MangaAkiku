package com.example.mangav5.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangav5.Dao.BookmarkDao;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.MainActivitys.ChapterPage;
import com.example.mangav5.Models.ChapterModel;
import com.example.mangav5.R;

import java.util.List;

public class MangaPageAdapter extends RecyclerView.Adapter<MangaPageAdapter.MangaViewHolder> {
    private List<ChapterModel> chapters;
    private final Context context;
    private final String mangaId;

    private final String mangaUrl;
    private BookmarkDao bookmarkDao;
    private BookmarksAdapter bookmarkAdapter;


    public MangaPageAdapter(List<ChapterModel> chapters, Context context,String mangaId,String mangaUrl) {
        this.chapters = chapters;
        this.context = context;
        this.mangaId = mangaId;
        AppDatabase db = AppDatabase.getInstance(context);
        this.bookmarkDao = db.bookmarkDao();
        this.mangaUrl = mangaUrl;
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

        GoToTheChapterPage(holder,chapter.getChapterId(),chapter.getTitle(),mangaUrl,chapter.getChapterUrl(),chapter.getSource());

    }

    @Override
    public int getItemCount() {
        return chapters == null ? 0 : chapters.size();
    }



    private void GoToTheChapterPage(MangaViewHolder holder, String chapterId, String chapterTitle, String mangaUrl, String chapterUrl,String source){
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChapterPage.class);
            intent.putExtra("chapterId", chapterId);
            intent.putExtra("chapterTitle", chapterTitle);
            intent.putExtra("mangaId", mangaId);
            intent.putExtra("mangaUrl",mangaUrl);
            intent.putExtra("chapterUrl",chapterUrl);
            intent.putExtra("source",source);

            Log.e("MangaPageAdapter", "mangaUrl " + mangaUrl);
            //Log.e("MangaPageAdapter", "chapterTitle " + chapterTitle);
            context.startActivity(intent);
        });
    }


    public static class MangaViewHolder extends RecyclerView.ViewHolder {
        TextView chapterTitle;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterTitle = itemView.findViewById(R.id.chapterTitle);
        }
    }
}
