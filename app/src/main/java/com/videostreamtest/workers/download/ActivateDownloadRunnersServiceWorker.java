package com.videostreamtest.workers.download;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.LocalMoviesDownloadTable;
import com.videostreamtest.workers.AbstractPraxtourWorker;

import java.util.ArrayList;
import java.util.List;

public class ActivateDownloadRunnersServiceWorker extends AbstractPraxtourWorker {

    public ActivateDownloadRunnersServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    protected Result doActualWork() {
        String localServerUrl = getInputData().getString("localMediaServer");
        String apikey = getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey", "");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data.Builder mediaDownloadInputData = new Data.Builder();
        mediaDownloadInputData.putString("localMediaServer", localServerUrl);
        mediaDownloadInputData.putString("apikey", apikey);

        List<OneTimeWorkRequest> downloadMovieRunners = new ArrayList<>();
        List<LocalMoviesDownloadTable> retrievedCurrentPendingDownloads = retrieveCurrentPendingDownloads();

        if (retrievedCurrentPendingDownloads!= null && retrievedCurrentPendingDownloads.size()>0) {
            Log.d(getClass().getSimpleName(), "Number of pending downloads: " + retrievedCurrentPendingDownloads.size());
            for (final LocalMoviesDownloadTable downloadStatus:retrievedCurrentPendingDownloads) {

                mediaDownloadInputData.putInt("movie-id", downloadStatus.getMovieId());

                OneTimeWorkRequest downloadRunner = new OneTimeWorkRequest.Builder(DownloadMovieServiceWorker.class)
                        .setConstraints(constraints)
                        .setInputData(mediaDownloadInputData.build())
                        .addTag("download-runner-request-movie-id-"+downloadStatus.getMovieId())
                        .build();
                downloadMovieRunners.add(downloadRunner);
                WorkManager.getInstance(getApplicationContext())
                        .beginUniqueWork("download-runner-cluster-"+downloadStatus.getMovieId(), ExistingWorkPolicy.KEEP, downloadRunner)
                        .enqueue();
            }
        }

        //deprecated
//        List<OneTimeWorkRequest> downloadRunners = new ArrayList<>();
//
//        for (int downloadRunnerIndex = 0; downloadRunnerIndex < NUMBER_OF_DOWNLOAD_RUNNERS; downloadRunnerIndex++) {
//            OneTimeWorkRequest downloadRunner = new OneTimeWorkRequest.Builder(DownloadMovieServiceWorker.class)
//                    .setConstraints(constraints)
//                    .setInputData(mediaDownloadInputData.build())
//                    .addTag("download-runner-request-id-"+downloadRunnerIndex)
//                    .build();
//            downloadRunners.add(downloadRunner);
//
//        }
//        WorkManager.getInstance(getApplicationContext())
//                .beginUniqueWork("download-runner-cluster-"+0, ExistingWorkPolicy.APPEND, downloadMovieRunners)
//                .enqueue();

//        LogHelper.WriteLogRule(getApplicationContext(), apikey, "Periodic DownloadRunners initiated.","DEBUG", "");

        return Result.success();
    }

    private List<LocalMoviesDownloadTable> retrieveCurrentPendingDownloads() {
        List<LocalMoviesDownloadTable> pendingDownloads = PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().getPendingDownloadStatus();
        if (pendingDownloads != null && pendingDownloads.size()>0) {
            return pendingDownloads;
        }
        return new ArrayList<>();
    }


}
