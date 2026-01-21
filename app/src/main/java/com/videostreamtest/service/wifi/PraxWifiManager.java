package com.videostreamtest.service.wifi;

import static com.videostreamtest.constants.SharedPreferencesConstants.NO_WIFI_PERMISSION;
import static com.videostreamtest.constants.SharedPreferencesConstants.WIFI_NOT_CONNECTED;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.constants.BroadcastConstants;

import java.util.ArrayList;
import java.util.List;

public class PraxWifiManager {

    private static final String TAG = PraxWifiManager.class.getSimpleName();

    /**
     * From Android 10 and above the return value of this method does not correspond to successful
     * connection to a network. True means that the suggestions has been successfully made to the
     * system and will be processed. The actual connectivity result will come back later, asynchronously.
     * False means that the suggestions was rejected.
     * In the case of Android 10+, this method also sends a broadcast about connection status (once available).
     * <p>
     * Below Android 10, the return value corresponds to success of network connectivity.
     */
    public static boolean connect(ScanResult network, String password) {
        Log.d(TAG, "Connecting to " + network.SSID + "with password " + password);
        Context context = PraxtourApplication.getAppContext();

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Above Android 10
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                    .setSsid(network.SSID)
                    .setWpa2Passphrase(password)
                    .setIsAppInteractionRequired(true)
                    .build();

            List<WifiNetworkSuggestion> suggestions = new ArrayList<>();
            suggestions.add(suggestion);

            int status = wifiManager.addNetworkSuggestions(suggestions);

            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Toast.makeText(context, "Attempting to connect..", Toast.LENGTH_SHORT).show();
            } else {
                return false;
            }

            IntentFilter filter = new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION.equals(intent.getAction())) {
                        Intent broadcastIntent = new Intent("com.videostreamtest.EVENT_CONNECTION_RESULT");
                        broadcastIntent.putExtra("connectionSuccessful", true);
                        LocalBroadcastManager.getInstance(PraxtourApplication.getAppContext()).sendBroadcast(broadcastIntent);
                    }
                }
            };
            context.registerReceiver(receiver, filter);
            return true;
        } else {
            // Below Android 10
            // IMPORTANT: disconnect here manually. Android 9 does not allow programmatically
            // forgetting networks, so we only have a brief window to disconnect and then connect
            // to another network.
            wifiManager.disconnect();
            Log.d(TAG, "Disconnected current network");

            // Build up credentials of network to connect to
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + network.SSID + "\"";
            wifiConfig.preSharedKey = "\"" + password + "\"";
            int netId = wifiManager.addNetwork(wifiConfig);
            if (netId == -1) {
                Log.d(TAG, "Failed to add wifiConfig");
                return false;
            }

            boolean success = wifiManager.enableNetwork(netId, true);
            if (success) {
                wifiManager.reconnect();
                return true;
            } else {
                Log.d(TAG, "Failed to connect");
                return false;
            }
        }
    }

    /**
     * Returns {@code NO_WIFI_PERMISSION} if user has not granted WiFi permissions.
     * If permissions are granted and there is no connected network, this returns {@code WIFI_NOT_CONNECTED}.
     * If permissions are granted and the device is connected to a network, this returns its name (ssid).
     */
    public static String getConnectedNetworkName(Context context) {
        if (!requiredPermissionsGranted(context)) {
            return NO_WIFI_PERMISSION;
        }

        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                // Remove quotes from SSID if present
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                if (!ssid.equals("<unknown ssid>")) { // happens sometimes, better to leave it here
                    return ssid;
                }
            }
        }

        return WIFI_NOT_CONNECTED;
    }

    public static WifiStrength getConnectedNetworkStrength(Context context) {
        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return getSignalStrengthFromLevel(wifiInfo.getRssi());
    }

    public static WifiStrength getSignalStrengthFromLevel(int level) {
        // see online for ranges
        if (level >= -50) {
            return WifiStrength.EXCELLENT;
        } else if (isBetweenIncluding(level, -60, -51)) {
            return WifiStrength.GOOD;
        } else if (isBetweenIncluding(level, -70, -61)) {
            return WifiStrength.FAIR;
        } else if (isBetweenIncluding(level, -90, -71)) {
            return WifiStrength.POOR;
        } else {
            return WifiStrength.UNUSABLE;
        }
    }

    private static boolean isBetweenIncluding(int value, int lowerBound, int upperBound) {
        return lowerBound <= value && value <= upperBound;
    }

    private static boolean requiredPermissionsGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) ==
                    PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void disconnectFromCurrentNetwork(Activity activity) {
        Context context = activity.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                Network currentNetwork = connectivityManager.getActiveNetwork();
                connectivityManager.unregisterNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        Toast.makeText(context, "Disconnected from network", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent("com.videostreamtest.wifi.ACTION_SHOW_NETWORKS");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                });
            }
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                    wifiManager.disconnect();
                    Log.d(TAG, "Disconnected from " + wifiInfo.getSSID());
                    Log.d(TAG, "Forgot?" + wifiManager.removeNetwork(wifiInfo.getNetworkId()));
                }
            }
        }
    }
}
