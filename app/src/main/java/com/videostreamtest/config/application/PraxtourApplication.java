package com.videostreamtest.config.application;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

public class PraxtourApplication extends Application implements Configuration.Provider {

    private static PraxtourApplication instance;
    private WifiManager.WifiLock wifiLock;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        acquireWifiLock();
    }

    public static PraxtourApplication getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build();
    }

    /**
     * Locks wifi: does not allow OS to shutdown wifi services.
     */
    private void acquireWifiLock() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PraxtourWifiLock");
        wifiLock.setReferenceCounted(false);
        wifiLock.acquire();
    }

    /**
     * Releases wifi lock: allows OS to shutdown wifi services.
     */
    public void releaseWifiLock() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }
}
