package com.videostreamtest.ui.phone.log;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.videostreamtest.workers.WriteLogServiceWorker;

public class LogActivity extends AppCompatActivity {
    private static LogActivity thisInstance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisInstance = this;
    }

    public static LogActivity getInstance() {
        return thisInstance;
    }

    public void writeToLogRecords(final String apikey, final String logRule, final String logType, final String profileName) {
        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);
        networkData.putString("logrule", logRule);
        networkData.putString("logtype", logType);
        networkData.putString("profilename", profileName);

        OneTimeWorkRequest writeLogRequest = new OneTimeWorkRequest.Builder(WriteLogServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("write-log")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(writeLogRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(writeLogRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("log-result");
                        Log.d(this.getClass().getSimpleName(), "LOG RESULT :: "+result);
                    }
                });
    }
}
