package com.videostreamtest.config.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.dao.ProfileDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Profile;

import java.util.List;

public class ProfileRepository {
    private ProfileDao profileDao;
    private LiveData<List<Profile>> accountProfiles;

    public ProfileRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        profileDao = praxtourDatabase.profileDao();
        accountProfiles = profileDao.getAccountProfiles();
    }

    public LiveData<Profile> getProfile(final Integer profileId) {
        return profileDao.getProfile(profileId);
    }

    public LiveData<List<com.videostreamtest.config.entity.Profile>> getAccountProfiles() {
        return accountProfiles;
    }

    //Insert mapped database profile from response profile to local database
    public void insert(final com.videostreamtest.config.entity.Profile profile) {
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            profileDao.insert(profile);
        });
    }

    public void update(com.videostreamtest.config.entity.Profile updatedProfile) {
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            profileDao.update(updatedProfile);
        });
    }

}
