package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "routefilm_table")
public class Routefilm {
    @PrimaryKey
    @ColumnInfo(name = "movie_id")
    @NonNull
    private Integer movieId;

    @ColumnInfo(name = "account_token")
    @NonNull
    private String accountToken;

    @ColumnInfo(name = "movie_title")
    @NonNull
    private String movieTitle;

    @ColumnInfo(name = "movie_length")
    @NonNull
    private Integer movieLength;

    @ColumnInfo(name = "movie_url")
    @NonNull
    private String movieUrl; // e.g. /streams/{vodId}.mp4

    @ColumnInfo(name = "movie_imagepath")
    @NonNull
    private String movieImagepath;

    @ColumnInfo(name = "movie_routeinfo_path")
    @NonNull
    private String movieRouteinfoPath;

    @ColumnInfo(name = "movie_filesize")
    @NonNull
    private long movieFileSize;

    @ColumnInfo(name = "movie_scenery_filesize")
    @NonNull
    private long sceneryFileSize;

    @ColumnInfo(name = "movie_map_filesize")
    @NonNull
    private long mapFileSize;

    //Speeds are in Kmh
    @ColumnInfo(name = "movie_recorded_speed")
    @NonNull
    private Integer recordedSpeed;

    @ColumnInfo(name = "movie_minimal_speed")
    @NonNull
    private Integer minimalSpeed;

    @ColumnInfo(name = "movie_recorded_fps")
    @NonNull
    private Integer recordedFps;

    @NonNull
    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(@NonNull Integer movieId) {
        this.movieId = movieId;
    }

    @NonNull
    public String getAccountToken() {
        return accountToken;
    }

    public void setAccountToken(@NonNull String accountToken) {
        this.accountToken = accountToken;
    }

    @NonNull
    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(@NonNull String movieTitle) {
        this.movieTitle = movieTitle;
    }

    @NonNull
    public Integer getMovieLength() {
        return movieLength;
    }

    public void setMovieLength(@NonNull Integer movieLength) {
        this.movieLength = movieLength;
    }

    @NonNull
    public String getMovieUrl() {
        return movieUrl;
    }

    public void setMovieUrl(@NonNull String movieUrl) {
        this.movieUrl = movieUrl;
    }

    @NonNull
    public String getMovieImagepath() {
        return movieImagepath;
    }

    public void setMovieImagepath(@NonNull String movieImagepath) {
        this.movieImagepath = movieImagepath;
    }

    @NonNull
    public String getMovieRouteinfoPath() {
        return movieRouteinfoPath;
    }

    public void setMovieRouteinfoPath(@NonNull String movieRouteinfoPath) {
        this.movieRouteinfoPath = movieRouteinfoPath;
    }

    public long getMovieFileSize() {
        return movieFileSize;
    }

    public void setMovieFileSize(long movieFileSize) {
        this.movieFileSize = movieFileSize;
    }

    public long getSceneryFileSize() {
        return sceneryFileSize;
    }

    public void setSceneryFileSize(long sceneryFileSize) {
        this.sceneryFileSize = sceneryFileSize;
    }

    public long getMapFileSize() {
        return mapFileSize;
    }

    public void setMapFileSize(long mapFileSize) {
        this.mapFileSize = mapFileSize;
    }

    @NonNull
    public Integer getRecordedSpeed() {
        return recordedSpeed;
    }

    public void setRecordedSpeed(@NonNull Integer recordedSpeed) {
        this.recordedSpeed = recordedSpeed;
    }

    @NonNull
    public Integer getMinimalSpeed() {
        return minimalSpeed;
    }

    public void setMinimalSpeed(@NonNull Integer minimalSpeed) {
        this.minimalSpeed = minimalSpeed;
    }

    @NonNull
    public Integer getRecordedFps() {
        return recordedFps;
    }

    public void setRecordedFps(@NonNull Integer recordedFps) {
        this.recordedFps = recordedFps;
    }
}
