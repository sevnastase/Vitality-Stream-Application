package com.videostreamtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

public class CadenceSensorBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = CadenceSensorBroadcastReceiver.class.getSimpleName();
    private static long lastUpdateTime = 0L; // static so it persists across onReceive calls
    private static final long MIN_UPDATE_INTERVAL_MS_DEFAULT = 1000;
    private static final long MIN_UPDATE_INTERVAL_MS_GIADA = 1500;

    @Override
    public void onReceive(Context context, Intent intent) {
        long now = System.currentTimeMillis();
        long timePassed = now - lastUpdateTime;
        // Giada computers that we use are weaker, so they need more time to process one request
        if (Build.BRAND.toLowerCase().contains("giada") && timePassed < MIN_UPDATE_INTERVAL_MS_GIADA) {
            return;
        } else if (timePassed < MIN_UPDATE_INTERVAL_MS_DEFAULT) {
            return;
        }
        lastUpdateTime = now;

        int rpmReceived = intent.getIntExtra(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, 0);

        Log.d(TAG, "Cadence update: " + rpmReceived);

        // Limit RPM value: too fast cycling can make the videoplayer lag
        if (rpmReceived > 100) rpmReceived = 100;

        VideoplayerExoActivity exoInstance = VideoplayerExoActivity.getInstance();
        if (exoInstance != null) {
            exoInstance.updateVideoPlayerScreen(rpmReceived);
            exoInstance.updateVideoPlayerParams(rpmReceived);
        }

        VideoplayerActivity legacyInstance = VideoplayerActivity.getInstance();
        if (legacyInstance != null) {
            legacyInstance.updateVideoPlayerScreen(rpmReceived);
            legacyInstance.updateVideoPlayerParams(rpmReceived);
        }
    }
}
