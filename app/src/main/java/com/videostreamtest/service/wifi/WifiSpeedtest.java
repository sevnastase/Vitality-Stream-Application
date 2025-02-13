package com.videostreamtest.service.wifi;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;

public class WifiSpeedtest {
    private static final String TAG = WifiSpeedtest.class.getSimpleName();

    public static void getDownloadSpeedMbps(WifiCallback callback) {
        new Thread(() -> {
            try {
                InputStream is = new URL("http://speedtest.tele2.net/10MB.zip\n").openStream();
                byte[] buf = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                long startTime = System.nanoTime();

                while ((bytesRead = is.read(buf)) != -1) {
                    totalBytesRead += bytesRead;
                    if (totalBytesRead >= 200 * 1024) { // stops after 200KB
                        break;
                    }
                }
                is.close();

                long endTime = System.nanoTime();
                long totalTime = endTime - startTime;

                long downloadSpeedMbps = (totalBytesRead * 8L * 1_000_000_000L) / (totalTime * 1024 * 1024);
                Log.d(TAG, "downloadspeed: " + downloadSpeedMbps);

                if (callback != null) {
                    callback.onSuccess(downloadSpeedMbps);
                }
            }
            catch(Exception e){
                e.printStackTrace();
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }).start();
    }

    public static void getPingTo(final String url, WifiCallback callback) {
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                boolean reachable = InetAddress.getByName(url).isReachable(1500);
                Log.d(TAG, url + " reachable? " + reachable);
                long endTime = System.currentTimeMillis();

                if (reachable) {
                    long latency = endTime - startTime;
                    Log.d(TAG, "ping: " + latency);
                    if (callback != null) {
                        callback.onSuccess(latency);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }).start();
    }
}
