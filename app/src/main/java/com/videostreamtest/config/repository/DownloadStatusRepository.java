package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.DownloadStatusDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.LocalMoviesDownloadTable;

import java.util.List;

public class DownloadStatusRepository {
    private DownloadStatusDao downloadStatusDao;

    public DownloadStatusRepository(Application application) {
        downloadStatusDao = PraxtourDatabase.getDatabase(application).downloadStatusDao();
    }

    public LiveData<LocalMoviesDownloadTable> getDownloadStatus(final Integer movieId) {
        return downloadStatusDao.getDownloadStatus(movieId);
    }

    public LiveData<List<LocalMoviesDownloadTable>> getAllDownloadStatus() {
        return downloadStatusDao.getAllDownloadStatus();
    }

    public LiveData<List<LocalMoviesDownloadTable>> getAllActiveDownloadStatus() {
        return downloadStatusDao.getAllActiveDownloadStatus();
    }

    public void resetInterruptedDownloads() {
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            downloadStatusDao.resetInterruptedDownloads();
        });
    }

}
