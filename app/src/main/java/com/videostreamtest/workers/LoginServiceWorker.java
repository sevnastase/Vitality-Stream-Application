package com.videostreamtest.workers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.videostreamtest.constants.SharedPreferencesConstants;
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
        Data output;
        //Define which services you need
        final DatabaseRestService databaseRestService = new DatabaseRestService();
        //Execute some actions
        String apikey = databaseRestService.authenticateUser(username, password);
        if (apikey == null) {
            output = new Data.Builder()
                    .putString("cause", "credentials")
                    .build();
            return Result.failure(output);
        }

        boolean result = databaseRestService.authenticateDevice("", apikey);
        if (!result) {
            output = new Data.Builder()
                    .putString("cause", "device")
                    .build();
            return Result.failure(output);
        }

        //Store outcome in the output data model
        output = new Data.Builder()
                .putString("apikey", apikey)
                .putString("password", password)
                .build();
        //Return result with data output
        return Result.success(output);
    }
}
