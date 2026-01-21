package com.videostreamtest.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.videostreamtest.service.wifi.PraxWifiManager;

public class NetworkHelper {
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
}
