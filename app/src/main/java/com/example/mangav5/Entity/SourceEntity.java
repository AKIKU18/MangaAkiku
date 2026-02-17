package com.example.mangav5.Entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sourceEntity")
public class SourceEntity {
    @PrimaryKey
    public int id = 1; // unique ID for each record
    public String mainSource;

    public SourceEntity(String mainSource) {
        this.mainSource = mainSource;
    }

    public String getMainSource() {
        return mainSource;
    }

    public void setMainSource(String mainSource) {
        this.mainSource = mainSource;
    }
}
