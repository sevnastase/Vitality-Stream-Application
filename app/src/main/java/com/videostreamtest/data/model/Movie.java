package com.videostreamtest.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Movie {
    private Integer id;
    private String movieTitle;
    private Integer movieLength;
    private String movieUrl; // e.g. /streams/{vodId}.mp4
    private String movieImagepath;
    private String movieRouteinfoPath;
    //Speeds are in Kmh
    private Integer recordedSpeed;
    private Integer minimalSpeed;
    private Integer recordedFps;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public Integer getMovieLength() {
        return movieLength;
    }

    public void setMovieLength(Integer movieLength) {
        this.movieLength = movieLength;
    }

    public String getMovieUrl() {
        return movieUrl;
    }

    public void setMovieUrl(String movieUrl) {
        this.movieUrl = movieUrl;
    }

    public String getMovieImagepath() {
        return movieImagepath;
    }

    public void setMovieImagepath(String movieImagepath) {
        this.movieImagepath = movieImagepath;
    }

    public String getMovieRouteinfoPath() {
        return movieRouteinfoPath;
    }

    public void setMovieRouteinfoPath(String movieRouteinfoPath) {
        this.movieRouteinfoPath = movieRouteinfoPath;
    }

    public Integer getRecordedSpeed() {
        return recordedSpeed;
    }

    public void setRecordedSpeed(Integer recordedSpeed) {
        this.recordedSpeed = recordedSpeed;
    }

    public Integer getMinimalSpeed() {
        return minimalSpeed;
    }

    public void setMinimalSpeed(Integer minimalSpeed) {
        this.minimalSpeed = minimalSpeed;
    }

    public Integer getRecordedFps() {
        return recordedFps;
    }

    public void setRecordedFps(Integer recordedFps) {
        this.recordedFps = recordedFps;
    }
}
