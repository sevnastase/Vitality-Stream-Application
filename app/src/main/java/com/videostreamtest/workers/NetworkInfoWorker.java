package com.videostreamtest.workers;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_MEDIA_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import java.net.InetAddress;

public class NetworkInfoWorker extends AbstractPraxtourWorker {

    private static final String TAG = NetworkInfoWorker.class.getSimpleName();

    public NetworkInfoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    protected Result doActualWork() {
        Data output = new Data.Builder().build();
        if (isInternetAvailable()) {
            Log.d(TAG, "Internetconnection available.");
            output = new Data.Builder()
                    .putString("connection_result", "connected")
                    .build();
        } else {
            Log.e(TAG, "Internetconnection failed.");
            return Result.failure();
        }

        return Result.success(output);
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName(PRAXCLOUD_MEDIA_URL);
            boolean isActive = ipAddr.isReachable(200);
            //You can replace it with your name
            return isActive;
        } catch (Exception e) {
            return false;
        }
    }
}
