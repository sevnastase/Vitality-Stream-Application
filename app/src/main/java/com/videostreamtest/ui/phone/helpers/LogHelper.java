package com.videostreamtest.ui.phone.helpers;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.videostreamtest.workers.WriteLogServiceWorker;

public class LogHelper {
    private static final String TAG = ProductHelper.class.getSimpleName();

    public static void WriteLogRule(final Context applicationContext, final String apikey, final String logRule, final String logType, final String profileName){
        //CONSTRAINTS
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        //BUILD DATA
        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);
        networkData.putString("logrule", logRule);
        networkData.putString("logtype", logType);
        networkData.putString("profilename", profileName);

        //CREATE ONETIME WORKREQUEST
        OneTimeWorkRequest writeLogRequest = new OneTimeWorkRequest.Builder(WriteLogServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(networkData.build())
                .addTag("write-log")
                .build();

        //START REQUEST
        WorkManager
                .getInstance(applicationContext)
                .enqueue(writeLogRequest);
    }
}
