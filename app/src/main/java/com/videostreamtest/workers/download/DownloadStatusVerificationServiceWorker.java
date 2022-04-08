package com.videostreamtest.workers.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
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
        final List<StandAloneDownloadStatus> downloadStatusList = PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().getAllRawDownloadStatus();
        if (downloadStatusList != null && downloadStatusList.size()>0) {
            for (final StandAloneDownloadStatus downloadStatus: downloadStatusList) {
                if (downloadStatus.getDownloadStatus() == 100 &&
                        !DownloadHelper.isMoviePresent(getApplicationContext(), getRoutefilm(downloadStatus.getMovieId()))) {
                    downloadStatus.setDownloadStatus(-1);
//                    PraxtourDatabase.databaseWriterExecutor.execute(()->{
                        PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().insert(downloadStatus);
//                    });
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
