package com.example.mangav5.ServiceMaster;

import android.content.Context;

import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.BookmarkEntity;
import com.example.mangav5.Entity.MangaItemEntity;
import com.example.mangav5.Models.MangaItemModel;

import java.util.ArrayList;
import java.util.List;

public class NotificationController {

    public static void GetNotifications(Context context,MangaListCallback callback){
        AppDatabase db = AppDatabase.getInstance(context);

        List<BookmarkEntity> notifications = db.bookmarkDao().getAllBookmarks();
        List<MangaItemModel> items = new ArrayList<>();

        for (BookmarkEntity notification : notifications) {
            //WorkInProgress
        }

    }

    public interface MangaListCallback {
        void onSuccess(List<MangaItemModel> mangas);

        void onError(String message);
    }
}
