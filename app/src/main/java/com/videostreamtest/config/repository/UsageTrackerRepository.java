package com.videostreamtest.config.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.tracker.UsageTrackerDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.tracker.UsageTracker;

public class UsageTrackerRepository {
    private UsageTrackerDao usageTrackerDao;
    private String accounttoken;

    public UsageTrackerRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        usageTrackerDao = praxtourDatabase.usageTrackerDao();
        SharedPreferences sharedPreferences = application.getSharedPreferences("app", Context.MODE_PRIVATE);
        accounttoken = sharedPreferences.getString("apikey", "");
    }

    public LiveData<Integer> getSelectedProduct() {
        Log.d(getClass().getSimpleName(), "repo accountoken: "+accounttoken);
        return usageTrackerDao.getSelectedProduct(accounttoken);
    }

    public void setSelectedProduct(final Integer productId) {
        PraxtourDatabase.databaseWriterExecutor.execute(()->{
            usageTrackerDao.setSelectedProduct(accounttoken, productId);
        });
    }

    public LiveData<Integer> getSelectedMovie() {
        return usageTrackerDao.getSelectedMovie(accounttoken);
    }

    public void setSelectedMovie(final Integer movieId) {
        PraxtourDatabase.databaseWriterExecutor.execute(()->{
            usageTrackerDao.setSelectedMovie(accounttoken, movieId);
        });
    }

    public LiveData<Integer> getSelectedBackgroundSound() {
        return usageTrackerDao.getSelectedBackgroundSound(accounttoken);
    }

    public void setSelectedBackgroundSound(final Integer backgroundSoundId) {
        PraxtourDatabase.databaseWriterExecutor.execute(()->{
            usageTrackerDao.setSelectedBackgroundSound(accounttoken, backgroundSoundId);
        });
    }

    public LiveData<Integer> getSelectedProfile() {
        return usageTrackerDao.getSelectedProfile(accounttoken);
    }

    public void setSelectedProfile(final Integer profileId) {
        PraxtourDatabase.databaseWriterExecutor.execute(()->{
            usageTrackerDao.setSelectedProfile(accounttoken, profileId);
        });
    }

    public void insertNewUsageTrackerInformationObject(final UsageTracker usageTracker) {
        PraxtourDatabase.databaseWriterExecutor.execute(()->{
            usageTrackerDao.insert(usageTracker);
        });
    }

    public LiveData<UsageTracker> getUsageTrackers(final String accounttoken) {
        return usageTrackerDao.getCurrentUsageTrackingInformation(accounttoken);
    }
}
