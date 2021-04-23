package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "routepart_table")
public class Routepart {
    @PrimaryKey
    @ColumnInfo(name = "routepart_id")
    @NonNull
    private Integer routepartId;

    @ColumnInfo(name = "movie_id")
    @NonNull
    private Integer movieId;

    @ColumnInfo(name = "moviepart_framenumber")
    @NonNull
    private Integer moviePartFrameNumber;

    @ColumnInfo(name = "moviepart_imagepath")
    @NonNull
    private String moviePartImagePath;

    @ColumnInfo(name = "moviepart_name")
    @NonNull
    private String moviePartName;

    @NonNull
    public Integer getRoutepartId() {
        return routepartId;
    }

    public void setRoutepartId(@NonNull Integer routepartId) {
        this.routepartId = routepartId;
    }

    @NonNull
    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(@NonNull Integer movieId) {
        this.movieId = movieId;
    }

    @NonNull
    public Integer getMoviePartFrameNumber() {
        return moviePartFrameNumber;
    }

    public void setMoviePartFrameNumber(@NonNull Integer moviePartFrameNumber) {
        this.moviePartFrameNumber = moviePartFrameNumber;
    }

    @NonNull
    public String getMoviePartImagePath() {
        return moviePartImagePath;
    }

    public void setMoviePartImagePath(@NonNull String moviePartImagePath) {
        this.moviePartImagePath = moviePartImagePath;
    }

    @NonNull
    public String getMoviePartName() {
        return moviePartName;
    }

    public void setMoviePartName(@NonNull String moviePartName) {
        this.moviePartName = moviePartName;
    }
}
