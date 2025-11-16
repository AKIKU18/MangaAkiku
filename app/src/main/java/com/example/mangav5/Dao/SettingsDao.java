package com.example.mangav5.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mangav5.Entity.SettingsItemEntity;

@Dao
public interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSetting(SettingsItemEntity setting);

    @Query("SELECT * FROM settingsItem WHERE `key` = :key LIMIT 1")
    SettingsItemEntity getSetting(String key);

    @Query("DELETE FROM settingsItem")
    void deleteSettings();
}


