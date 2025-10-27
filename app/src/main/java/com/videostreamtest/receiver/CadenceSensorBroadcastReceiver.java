package com.videostreamtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

public class CadenceSensorBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = CadenceSensorBroadcastReceiver.class.getSimpleName();
    private static long lastUpdateTime = 0L; // static so it persists across onReceive calls
    private static final long MIN_UPDATE_INTERVAL_MS = 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < MIN_UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateTime = now;

        int rpmReceived = intent.getIntExtra(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, 0);

        Log.d(TAG, "Cadence update: " + rpmReceived);

        // Clamp rpm values
        if (rpmReceived > 0 && rpmReceived < 50) rpmReceived = 50;
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
