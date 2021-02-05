package com.videostreamtest.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MoviePart {
    private Integer id;
    private Integer movieId;
    private Integer frameNumber;
    private String moviepartName;
    private String moviepartImagepath;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Integer getFrameNumber() {
        return frameNumber;
    }

    public void setFrameNumber(Integer frameNumber) {
        this.frameNumber = frameNumber;
    }

    public String getMoviepartName() {
        return moviepartName;
    }

    public void setMoviepartName(String moviepartName) {
        this.moviepartName = moviepartName;
    }

    public String getMoviepartImagepath() {
        return moviepartImagepath;
    }

    public void setMoviepartImagepath(String moviepartImagepath) {
        this.moviepartImagepath = moviepartImagepath;
    }
}
