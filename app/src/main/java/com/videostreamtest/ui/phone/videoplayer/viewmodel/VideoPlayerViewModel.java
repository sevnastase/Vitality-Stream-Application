package com.videostreamtest.ui.phone.videoplayer.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.EffectSound;
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.config.repository.BackgroundSoundRepository;
import com.videostreamtest.config.repository.EffectSoundRepository;
import com.videostreamtest.config.repository.RoutepartRepository;
import com.videostreamtest.data.model.Movie;

import java.util.List;

public class VideoPlayerViewModel extends AndroidViewModel {
    //REPOSITORIES
    private RoutepartRepository         routepartRepository;
    private BackgroundSoundRepository   backgroundSoundRepository;
    private EffectSoundRepository       effectSoundRepository;

    //ELEMENTS
    private MutableLiveData <Integer>   rpmData                     = new MutableLiveData<>();
    private MutableLiveData <Integer>   kmhData                     = new MutableLiveData<>();
    private MutableLiveData <Float>     volumeLevel                 = new MutableLiveData<>();
    private MutableLiveData <Movie>     selectedMovie               = new MutableLiveData<>();
    private MutableLiveData <Long>      movieTotalDurationSeconds   = new MutableLiveData<>();
    private MutableLiveData <Long>      movieSpendDurationSeconds   = new MutableLiveData<>();
    private MutableLiveData <Boolean>   statusbarVisible            = new MutableLiveData<>();
    private MutableLiveData <Boolean>   playerPaused                = new MutableLiveData<>();

    //START VALUES
    private Integer     startRpmValue           = 0;
    private Integer     startKmhValue           = 22;
    private Float       startVolumeLevel        = 0.8f;
    private Boolean     statusbarVisibility     = false;
    private Boolean     playerStatus            = false;
    private Long        totalDurationSeconds    = 0L;
    private Long        spendDurationSeconds    = 0L;

    public VideoPlayerViewModel(@NonNull Application application) {
        super(application);
        //INITIALIZE REPOSITORIES
        routepartRepository         = new RoutepartRepository(application);
        backgroundSoundRepository   = new BackgroundSoundRepository(application);
        effectSoundRepository       = new EffectSoundRepository(application);

        //Set Start values on creation of viewmodel
        rpmData.setValue(startRpmValue);
        kmhData.setValue(startKmhValue);
        volumeLevel.setValue(startVolumeLevel);
        statusbarVisible.setValue(statusbarVisibility);
        movieTotalDurationSeconds.setValue(totalDurationSeconds);
        movieSpendDurationSeconds.setValue(spendDurationSeconds);
        playerPaused.setValue(playerStatus);
    }

    public LiveData<Integer> getRpmData() {
        return rpmData;
    }

    public void setRpmData(final Integer rpmValue) {
        rpmData.setValue(rpmValue);
    }

    public MutableLiveData<Integer> getKmhData() {
        return kmhData;
    }

    public void setKmhData(Integer kmhData) {
        this.kmhData.setValue(kmhData);
    }

    public MutableLiveData<Float> getVolumeLevel() {
        return volumeLevel;
    }

    public void setVolumeLevel(Float volumeLevel) {
        this.volumeLevel.setValue(volumeLevel);
    }

    public LiveData<Movie> getSelectedMovie() {
        return selectedMovie;
    }

    public void setSelectedMovie(Movie selectedMovie) {
        this.selectedMovie.setValue(selectedMovie);
    }

    public MutableLiveData<Long> getMovieTotalDurationSeconds() {
        return movieTotalDurationSeconds;
    }

    public void setMovieTotalDurationSeconds(long movieTotalDurationSeconds) {
        this.movieTotalDurationSeconds.setValue(movieTotalDurationSeconds);
    }

    public MutableLiveData<Long> getMovieSpendDurationSeconds() {
        return movieSpendDurationSeconds;
    }

    public void setMovieSpendDurationSeconds(long movieSpendDurationSeconds) {
        this.movieSpendDurationSeconds.setValue(movieSpendDurationSeconds);
    }

    public MutableLiveData<Boolean> getStatusbarVisible() {
        return statusbarVisible;
    }

    public void setStatusbarVisible(boolean statusbarVisible) {
        this.statusbarVisible.setValue(statusbarVisible);
    }

    public MutableLiveData<Boolean> getPlayerPaused() {
        return playerPaused;
    }

    public void setPlayerPaused(boolean playerPaused) {
        this.playerPaused.setValue(playerPaused);
    }

    public LiveData<List<Routepart>> getRoutePartsOfMovieId(final Integer movieId){
        return routepartRepository.getRoutePartsOfMovieWithId(movieId);
    }

    public LiveData<List<BackgroundSound>> getBackgroundSounds(final Integer movieId) {
        return backgroundSoundRepository.getBackgroundSounds(movieId);
    }

    public LiveData<List<EffectSound>> getEffectSounds(final Integer movieId) {
        return effectSoundRepository.getEffectSounds(movieId);
    }
}
