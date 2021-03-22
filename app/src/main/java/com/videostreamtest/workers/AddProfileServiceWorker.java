package com.videostreamtest.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.service.database.DatabaseRestService;

public class AddProfileServiceWorker extends Worker {

    public AddProfileServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");
        final String profilename = getInputData().getString("profilename");
        final String imgpath = getInputData().getString("imgpath");
        //Pre-define output
        Data output = new Data.Builder().build();
        //Define which services you need
        final DatabaseRestService databaseRestService = new DatabaseRestService();
        //Execute some actions
        final String result = databaseRestService.addProfile(apikey, profilename, imgpath);
        //Store outcome in the output data model
        output = new Data.Builder()
                .putString("add-profile-result", result)
                .build();
        //Return result with data output
        return ListenableWorker.Result.success(output);
    }
}
