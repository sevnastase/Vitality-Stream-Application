package com.videostreamtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;
import com.videostreamtest.utils.ApplicationSettings;

import java.util.ArrayList;

public class CadenceSensorBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = CadenceSensorBroadcastReceiver.class.getSimpleName();
    private static long lastUpdateTime = 0L; // static so it persists across onReceive calls
    private static final long MIN_UPDATE_INTERVAL_MS_DEFAULT = 1000;
    private static final long MIN_UPDATE_INTERVAL_MS_GIADA = 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        long now = System.currentTimeMillis();
        long timePassed = now - lastUpdateTime;
        // Giada computers that we use are weaker, so they need more time to process one request (only BLE)
        if (!AccountHelper.isChinesportAccount(PraxtourApplication.getAppContext())) {
            if (Build.BRAND.toLowerCase().contains("giada") && timePassed < MIN_UPDATE_INTERVAL_MS_GIADA) {
                return;
            } else if (timePassed < MIN_UPDATE_INTERVAL_MS_DEFAULT) {
                return;
            }
        }
        lastUpdateTime = now;

        String action = intent.getAction();
        if (action == null) return;

        int rpmReceived;
        switch (action) {
            case ApplicationSettings.COMMUNICATION_INTENT_FILTER:
                rpmReceived = intent.getIntExtra(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, 0);
                Log.d(TAG, "Cadence update: " + rpmReceived);
                break;
            case "com.videostreamtest.MQTT_DATA_UPDATE":
                ArrayList<String> motoLifeData = intent.getStringArrayListExtra("motoLifeData");
                if (motoLifeData == null) return;
                rpmReceived = Integer.parseInt(motoLifeData.get(0));
                break;
            default:
                return;
        }

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
