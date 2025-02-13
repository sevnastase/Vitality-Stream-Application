package com.videostreamtest.service.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.videostreamtest.config.application.PraxtourApplication;

import java.util.ArrayList;
import java.util.List;

public class WifiService extends Service {

    private static final String TAG = WifiService.class.getSimpleName();
    private List<ScanResult> availableNetworks;
    private BroadcastReceiver networksReceiver;
    private BroadcastReceiver localBroadcastReceiver;
    private android.net.wifi.WifiManager wifiManager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WifiService started");

        wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        networksReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleWifiScanResults(intent);
            }
        };

        IntentFilter wifiFilter = new IntentFilter(android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(networksReceiver, wifiFilter);

        localBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleLocalBroadcastReceived(intent);
            }
        };

        IntentFilter connectorFilter = new IntentFilter();
        connectorFilter.addAction("com.videostreamtest.wifi.ACTION_CONNECT");
        connectorFilter.addAction("com.videostreamtest.wifi.ACTION_SCAN");
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(localBroadcastReceiver, connectorFilter);

        new Handler().postDelayed(this::retrieveNetworks, 500);

        return START_STICKY;
    }

    private void handleLocalBroadcastReceived(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.d(TAG, "Intent or getAction() was null");
            return;
        }
        switch (action) {
            case "com.videostreamtest.wifi.ACTION_CONNECT":
                boolean success = connectToNetwork(intent);
                Intent broadcastIntent = new Intent("com.videostreamtest.EVENT_CONNECTION_RESULT");
                broadcastIntent.putExtra("connectionSuccessful", success);
                LocalBroadcastManager.getInstance(PraxtourApplication.getAppContext()).sendBroadcast(broadcastIntent);
                break;
            case "com.videostreamtest.wifi.ACTION_SCAN":
                retrieveNetworks();
        }
    }

    /**
     * Gets all nearby networks that the device sees.
     *
     * @modifies {@code this#availableNetworks}. Either changes it to the available networks
     * at the time of calling, or leaves in the previous state if there was a problem
     * while scanning.
     */
    private void retrieveNetworks() {
        Log.d(TAG, "Starting wifi scan..");
        boolean success = wifiManager.startScan();
        if (success) {
            Log.d(TAG, "Start of wifi scan successful!");
            availableNetworks = wifiManager.getScanResults();
        }
    }

    private void handleWifiScanResults(Intent intent) {
        String action = intent.getAction();
        if (android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            availableNetworks = wifiManager.getScanResults();
            broadcastNetworks();
        }
    }

    private void broadcastNetworks() {
        Intent intent = new Intent("com.videostreamtest.wifi.ACTION_SHOW_NETWORKS");
        // Needs to be done explicitly so intent.putExtra() knows that
        // the list contains objects that implement {@link Parcelable}
        ArrayList<ScanResult> networks = new ArrayList<>(availableNetworks);
        intent.putExtra("networkNames", networks);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private boolean connectToNetwork(Intent intent) {
        ScanResult network = intent.getParcelableExtra("theNetwork");
        String password = intent.getStringExtra("thePassword");
        if (network == null || password == null) {
            throw new NullPointerException("Network or password was null");
        }
        return WifiManager.connect(network, password);
    }
}
