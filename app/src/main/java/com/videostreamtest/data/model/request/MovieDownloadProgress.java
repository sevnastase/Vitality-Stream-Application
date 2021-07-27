package com.videostreamtest.data.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDownloadProgress {
    private int movieId;
    private int roundedDownloadProgress;
    private String accountToken;

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getRoundedDownloadProgress() {
        return roundedDownloadProgress;
    }

    public void setRoundedDownloadProgress(int roundedDownloadProgress) {
        this.roundedDownloadProgress = roundedDownloadProgress;
    }

    public String getAccountToken() {
        return accountToken;
    }

    public void setAccountToken(String accountToken) {
        this.accountToken = accountToken;
    }
}
