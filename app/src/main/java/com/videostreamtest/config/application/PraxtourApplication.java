package com.videostreamtest.config.application;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

public class PraxtourApplication extends Application implements Configuration.Provider {

    public static Activity currentActivity = null;

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build();
    }
}
