package com.videostreamtest.workers.logging;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.service.database.DatabaseRestService;

public class WriteLogServiceWorker extends Worker {

    public WriteLogServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Get Input from InputData
        final String apikey = getInputData().getString("apikey");
        final String logrule = getInputData().getString("logrule");
        final String logtype = getInputData().getString("logtype");
        final String profilename = getInputData().getString("profilename");
        //Pre-define output
        Data output = new Data.Builder().build();
        //Define which services you need
        final DatabaseRestService databaseRestService = new DatabaseRestService();
        //Execute some actions
        final String result = databaseRestService.writeLog(apikey, logrule, logtype, profilename);
        //Store outcome in the output data model
        output = new Data.Builder()
                .putString("log-result", result)
                .build();
        //Return result with data output
        return ListenableWorker.Result.success(output);
    }
}
