package com.videostreamtest.helpers;

import static com.videostreamtest.constants.PraxConstants.NetworkConstants.MAX_PING_TO_API;
import static com.videostreamtest.constants.PraxConstants.NetworkConstants.MIN_DOWNLOAD_SPEED_KBPS;
import static com.videostreamtest.service.wifi.WifiSpeedtest.DEFAULT_NETWORK_VALUE;
import static com.videostreamtest.service.wifi.WifiSpeedtest.ERROR_NETWORK_VALUE;
import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.util.Log;

import com.videostreamtest.service.wifi.PraxWifiManager;
import com.videostreamtest.service.wifi.WifiSpeedtest;

import java.util.concurrent.atomic.AtomicInteger;

public class NetworkHelper {
    private static final String TAG = NetworkHelper.class.getSimpleName();
    public static boolean isNetworkPraxtourLAN(Context context) {
        return "PraxAP".equals(PraxWifiManager.getConnectedNetworkName(context));
    }

    public static boolean isInternetReachable(Context context) {
        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        Network currentNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);

        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * @param handler {@link Handler} that runs this runnable
     * @param callback implementation of the {@link PraxCallbacks.SpeedtestCallback} interface that will
     *                          be called on results
     */
    public static Runnable getSpeedtestRunnable(final Handler handler, final PraxCallbacks.SpeedtestCallback callback) {
        final Runnable[] result =  new Runnable[1]; // needed to reference self in run()

        result[0] = new Runnable() {
            /**
             * A fault is either a call to onError, or a measurement lower than MIN_NETWORK_SPEED_KBPS.
             * When {@code faults} passes this value, the network info (speed, ping) will be shown.
             */
            final long MIN_FAULTS = 3;
            /**
             * A fault is either a call to onError, or a measurement lower than MIN_NETWORK_SPEED_KBPS.
             * When {@code faults} passes this value, the network error text will be shown.
             */
            final long MAX_FAULTS = 10;
            final AtomicInteger faults = new AtomicInteger(0);
            long currentPing = DEFAULT_NETWORK_VALUE;
            long currentDownloadSpeedKbps = DEFAULT_NETWORK_VALUE;

            @Override
            public void run() {
                Log.d(TAG, "Greg running network, #faults: " + faults);
                WifiSpeedtest.getPingTo(PRAXCLOUD_API_URL, new PraxCallbacks.WifiCallback() {
                    @Override
                    public void onSuccess(long value) {
                        currentPing = value;

                        if (value == ERROR_NETWORK_VALUE || value > MAX_PING_TO_API) {
                            faults.addAndGet(1);
                        }

                        Log.d(TAG, "\tGreg ping " + currentPing);

                        WifiSpeedtest.getDownloadSpeedMbps(new PraxCallbacks.WifiCallback() {
                            @Override
                            public void onSuccess(long value) {
                                currentDownloadSpeedKbps = value;

                                Log.d(TAG, "\tGreg speed " + currentDownloadSpeedKbps);

                                if (value < MIN_DOWNLOAD_SPEED_KBPS) {
                                    faults.addAndGet(1);
                                }

                                if (faults.get() >= MAX_FAULTS) {
                                    callback.onFailure(currentPing, currentDownloadSpeedKbps);
                                    return;
                                }

                                if (faults.get() >= MIN_FAULTS) {
                                    callback.onWarning(currentPing, currentDownloadSpeedKbps);
                                } else {
                                    callback.onSuccess(currentPing, currentDownloadSpeedKbps);
                                }

                                handler.postDelayed(result[0], 1000);
                            }

                            @Override
                            public void onError(Exception e) {
                                currentDownloadSpeedKbps = ERROR_NETWORK_VALUE;

                                faults.addAndGet(1);

                                if (faults.get() >= MAX_FAULTS) {
                                    callback.onFailure(currentPing, currentDownloadSpeedKbps);
                                    return;
                                }

                                if (faults.get() >= MIN_FAULTS) {
                                    callback.onWarning(currentPing, currentDownloadSpeedKbps);
                                }

                                handler.postDelayed(result[0], 1000);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onFailure(currentPing, currentDownloadSpeedKbps);
                    }
                });
            }
        };

        return result[0];
    }
}
