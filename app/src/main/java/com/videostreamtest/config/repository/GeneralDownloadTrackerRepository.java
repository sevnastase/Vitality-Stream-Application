package com.videostreamtest.config.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.tracker.GeneralDownloadTrackerDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.tracker.GeneralDownloadTracker;

public class GeneralDownloadTrackerRepository {
    private GeneralDownloadTrackerDao generalDownloadTracker;
    private String accounttoken;

    public GeneralDownloadTrackerRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        generalDownloadTracker = praxtourDatabase.generalDownloadTrackerDao();
        SharedPreferences sharedPreferences = application.getSharedPreferences("app", Context.MODE_PRIVATE);
        accounttoken = sharedPreferences.getString("apikey", "");
    }

    public LiveData<GeneralDownloadTracker> getSelectedDownloadTypeInformation(final String downloadType) {
        return generalDownloadTracker.getCurrentDownloadTrackingInformation(downloadType);
    }
}
