package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.BackgroundSoundDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.BackgroundSound;

import java.util.List;

public class BackgroundSoundRepository {
    private BackgroundSoundDao backgroundSoundDao;

    public BackgroundSoundRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        backgroundSoundDao = praxtourDatabase.backgroundSoundDao();
    }

    public LiveData<List<BackgroundSound>> getBackgroundSounds(final Integer movieId) {
        return backgroundSoundDao.getBackgroundSounds(movieId);
    }

}
