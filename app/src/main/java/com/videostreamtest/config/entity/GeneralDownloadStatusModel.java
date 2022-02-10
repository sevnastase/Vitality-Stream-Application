package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "general_download_status_table")
public class GeneralDownloadStatusModel {
    @PrimaryKey
    @ColumnInfo(name = "download_id")
    @NonNull
    private Integer movieId;

    @ColumnInfo(name = "download_current_count")
    @NonNull
    private Integer currentCount;

    @ColumnInfo(name = "download_total_count")
    @NonNull
    private Integer totalCount;

    @ColumnInfo(name = "download_subject", defaultValue = "general")
    @NonNull
    private String downloadSubject;

    @ColumnInfo(name = "download_filename", defaultValue = "general.txt")
    @NonNull
    private String downloadFilename;

    @NonNull
    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(@NonNull Integer movieId) {
        this.movieId = movieId;
    }

    @NonNull
    public Integer getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(@NonNull Integer currentCount) {
        this.currentCount = currentCount;
    }

    @NonNull
    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(@NonNull Integer totalCount) {
        this.totalCount = totalCount;
    }

    @NonNull
    public String getDownloadSubject() {
        return downloadSubject;
    }

    public void setDownloadSubject(@NonNull String downloadSubject) {
        this.downloadSubject = downloadSubject;
    }

    @NonNull
    public String getDownloadFilename() {
        return downloadFilename;
    }

    public void setDownloadFilename(@NonNull String downloadFilename) {
        this.downloadFilename = downloadFilename;
    }
}
