package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.DownloadStatusDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;

import java.util.List;

public class DownloadStatusRepository {
    private DownloadStatusDao downloadStatusDao;

    public DownloadStatusRepository(Application application) {
        downloadStatusDao = PraxtourDatabase.getDatabase(application).downloadStatusDao();
    }

    public LiveData<StandAloneDownloadStatus> getDownloadStatus(final Integer movieId) {
        return downloadStatusDao.getDownloadStatus(movieId);
    }

    public LiveData<List<StandAloneDownloadStatus>> getAllDownloadStatus() {
        return downloadStatusDao.getAllDownloadStatus();
    }

    public LiveData<List<StandAloneDownloadStatus>> getAllActiveDownloadStatus() {
        return downloadStatusDao.getAllActiveDownloadStatus();
    }

}
