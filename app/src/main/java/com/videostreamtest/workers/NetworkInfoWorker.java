package com.videostreamtest.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkInfoWorker extends Worker {

    private static final String TAG = NetworkInfoWorker.class.getSimpleName();

    public NetworkInfoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        final String ipAddress = getInputData().getString("ipAddress");
        Data output = new Data.Builder().build();
        try {
            InetAddress geek = InetAddress.getByName(ipAddress);
            Log.d(TAG, "Sending Ping Request to " + ipAddress);
            if (geek.isReachable(100)) {
                Log.d(TAG, "Host is easily reachable");
                output = new Data.Builder()
                        .putString("ping_result", "Less or equal to 100 ms")
                        .putString("ping_value", "100")
                        .build();
            } else if (geek.isReachable(700)) {
                Log.d(TAG, "Host is slowly reachable");
                output = new Data.Builder()
                        .putString("ping_result", "Less or equal to 1000 ms")
                        .putString("ping_value", "700")
                        .build();
            }
            else {
                Log.d(TAG, "Sorry ! We can't reach to this host");
                output = new Data.Builder()
                        .putString("ping_result", "Longer than 1000 ms (very slow)")
                        .putString("ping_value", "1000+")
                        .build();
            }
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        return Result.success(output);
    }
}
