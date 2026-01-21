package com.videostreamtest.ui.phone.productpicker.fragments.settings.wifi;

import static com.videostreamtest.constants.SharedPreferencesConstants.NO_WIFI_PERMISSION;
import static com.videostreamtest.constants.SharedPreferencesConstants.WIFI_NOT_CONNECTED;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.constants.BroadcastConstants;
import com.videostreamtest.service.wifi.WifiCallback;
import com.videostreamtest.service.wifi.PraxWifiManager;
import com.videostreamtest.service.wifi.WifiService;
import com.videostreamtest.service.wifi.WifiSpeedtest;
import com.videostreamtest.service.wifi.WifiStrength;
import com.videostreamtest.helpers.PermissionHelper;
import com.videostreamtest.helpers.ViewHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class WifiSettingsFragment extends Fragment {

    private static final String TAG = WifiSettingsFragment.class.getSimpleName();
    private static final int DEFAULT_REFRESH_INTERVAL_MILLIS = 3000;

    private RecyclerView networkRecyclerView;
    private BroadcastReceiver networkBroadcastReceiver;
    private ArrayList<ScanResult> availableNetworks;
    private WifiAdapter wifiAdapter;
    private Button refreshButton;
    private ConstraintLayout connectedNetworkLayout;
    private TextView connectedNetworkNameTextView;
    private ImageView connectedNetworkStrengthImageView;
    private TextView connectedNetworkDownloadSpeedTextView;
    private TextView connectedNetworkLatencyTextView;
    private TextView connectedTitleTextView;
    private LinearLayout networkSelectionLayout;
    private ProgressBar loadingWheel;
    private Handler signalStrengthHandler;
    private LinearLayout noWifiPermissionLayout;
    private TextView noWifiPermissionsTextView;
    private Button grantWifiPermissionButton;
    private BroadcastReceiver downloadBroadcastReceiver;
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    Toast.makeText(getContext(), "Permissions granted", Toast.LENGTH_SHORT).show();
                    initUi(null);
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    noWifiPermissionsTextView.setTextColor(getResources().getColor(R.color.red, null));
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_wifi, container, false);

        networkRecyclerView = view.findViewById(R.id.networks_recyclerview);

        refreshButton = view.findViewById(R.id.refresh_networks_button);
        connectedNetworkLayout = view.findViewById(R.id.connected_network_layout);
        connectedNetworkNameTextView = view.findViewById(R.id.connected_network_name_textview);
        connectedNetworkStrengthImageView = view.findViewById(R.id.connected_network_strength_imageview);
        connectedNetworkDownloadSpeedTextView = view.findViewById(R.id.connected_network_download_speed_textview);
        connectedNetworkLatencyTextView = view.findViewById(R.id.connected_network_latency_textview);
        connectedTitleTextView = view.findViewById(R.id.connected_title_textview);
        networkSelectionLayout = view.findViewById(R.id.network_selection_layout);
        loadingWheel = view.findViewById(R.id.network_loading_wheel);
        noWifiPermissionLayout = view.findViewById(R.id.no_wifi_permissions_layout);
        noWifiPermissionsTextView = view.findViewById(R.id.no_wifi_permissions_textview);
        grantWifiPermissionButton = view.findViewById(R.id.grant_wifi_permission_button);

        networkBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleBroadcastReceived(intent);
            }
        };

        downloadBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case BroadcastConstants.network.EVENT_DOWNLOAD_STARTED:
                            networkSelectionLayout.setVisibility(View.GONE);
                            refreshConnectedNetworkStats(2*DEFAULT_REFRESH_INTERVAL_MILLIS);
                    }
                }
            }
        };

        signalStrengthHandler = new Handler();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        availableNetworks = new ArrayList<>();
        startWifiService();

        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(BroadcastConstants.network.ACTION_SHOW_NETWORKS);
        broadcastFilter.addAction(BroadcastConstants.network.EVENT_CONNECTION_RESULT);
        broadcastFilter.addAction(BroadcastConstants.network.ACTION_CONNECT);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(networkBroadcastReceiver, broadcastFilter);
        broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(BroadcastConstants.network.EVENT_DOWNLOAD_STARTED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(downloadBroadcastReceiver, broadcastFilter);

        wifiAdapter = new WifiAdapter(availableNetworks, getActivity());
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        networkRecyclerView.setLayoutManager(layoutManager);
        networkRecyclerView.setAdapter(wifiAdapter);

        refreshButton.setOnClickListener(v -> refreshNetworks());

        grantWifiPermissionButton.setOnClickListener(v -> {
            String permissionToRequest =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                            Manifest.permission.NEARBY_WIFI_DEVICES : Manifest.permission.ACCESS_FINE_LOCATION;
            this.permissionLauncher.launch(permissionToRequest);
        });

        initUi(view);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(networkBroadcastReceiver);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(downloadBroadcastReceiver);
        signalStrengthHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(networkBroadcastReceiver);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(downloadBroadcastReceiver);
        signalStrengthHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    private void startWifiService() {
        if (getActivity() == null) {
            Log.d(TAG, "Activity was null");
            return;
        }
        Log.d(TAG, "Starting WifiService");
        Intent wifiServiceIntent = new Intent(getActivity().getApplicationContext(), WifiService.class);
        getActivity().startService(wifiServiceIntent);
    }

    /**
     * Responsible for text color changing in the whole fragment, based on what activity owns the
     * fragment. Furthermore, it checks if the device is connected to a network, displays UI
     * accordingly. If it detects a lack of wifi permissions, it lets the user know.
     *
     * @param view if null, text color will not be changed.
     */
    private void initUi(@Nullable View view) {
        try {
            if (getActivity().getClass() == LoginActivity.class && view != null) {
                ViewHelper.setTextColorToWhiteInViewAndChildren(view, R.color.white);
            }
        } catch (NullPointerException ignored) {}

        toggleLoadingWheel(true);
        new Handler().postDelayed(this::toggleUiBasedOnWifiConnectivity, 1500);
    }

    /**
     * Shows/hides loading wheel. When showing it, the method also hides every other element
     * on the screen. However, when hiding it you need to take care of showing the correct
     * elements again.
     */
    private void toggleLoadingWheel(boolean value) {
        if (value) {
            noWifiPermissionLayout.setVisibility(View.GONE);
            connectedNetworkLayout.setVisibility(View.GONE);
            networkSelectionLayout.setVisibility(View.GONE);
            loadingWheel.setVisibility(View.VISIBLE);
        } else {
            loadingWheel.setVisibility(View.GONE);
        }
    }

    private void refreshNetworks() {
        Toast.makeText(getContext(), "Looking for networks...", Toast.LENGTH_LONG).show();
        refreshButton.setEnabled(false);
        new Handler().postDelayed(() -> refreshButton.setEnabled(true), 1500);
        wifiAdapter.updateAvailableNetworks(new ArrayList<>());
        Intent intent = new Intent(BroadcastConstants.network.ACTION_SCAN);
        LocalBroadcastManager.getInstance(PraxtourApplication.getAppContext()).sendBroadcast(intent);
    }

    private void handleBroadcastReceived(Intent intent) {
        Log.d(TAG, "Received wifi networks broadcast!");
        if (intent.getAction() == null) {
            Log.d(TAG, "\t but intent.getAction() was null");
            return;
        }
        Log.d(TAG, "\t action is: " + intent.getAction());

        switch (intent.getAction()) {
            case BroadcastConstants.network.ACTION_SHOW_NETWORKS:
                // Fetch list of available networks
                availableNetworks = intent.getParcelableArrayListExtra("networkNames");
                Log.d(TAG, "Received networks in fragment:");
                for (ScanResult result : availableNetworks) {
                    Log.d(TAG, "\t SSID: " + result.SSID + ", Level: " + result.level);
                }

                // Update UI
                initUi(null);
                wifiAdapter.updateAvailableNetworks(cleanScanResultList(availableNetworks));
                break;
            case BroadcastConstants.network.EVENT_CONNECTION_RESULT:
                boolean success = intent.getBooleanExtra("connectionSuccessful", false);
                Log.d(TAG, "Received broadcast, did we connect? " + success);
                new Handler().postDelayed(() -> {
                    if (success) {
                        Toast.makeText(PraxtourApplication.getAppContext(), "Connection successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PraxtourApplication.getAppContext(), "Could not connect", Toast.LENGTH_SHORT).show();
                    }

                    refreshNetworks();
                    initUi(null);
                }, 1000);
            case BroadcastConstants.network.ACTION_CONNECT:
                toggleLoadingWheel(true);
        }
    }

    private void toggleUiBasedOnWifiConnectivity() {
        String connectedNetworkName = PraxWifiManager.getConnectedNetworkName(PraxtourApplication.getAppContext());
        if (NO_WIFI_PERMISSION.equals(connectedNetworkName)) {
            noWifiPermissionLayout.setVisibility(View.VISIBLE);
            connectedNetworkLayout.setVisibility(View.GONE);
            networkSelectionLayout.setVisibility(View.GONE);
        } else if (WIFI_NOT_CONNECTED.equals(connectedNetworkName)) {
            Log.d(TAG, "Not connected");
            connectedNetworkNameTextView.setText("Select a network below");
            noWifiPermissionLayout.setVisibility(View.GONE);
            connectedNetworkLayout.setVisibility(View.VISIBLE);
            connectedTitleTextView.setVisibility(View.GONE);
            networkSelectionLayout.setVisibility(View.VISIBLE);
        } else { // Connected to some network
            Log.d(TAG, "Was connected to " + connectedNetworkName);
            refreshConnectedNetworkStats(DEFAULT_REFRESH_INTERVAL_MILLIS);
            connectedNetworkNameTextView.setText(connectedNetworkName);
            noWifiPermissionLayout.setVisibility(View.GONE);
            connectedNetworkLayout.setVisibility(View.VISIBLE);
            connectedTitleTextView.setVisibility(View.VISIBLE);
            networkSelectionLayout.setVisibility(View.VISIBLE);
        }

        toggleLoadingWheel(false);
    }

    /**
     * Clean {@param list} of any empty strings and duplicates. Returns the
     * cleaned list, preserving the order.
     */
    private ArrayList<ScanResult> cleanScanResultList(ArrayList<ScanResult> list) {
        if (list == null || list.isEmpty()) {
            Log.d(TAG, "Network list was null or empty");
            return new ArrayList<>();
        }

        ArrayList<ScanResult> resultList = new ArrayList<>();

        for (ScanResult sr : list) {
            if (isNetworkToBeListed(sr, resultList)) {
                resultList.add(sr);
            }
        }

        Collections.sort(resultList, (sr1, sr2) -> {
            return Integer.compare(sr2.level, sr1.level); // sort by descending strength
        });

        return resultList;
    }

    /**
     * Check if {@param network} should be added to the array of listed networks.
     * The conditions are:
     *  - There is no network with the same name in the list (sometimes Android's API will return
     *    the same network twice from a scan).
     *  - The network's name is not the empty string.
     *  - The device is not connected to the network with name {@code network.SSID}.
     */
    private boolean isNetworkToBeListed(ScanResult network, ArrayList<ScanResult> alreadyAddedNetworks) {
        String connectedNetworkName = PraxWifiManager.getConnectedNetworkName(PraxtourApplication.getAppContext());

        return !existingName(network, alreadyAddedNetworks) &&
                !network.SSID.trim().isEmpty() &&
                !network.SSID.trim().equals(connectedNetworkName);
    }

    /**
     * Checks if {@param list} has an entry with the same SSID (network name)
     * as {@param scanResult}.
     */
    private boolean existingName(ScanResult scanResult, ArrayList<ScanResult> list) {
        boolean result = false;

        for (ScanResult sr : list) {
            if (sr.SSID.trim().equalsIgnoreCase(scanResult.SSID.trim())) {
                result = true;
            }
        }

        return result;
    }

    private void refreshConnectedNetworkStats() {
        refreshConnectedSignalStrength();
        refreshConnectedNetworkDownloadSpeed();
        refreshConnectedNetworkLatency();
    }

    private void refreshConnectedNetworkStats(int repeatMillis) {
        signalStrengthHandler.removeCallbacksAndMessages(null);
        signalStrengthHandler.post(new Runnable() {
            @Override
            public void run() {
                refreshConnectedNetworkStats();
                signalStrengthHandler.postDelayed(this, repeatMillis);
            }
        });
    }

    private void refreshConnectedSignalStrength() {
        WifiStrength signalStrength = PraxWifiManager.getConnectedNetworkStrength(PraxtourApplication.getAppContext());

        try {
            if (getActivity().getClass() != LoginActivity.class) {
                switch (signalStrength) {
                    case EXCELLENT:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_4_black);
                        break;
                    case GOOD:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_3_black);
                        break;
                    case FAIR:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_2_black);
                        break;
                    case POOR:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_1_black);
                        break;
                    default:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_0_black);
                        break;
                }
            } else {
                switch (signalStrength) {
                    case EXCELLENT:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_4_white);
                        break;
                    case GOOD:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_3_white);
                        break;
                    case FAIR:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_2_white);
                        break;
                    case POOR:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_1_white);
                        break;
                    default:
                        connectedNetworkStrengthImageView.setImageResource(R.drawable.wifi_strength_0_white);
                        break;
                }
            }
        } catch (NullPointerException ignored) {}
    }

    private void refreshConnectedNetworkDownloadSpeed() {
        WifiSpeedtest.getDownloadSpeedMbps(new WifiCallback() {
            @Override
            public void onSuccess(long value) {
                try {
                    getActivity().runOnUiThread(() -> {
                        String valueToBeDisplayed = value + " Mb/s";
                        connectedNetworkDownloadSpeedTextView.setText(valueToBeDisplayed);
                    });
                } catch (NullPointerException e) {
                    Log.d(TAG, "Error running on UI thread");
                }
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    private void refreshConnectedNetworkLatency() {
        WifiSpeedtest.getPingTo("praxtour.nl", new WifiCallback() {
            @Override
            public void onSuccess(long value) {
                try {
                    getActivity().runOnUiThread(() -> {
                        String valueToBeDisplayed = value + "ms";
                        connectedNetworkLatencyTextView.setText(valueToBeDisplayed);
                    });
                } catch (NullPointerException e) {
                    Log.d(TAG, "Error running on UI thread");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "Could not measure latency " + Arrays.toString(e.getStackTrace()));
            }
        });
    }

    /**
     * Reloads fragment from the beginning, triggering onCreateView and onViewCreated.
     */
    private void reloadFragment() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.settings_framelayout, new WifiSettingsFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * This method assumes we are using Android 7 or higher.
     */
    private void requestWifiPermissions() {
        ArrayList<String> permissions = new ArrayList<>();

        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        PermissionHelper.requestPermissions(permissions, getActivity());
    }
}
