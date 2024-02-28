package com.videostreamtest.ui.phone.videoplayer.viewmodel;

import static com.videostreamtest.service.database.DatabaseRestService.TAG;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.EffectSound;
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.config.repository.BackgroundSoundRepository;
import com.videostreamtest.config.repository.EffectSoundRepository;
import com.videostreamtest.config.repository.RoutepartRepository;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.utils.DistanceLookupTable;

import java.util.List;

public class VideoPlayerViewModel extends AndroidViewModel {
    //REPOSITORIES
    private RoutepartRepository         routepartRepository;
    private BackgroundSoundRepository   backgroundSoundRepository;
    private EffectSoundRepository       effectSoundRepository;

    //ELEMENTS
    private MutableLiveData <Integer>   rpmData                     = new MutableLiveData<>();
    private MutableLiveData <Integer>   kmhData                     = new MutableLiveData<>();
    private MutableLiveData <Integer>     volumeLevel                 = new MutableLiveData<>();
    private MutableLiveData <Movie>     selectedMovie               = new MutableLiveData<>();
    private MutableLiveData <Long>      movieTotalDurationSeconds   = new MutableLiveData<>();
    private MutableLiveData <Long>      movieSpendDurationSeconds   = new MutableLiveData<>();
    private MutableLiveData <Boolean>   statusbarVisible            = new MutableLiveData<>();
    private MutableLiveData <Boolean>   playerPaused                = new MutableLiveData<>();
    private MutableLiveData <Boolean>   resetChronometer            = new MutableLiveData<>();
    private final MutableLiveData <Integer> distanceOffset          = new MutableLiveData<>(0);
    private MutableLiveData <Integer>   currentMetersDone           = new MutableLiveData<>();
    private MutableLiveData <Integer>   metersToGo                  = new MutableLiveData<>();

    //START VALUES
    private Integer     startRpmValue           = 0;
    private Integer     startKmhValue           = 22;
    private Integer     startVolumeLevel        = 80;
    private Boolean     statusbarVisibility     = false;
    private Boolean     isPlayerPaused          = false;
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
        playerPaused.setValue(isPlayerPaused);

        //Update ViewModel when observables change
        this.selectedMovie.observeForever(movie -> distanceCalculationLogic());
        this.movieTotalDurationSeconds.observeForever(duration -> distanceCalculationLogic());
        this.movieSpendDurationSeconds.observeForever(duration -> distanceCalculationLogic());
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

    public MutableLiveData<Integer> getVolumeLevel() {
        return volumeLevel;
    }

    public void setVolumeLevel(Integer volumeLevel) {
        if(volumeLevel >= 0 && volumeLevel <= 100) {
            this.volumeLevel.setValue(volumeLevel);
        }
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

    public MutableLiveData<Boolean> getResetChronometer() {
        return resetChronometer;
    }

    public void setResetChronometer(boolean resetChronometer) {
        this.resetChronometer.setValue(resetChronometer);
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

    public MutableLiveData<Integer> getDistanceOffset() {
        return distanceOffset;
    }

    public void setDistanceOffset(int offset) {
        distanceOffset.setValue(offset);
    }

    public LiveData<Integer> getCurrentMetersDone() {
        return currentMetersDone;
    }

    public void setCurrentMetersDone(Integer meters) {
        this.currentMetersDone.setValue(meters);
    }

    public LiveData<Integer> getMetersToGo() {
        return metersToGo;
    }

    public void setMetersToGo(Integer meters) {
        this.metersToGo.setValue(meters);
    }

    private float getMps(Movie selectedMovie) {
        Long movieTotalDurationSecondsVal = movieTotalDurationSeconds.getValue();
        if (movieTotalDurationSecondsVal != null) {
            return DistanceLookupTable.getMeterPerSecond(selectedMovie.getMovieLength(), movieTotalDurationSecondsVal /1000);
        }
        return 0f;
    }

    public void distanceCalculationLogic() {
        Movie selectedMovieVal = selectedMovie.getValue();
        Long movieTotalDurationSecondsVal = movieTotalDurationSeconds.getValue();
        Long movieSpendDurationSecondsVal = movieSpendDurationSeconds.getValue();
        int distanceOffsetVal = distanceOffset.getValue();

        if (selectedMovieVal != null && movieTotalDurationSecondsVal != null && movieSpendDurationSecondsVal != null) {
            final float mps = DistanceLookupTable.getMeterPerSecond(selectedMovieVal.getMovieLength(), movieTotalDurationSecondsVal / 1000);
            int calculatedCurrentMetersDone = (int) (mps * (movieSpendDurationSecondsVal / 1000)) - distanceOffsetVal;
            if (calculatedCurrentMetersDone < 0) calculatedCurrentMetersDone = 0;
            final int calculatedMetersToGo = selectedMovieVal.getMovieLength() - calculatedCurrentMetersDone - distanceOffsetVal;

            Log.d(TAG, "calculatedCurrentMetersDone = " + calculatedCurrentMetersDone);
            Log.d(TAG, "distanceOffset = " + distanceOffsetVal);
            Log.d(TAG, "calculatedMetersToGo = " + calculatedMetersToGo);

            currentMetersDone.postValue(calculatedCurrentMetersDone);
            metersToGo.postValue(calculatedMetersToGo);
        }
    }

    public void resetDistance(MoviePart moviePart, Movie selectedMovie) {
        int seekBarPartFrameNumber = moviePart.getFrameNumber().intValue();
        int seekBarPartDurationSeconds = (1000 * seekBarPartFrameNumber) / selectedMovie.getRecordedFps().intValue();
        int newDistanceOffset = (int) (getMps(selectedMovie) * (seekBarPartDurationSeconds / 1000));

        setDistanceOffset(newDistanceOffset);
        distanceCalculationLogic();
    }
}
