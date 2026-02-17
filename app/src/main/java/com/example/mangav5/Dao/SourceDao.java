package com.example.mangav5.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mangav5.Entity.SourceEntity;

import java.util.List;

@Dao
public interface SourceDao {
    //Add source or if already exist update
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void addSource(SourceEntity source);

    //Get all sources
    @Query("SELECT * FROM sourceEntity")
    public SourceEntity getSource();
}
