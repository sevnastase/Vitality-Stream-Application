package com.videostreamtest.config.entity.tracker;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "general_download_table")
public class GeneralDownloadTracker {
    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    private Integer id;

    @ColumnInfo(name = "download_type")
    @NonNull
    private String downloadType;

    @ColumnInfo(name = "download_current_file")
    @NonNull
    private String downloadCurrentFile;

    @ColumnInfo(name = "download_type_total")
    @NonNull
    private Integer downloadTypeTotal;

    @ColumnInfo(name = "download_type_current")
    @NonNull
    private Integer downloadTypeCurrent;

    @NonNull
    public Integer getId() {
        return id;
    }

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    @NonNull
    public String getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(@NonNull String downloadType) {
        this.downloadType = downloadType;
    }

    @NonNull
    public String getDownloadCurrentFile() {
        return downloadCurrentFile;
    }

    public void setDownloadCurrentFile(@NonNull String downloadCurrentFile) {
        this.downloadCurrentFile = downloadCurrentFile;
    }

    @NonNull
    public Integer getDownloadTypeTotal() {
        return downloadTypeTotal;
    }

    public void setDownloadTypeTotal(@NonNull Integer downloadTypeTotal) {
        this.downloadTypeTotal = downloadTypeTotal;
    }

    @NonNull
    public Integer getDownloadTypeCurrent() {
        return downloadTypeCurrent;
    }

    public void setDownloadTypeCurrent(@NonNull Integer downloadTypeCurrent) {
        this.downloadTypeCurrent = downloadTypeCurrent;
    }
}
