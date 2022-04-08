package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "movieflags_table")
public class MovieFlag {
    @PrimaryKey
    @ColumnInfo(name = "movieflag_id")
    @NonNull
    private Integer id;
    @ColumnInfo(name = "movie_id")
    @NonNull
    private Integer movieId;
    @ColumnInfo(name = "flag_id")
    @NonNull
    private Integer flagId;

    @NonNull
    public Integer getId() {
        return id;
    }

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    @NonNull
    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(@NonNull Integer movieId) {
        this.movieId = movieId;
    }

    @NonNull
    public Integer getFlagId() {
        return flagId;
    }

    public void setFlagId(@NonNull Integer flagId) {
        this.flagId = flagId;
    }
}
