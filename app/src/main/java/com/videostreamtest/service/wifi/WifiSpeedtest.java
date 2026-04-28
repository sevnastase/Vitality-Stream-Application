package com.videostreamtest.service.wifi;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WifiSpeedtest {
    private static final String TAG = WifiSpeedtest.class.getSimpleName();
    public static final long DEFAULT_NETWORK_VALUE = 0L;
    public static final long ERROR_NETWORK_VALUE = -1L;

    public static void getDownloadSpeedMbps(WifiCallback callback) {
        new Thread(() -> {
            try {
                InputStream is = new URL("https://media.praxcloud.eu/conn_test/10mb.zip\n").openStream();
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
                Log.d(TAG, e.toString());
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }).start();
    }

    public static void getPingTo(final String url, WifiCallback callback) {
        new Thread(() -> {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(PRAXCLOUD_API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            PraxCloud praxCloud = retrofit.create(PraxCloud.class);

            final long TIMEOUT_MS = 1500;
            AtomicBoolean handled = new AtomicBoolean(false);
            Handler timeoutHandler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                if (handled.compareAndSet(false, true)) {
                    callback.onError(new IOException("Could not ping " + url));
                }
            };
            timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

            try {
                long startTime = System.currentTimeMillis();
                boolean reachable = praxCloud.getServerStatus().execute().code() == 200;
                long endTime = System.currentTimeMillis();

                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (!handled.compareAndSet(false, true)) return;

                if (reachable) {
                    long latency = endTime - startTime;
                    callback.onSuccess(latency);
                } else {
                    callback.onError(new IOException("Could not ping " + url));
                }
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, e.toString());
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (handled.compareAndSet(false, true)) {
                    callback.onError(e);
                }
            }
        }).start();
    }
}
