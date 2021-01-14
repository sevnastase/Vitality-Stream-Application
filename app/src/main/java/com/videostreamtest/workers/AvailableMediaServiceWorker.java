package com.videostreamtest.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.service.database.DatabaseRestService;

public class AvailableMediaServiceWorker extends Worker {

    private static final String TAG = AvailableMediaServiceWorker.class.getSimpleName();

    public AvailableMediaServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");
        //Pre-define output
        Data output = new Data.Builder().build();
        //Define which services you need
        final DatabaseRestService databaseRestService = new DatabaseRestService();
        //Execute some actions
        final String result = databaseRestService.getAvailableMovies(apikey);
        //Store outcome in the output data model
        output = new Data.Builder()
                .putString("movie-list", result)
                .build();
        //Return result with data output
        return ListenableWorker.Result.success(output);
    }
}
