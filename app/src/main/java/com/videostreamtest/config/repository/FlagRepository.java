package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.dao.FlagDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;

import java.util.List;

public class FlagRepository {
    private FlagDao flagDao;
    private MutableLiveData<Flag> selectedFlag;
    private LiveData<List<Flag>> allFlags;

    public FlagRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        flagDao = praxtourDatabase.flagDao();
    }

    public LiveData<Flag> getFlagOfMovie() {
        return flagDao.getFlagFromSelectedMovie();
    }

    public LiveData<MovieFlag> getMovieFlagOfMovie() {
        return flagDao.getMovieFlagFromSelectedMovie();
    }

    public LiveData<Flag> getSelectedFlag() {
        return selectedFlag;
    }

    public void setSelectedFlag(Flag selectedFlag) {
        this.selectedFlag.setValue(selectedFlag);
    }

    public LiveData<List<Flag>> getAllFlags() {
        return flagDao.getAllFlags();
    }
}
