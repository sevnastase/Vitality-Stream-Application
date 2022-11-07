package com.videostreamtest.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.videostreamtest.config.entity.Routefilm;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Movie {
    private Integer id;
    private String movieTitle;
    private Integer movieLength;
    private String movieUrl; // e.g. /streams/{vodId}.mp4
    private String movieDashUrl; // e.g. /streams/{vodId}.mp4
    private long movieFileSize;
    private String movieImagepath;
    private long sceneryFileSize;
    private String movieRouteinfoPath;
    private long mapFileSize;
    //Speeds are in Kmh
    private Integer recordedSpeed;
    private Integer minimalSpeed;
    private Integer recordedFps;

    private String movieFlagUrl;

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

    public String getMovieDashUrl() {
        return movieDashUrl;
    }

    public void setMovieDashUrl(String movieDashUrl) {
        this.movieDashUrl = movieDashUrl;
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

    public String getMovieFlagUrl() {
        return movieFlagUrl;
    }

    public void setMovieFlagUrl(String movieFlagUrl) {
        this.movieFlagUrl = movieFlagUrl;
    }

    public static Movie fromRoutefilm(final Routefilm routefilm) {
        Movie movieDto = new Movie();
        movieDto.setId(routefilm.getMovieId());
        movieDto.setMinimalSpeed(routefilm.getMinimalSpeed());
        movieDto.setMovieImagepath(routefilm.getMovieImagepath());
        movieDto.setMovieLength(routefilm.getMovieLength());
        movieDto.setMovieImagepath(routefilm.getMovieImagepath());
        movieDto.setMovieRouteinfoPath(routefilm.getMovieRouteinfoPath());
        movieDto.setMovieTitle(routefilm.getMovieTitle());
        movieDto.setMovieUrl(routefilm.getMovieUrl());
        movieDto.setRecordedFps(routefilm.getRecordedFps());
        movieDto.setRecordedSpeed(routefilm.getRecordedSpeed());
        movieDto.setMovieFileSize(routefilm.getMovieFileSize());
        movieDto.setMapFileSize(routefilm.getMapFileSize());
        movieDto.setSceneryFileSize(routefilm.getSceneryFileSize());
        movieDto.setMovieFlagUrl(routefilm.getMovieFlagUrl());
        movieDto.setMovieDashUrl(routefilm.getMovieDashUrl());
        return movieDto;
    }
}
