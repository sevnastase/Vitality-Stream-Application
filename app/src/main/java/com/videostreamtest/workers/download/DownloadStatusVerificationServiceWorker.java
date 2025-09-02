package com.videostreamtest.workers.download;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.LocalMoviesDownloadTable;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;

import java.util.List;

public class DownloadStatusVerificationServiceWorker extends Worker {

    public DownloadStatusVerificationServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final List<LocalMoviesDownloadTable> downloadStatusList = PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().getAllRawDownloadStatus();
        if (downloadStatusList != null && downloadStatusList.size()>0) {
            for (final LocalMoviesDownloadTable downloadStatus: downloadStatusList) {
                if (downloadStatus.getDownloadStatus() == 100 &&
                        !DownloadHelper.isMoviePresent(getApplicationContext(), getRoutefilm(downloadStatus.getMovieId().intValue()))) {
                    Log.d(getClass().getSimpleName(), String.format("Movie %s registered as downloaded but not present on disk.", getRoutefilm(downloadStatus.getMovieId().intValue()).getMovieTitle()));
                    downloadStatus.setDownloadStatus(-1);
                    PraxtourDatabase.databaseWriterExecutor.execute(()->{
                        PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().insert(downloadStatus);
                    });
                }
                if (downloadStatus.getDownloadStatus() < 0 &&
                    DownloadHelper.isMoviePresent(getApplicationContext(), getRoutefilm(downloadStatus.getMovieId().intValue()))) {
                    Log.d(getClass().getSimpleName(), String.format("Movie %s registered as NOT downloaded but is present on disk.", getRoutefilm(downloadStatus.getMovieId().intValue()).getMovieTitle()));
                    downloadStatus.setDownloadStatus(100);
                    PraxtourDatabase.databaseWriterExecutor.execute(()->{
                        PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().insert(downloadStatus);
                    });
                }
            }
        }
        return Result.success();
    }

    private Movie getRoutefilm(final Integer movieId) {
        final Routefilm routefilm = PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao().getRoutefilm(movieId);
        if (routefilm != null) {
            return Movie.fromRoutefilm(routefilm);
        } else {
            return null;
        }
    }
}
