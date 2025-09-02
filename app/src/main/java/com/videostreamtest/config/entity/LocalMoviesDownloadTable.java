package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "download_table")
public class LocalMoviesDownloadTable {
    @PrimaryKey
    @ColumnInfo(name = "download_id")
    @NonNull
    private Integer movieId;

    @ColumnInfo(name = "download_movie_id")
    @NonNull
    private Integer downloadMovieId;

    @ColumnInfo(name = "download_status", defaultValue = "-1")
    @NonNull
    private Integer downloadStatus;

    @NonNull
    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(@NonNull Integer movieId) {
        this.movieId = movieId;
    }

    @NonNull
    public Integer getDownloadMovieId() {
        return downloadMovieId;
    }

    public void setDownloadMovieId(@NonNull Integer downloadMovieId) {
        this.downloadMovieId = downloadMovieId;
    }

    @NonNull
    public Integer getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(@NonNull Integer downloadStatus) {
        this.downloadStatus = downloadStatus;
    }
}
