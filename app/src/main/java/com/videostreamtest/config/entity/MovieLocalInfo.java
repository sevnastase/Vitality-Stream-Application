package com.videostreamtest.config.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "movie_local_info_table")
public class MovieLocalInfo {
    @PrimaryKey
    @ColumnInfo(name = "movie_id")
    private Integer movieId;

    @ColumnInfo(name = "movie_scenery_path")
    private String movieSceneryPath;

    @ColumnInfo(name = "movie_map_path")
    private String movieMapPath;

    @ColumnInfo(name = "movie_flag_path")
    private String movieFlagPath;

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public String getMovieSceneryPath() {
        return movieSceneryPath;
    }

    public void setMovieSceneryPath(String movieSceneryPath) {
        this.movieSceneryPath = movieSceneryPath;
    }

    public String getMovieMapPath() {
        return movieMapPath;
    }

    public void setMovieMapPath(String movieMapPath) {
        this.movieMapPath = movieMapPath;
    }

    public String getMovieFlagPath() {
        return movieFlagPath;
    }

    public void setMovieFlagPath(String movieFlagPath) {
        this.movieFlagPath = movieFlagPath;
    }
}
