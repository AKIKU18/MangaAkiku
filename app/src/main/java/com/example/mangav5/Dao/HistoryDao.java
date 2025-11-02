package com.example.mangav5.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mangav5.Entity.HistoryEntity;

import java.util.List;

@Dao
public interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    List<HistoryEntity> getAllHistory();

    @Query("DELETE FROM history")
    void deleteAllHistory();

    @Query("DELETE FROM history WHERE mangaId = :mangaId")
    void deleteHistoryItem(String mangaId);

    @Query("SELECT * FROM history WHERE mangaId = :mangaId")
    HistoryEntity getHistoryItem(String mangaId);

    @Insert()
    void insertHistoryItem(HistoryEntity historyItem);

    @Query("SELECT * FROM history WHERE mangaId = :mangaId")
    List<HistoryEntity> getSameHistoryItemByMangaId(String mangaId);

    @Query("SELECT * FROM history WHERE mangaId = :mangaId ORDER BY timestamp DESC LIMIT 1")
    HistoryEntity getHistoryItemInOrder(String mangaId);
}
