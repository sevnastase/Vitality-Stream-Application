package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.EffectSoundDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.EffectSound;

import java.util.List;

public class EffectSoundRepository {
    private EffectSoundDao effectSoundDao;

    public EffectSoundRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        effectSoundDao = praxtourDatabase.effectSoundDao();
    }

    public LiveData<List<EffectSound>> getEffectSounds(final Integer movieId) {
        return effectSoundDao.getEffectSounds(movieId);
    }
}
