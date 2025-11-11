package com.videostreamtest.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.videostreamtest.service.database.DatabaseRestService;

public class LoginServiceWorker extends AbstractPraxtourWorker {
    private static final String TAG = LoginServiceWorker.class.getSimpleName();

    public LoginServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    protected Result doActualWork() {
        //Get Input
        final String username = getInputData().getString("username");
        final String password = getInputData().getString("password");
        //Pre-define output
        Data output = new Data.Builder().build();
        //Define which services you need
        final DatabaseRestService databaseRestService = new DatabaseRestService();
        //Execute some actions
        final String result = databaseRestService.loginUser(username, password);
        //Store outcome in the output data model
        output = new Data.Builder()
                .putString("apikey", result)
                .putString("password", password)
                .build();
        //Return result with data output
        return Result.success(output);
    }
}
