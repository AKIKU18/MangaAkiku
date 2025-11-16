package com.example.mangav5.Entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "settingsItem")
public class SettingsItemEntity {

    @PrimaryKey
    @NonNull
    private String key; // must be unique

    private String value;

    public SettingsItemEntity(@NonNull String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
}



