package com.videostreamtest.ui.phone.productpicker.fragments.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.data.model.BluetoothDeviceInfo;
import com.videostreamtest.service.bluetooth.BluetoothService;
import com.videostreamtest.helpers.BleHelper;
import com.videostreamtest.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.bluetooth.BluetoothScanCallback;
import com.videostreamtest.utils.bluetooth.BluetoothUtil;

import java.util.ArrayList;

// TODO improve bluetooth permission handling
public class BluetoothConnectionFragment extends Fragment {
    private static final String TAG = BluetoothConnectionFragment.class.getSimpleName();
    private final int CONNECTION_TIMEOUT_MS = 10000;
    private final int REFRESH_DEVICE_INFO_MS = 5000;

    private ProductViewModel productViewModel;
    private Activity hostActivity;
    private final Handler connectedDeviceInfoHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateSelectedDeviceInfoRunnable = new Runnable() {
        @Override
        public void run() {
            updateSelectedDeviceInfo();
            connectedDeviceInfoHandler.postDelayed(this, REFRESH_DEVICE_INFO_MS);
        }
    };
    private final Runnable startBluetoothScanRunnable = new Runnable() {
        @Override
        public void run() {
            startBluetoothScan();
        }
    };
    private final Runnable failedConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (!connected) {
                goBackToScanning();
                Toast.makeText(getContext(), "Connection failed", Toast.LENGTH_LONG).show();
            }
        }
    };

    // CONNECTED DEVICE VIEW
    private boolean connected;
    private LinearLayout deviceInfoBoxLayout;
    private TextView deviceConnectionStrengthTextView;
    private TextView deviceBatteryLevelTextView;
    private TextView deviceNameTextView;
    private TextView deviceConnectionStatusTextView;
    private Button disconnectButton;

    // DATA: don't need to display immediately, but store fresh values
    // -> views are updated at the same time
    private String currentConnectionStrength;
    private String currentBatteryLevel;

    // SELECTION VIEW
    private RecyclerView bluetoothDevicesRecyclerView;
    /** Adapter for the RecyclerView */
    private BluetoothDeviceAdapter bluetoothDeviceAdapter;
    private BluetoothDeviceInfo selectedDevice;

    // OTHER VIEWS
    private TextView instructionsForConnectionTextView;
    private TextView loadingTextView;

    // BLUETOOTH
    /** Represents the physical BT antenna of the device */
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback bluetoothScanCallback;
    private BluetoothService bluetoothService;

    /** Messages come from the gatt server <b>after connection has been made</b>.
     *  This object stays alive until we are in the fragment, even if {@link BluetoothService}
     *  has been stopped/unbound.
     */
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!PermissionHelper.hasBluetoothPermissions(PraxtourApplication.getAppContext())) return;

            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_STARTED_CONNECTING.equals(action)) {
                connectedDeviceInfoHandler.postDelayed(failedConnectionRunnable, CONNECTION_TIMEOUT_MS);
            } else if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                connectedDeviceInfoHandler.removeCallbacks(failedConnectionRunnable);
                connectedDeviceInfoHandler.post(updateSelectedDeviceInfoRunnable);
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                goBackToScanning();
            } else if (BluetoothService.ACTION_BATTERY_DATA_AVAILABLE.equals(action)) {
                int batteryLevel = intent.getIntExtra(BluetoothService.EXTRA_DATA, -1);
                if (batteryLevel < 0 || batteryLevel > 100) {
                    currentBatteryLevel = getResources().getString(R.string.settings_ble_battery_value_placeholder);
                } else {
                    currentBatteryLevel = String.format("%d%%", batteryLevel);
                }
                Log.d(TAG, "Battery level: " + batteryLevel);
            } else if (BluetoothService.ACTION_RSSI_DATA_AVAILABLE.equals(action)) {
                int rssi = intent.getIntExtra(BluetoothService.EXTRA_DATA, Integer.MAX_VALUE);
                if (rssi < Integer.MAX_VALUE) {
                    currentConnectionStrength = BleHelper.getRssiStrengthIndicator(PraxtourApplication.getAppContext(), rssi);
                } else {
                    currentConnectionStrength = "Could not get signal strength";
                }
            } else if (BluetoothService.ACTION_TOAST_MESSAGE.equals(action)) {
                String message = intent.getStringExtra(BluetoothService.EXTRA_DATA);
                if (message == null) return;
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            } else if (BluetoothService.ACTION_SAVE_DEVICE.equals(action)) {
                ArrayList<String> info = intent.getStringArrayListExtra(BluetoothService.EXTRA_DATA);
                if (info == null || info.size() < 2) {
                    Log.d(TAG, "Device info was not received correctly");
                    return;
                }
                saveDeviceInfo(info.get(0), info.get(1));
            } else if (BluetoothService.ACTION_RPM_DATA_AVAILABLE.equals(action)) {
                int lastMeasurement = intent.getIntExtra(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, -1);
                Log.d(TAG, String.format("Last measurement from cadence: %d", lastMeasurement));
            }
        }
    };

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                startBluetoothScan();
            } else {
                Toast.makeText(getContext(), "Bluetooth required to scan devices", Toast.LENGTH_SHORT).show();
            }
        });

    /** Listens to connect events from the recycler view. */
    public interface OnDeviceSelectedListener {
        void connectToDevice(BluetoothDeviceInfo device);
    }

    OnDeviceSelectedListener onDeviceSelectedListener;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothService = ((BluetoothService.LocalBinder) service).getService();
            if (bluetoothService != null) {
                if (!bluetoothService.init()) {
                    Toast.makeText(hostActivity, "No bluetooth from service", Toast.LENGTH_SHORT).show();
                    return;
                }

                bluetoothService.connect(selectedDevice.getAddress());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            hostActivity = (Activity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() called");
        View view = inflater.inflate(R.layout.fragment_bluetooth_connection, container, false);

        setDefaultSelectedDeviceValues();

        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        deviceInfoBoxLayout = view.findViewById(R.id.selected_device_info_box);
        deviceConnectionStrengthTextView = view.findViewById(R.id.selected_device_connection_strength_textview);
        deviceBatteryLevelTextView = view.findViewById(R.id.selected_device_battery_textview);
        deviceNameTextView = view.findViewById(R.id.selected_device_name_textview);
        deviceConnectionStatusTextView = view.findViewById(R.id.selected_device_connection_status_textview);
        disconnectButton = view.findViewById(R.id.ble_sensor_disconnect_button);
        disconnectButton.setOnClickListener(v -> {
            if (hostActivity != null) {
                Log.d(TAG, "User disconnected from device");
                unbindService();
                stopService();
                bluetoothDeviceAdapter.clearDeviceList();
                setDefaultSelectedDeviceValues();
            }
        });

        instructionsForConnectionTextView = view.findViewById(R.id.instructions_to_connect_textview);
        loadingTextView = view.findViewById(R.id.loading_textview);

        onDeviceSelectedListener = this::initiateConnectionToDevice;
        bluetoothDevicesRecyclerView = view.findViewById(R.id.available_bluetooth_devices_recyclerview);
        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(productViewModel, onDeviceSelectedListener);
        bluetoothDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        bluetoothDevicesRecyclerView.setAdapter(bluetoothDeviceAdapter);

        bluetoothScanCallback = new BluetoothScanCallback(hostActivity, bluetoothDeviceAdapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag") // The code checks this, lint is just bad
    @Override
    public void onResume() {
        super.onResume();

        if (hostActivity == null || !BluetoothUtil.isBluetoothAvailableOnDevice(hostActivity)) {
            Toast.makeText(PraxtourApplication.getAppContext(), "Bluetooth not available on device", Toast.LENGTH_LONG).show();
            return;
        }
        if (!setupBluetooth()) return;
        Log.d(TAG, "BT is available");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hostActivity.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter(), Context.RECEIVER_NOT_EXPORTED);
        } else {
            hostActivity.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        BluetoothDeviceInfo savedDeviceInfo = getSavedDevice();
        if (savedDeviceInfo != null) {
            Log.d(TAG, "Found saved device: " + savedDeviceInfo.getName() + ", attempting auto-connect");
            if (onDeviceSelectedListener == null) onDeviceSelectedListener = this::initiateConnectionToDevice;
            onDeviceSelectedListener.connectToDevice(savedDeviceInfo);
            return;
        }

        startBluetoothScan();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPause() {
        super.onPause();
        if (PermissionHelper.hasBluetoothPermissions(hostActivity) && bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(bluetoothScanCallback);
        }

        if (hostActivity != null) {
            try {
                hostActivity.unregisterReceiver(gattUpdateReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver not registered");
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        hostActivity = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connectedDeviceInfoHandler.removeCallbacksAndMessages(null);
    }

    /** Start BluetoothService */
    @SuppressLint("MissingPermission")
    private void initiateConnectionToDevice(BluetoothDeviceInfo device) {
        if (hostActivity == null || !PermissionHelper.hasBluetoothPermissions(hostActivity)) return;
        if (bluetoothLeScanner != null) bluetoothLeScanner.stopScan(bluetoothScanCallback);
        this.selectedDevice = device;
        updateSelectedDeviceInfo();
        showSelectedDeviceInfo();
        Intent intent = new Intent(hostActivity, BluetoothService.class);
        intent.putExtra("selected_device_address", device.getAddress());

        // call first to keep alive after leaving fragment/activity
        hostActivity.startService(intent);
        // bind after
        hostActivity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        Toast.makeText(hostActivity, "Connecting to " + device.getName() + "!", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission") // suppress because it is actually being checked, lint is just bad
    private boolean setupBluetooth() {
        bluetoothAdapter = BluetoothUtil.getBluetoothAdapter(hostActivity);
        if (bluetoothAdapter == null) return false;

        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "BT not enabled, asking user");
            if (!PermissionHelper.hasBluetoothPermissions(hostActivity)) {
                PermissionHelper.requestBluetoothPermissions(hostActivity);
                return false;
            }
            enableBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return false;
        }

        Log.d(TAG, "BT already enabled");
        return true;
    }

    /**
     * 1. Stops pre-existing bluetooth scan, if necessary.
     * 2. Checks for a saved bluetooth device and tries to connect to it.
     * 3. If there is no saved device, starts scanning and shows selection page
     */
    @SuppressLint("MissingPermission") // suppress because it is actually being checked, lint is just bad
    private void startBluetoothScan() {
        if (hostActivity == null || !PermissionHelper.hasBluetoothPermissions(hostActivity)) return;

        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(bluetoothScanCallback);
        }
        if (bluetoothAdapter == null) return;
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        Log.d(TAG, "Beginning bluetooth scan");
        bluetoothLeScanner.startScan(bluetoothScanCallback);
        showDeviceSelectionPage();
    }

    private void saveDeviceInfo(String address, String name) {
        SharedPreferences sharedPreferences = hostActivity.getSharedPreferences("app", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY, address);
        editor.putString(ApplicationSettings.DEFAULT_BLE_DEVICE_NAME_KEY, name);
        editor.apply();
    }

    private BluetoothDeviceInfo getSavedDevice() {
        SharedPreferences sharedPreferences = hostActivity.getSharedPreferences("app", Context.MODE_PRIVATE);
        String savedAddress = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY, null);
        String savedName = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_NAME_KEY, null);

        if (savedAddress == null || savedName == null) return null;

        return new BluetoothDeviceInfo(savedAddress, savedName, -1);
    }

    private void clearSavedDevice() {
        SharedPreferences sharedPreferences = hostActivity.getSharedPreferences("app", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY);
        editor.remove(ApplicationSettings.DEFAULT_BLE_DEVICE_NAME_KEY);
        editor.apply();
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_BATTERY_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothService.ACTION_RSSI_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothService.ACTION_TOAST_MESSAGE);
        intentFilter.addAction(BluetoothService.ACTION_GATT_STARTED_CONNECTING);

        intentFilter.addAction(BluetoothService.ACTION_RPM_DATA_AVAILABLE);
        return intentFilter;
    }

    @SuppressLint("MissingPermission")
    private void updateSelectedDeviceInfo() {
        String connectionStatus = "";
        if (connected && currentBatteryLevel != null && !getResources().getString(R.string.settings_ble_battery_value_placeholder).equals(currentBatteryLevel)) {
            connectionStatus = "Connected";
        } else if (connected  && getResources().getString(R.string.settings_ble_battery_value_placeholder).equals(currentBatteryLevel)) {
            connectionStatus = "Paired, connecting...";
        } else if (!connected) {
            connectionStatus = "Setting up Bluetooth";
            connectedDeviceInfoHandler.postDelayed(() -> {
                try {
                    deviceConnectionStatusTextView.setText("Beginning to pair");
                } catch (Exception ignored) {}
            }, BluetoothService.CONNECTION_DELAY_MS / 2);
        }
        deviceNameTextView.setText(selectedDevice.getName());
        deviceConnectionStrengthTextView.setText(currentConnectionStrength);
        deviceBatteryLevelTextView.setText(currentBatteryLevel);
        deviceConnectionStatusTextView.setText(connectionStatus);
    }

    private void showSelectedDeviceInfo() {
        bluetoothDevicesRecyclerView.setVisibility(View.GONE);
        instructionsForConnectionTextView.setVisibility(View.GONE);

        deviceInfoBoxLayout.setVisibility(View.VISIBLE);

        loadingTextView.setVisibility(View.GONE);
    }

    private void showDeviceSelectionPage() {
        bluetoothDevicesRecyclerView.setVisibility(View.VISIBLE);
        instructionsForConnectionTextView.setVisibility(View.VISIBLE);

        deviceInfoBoxLayout.setVisibility(View.GONE);

        loadingTextView.setVisibility(View.GONE);
    }

    private void showLoading() {
        bluetoothDevicesRecyclerView.setVisibility(View.GONE);
        instructionsForConnectionTextView.setVisibility(View.GONE);

        deviceInfoBoxLayout.setVisibility(View.GONE);

        loadingTextView.setVisibility(View.VISIBLE);
    }

    private void setDefaultSelectedDeviceValues() {
        currentBatteryLevel = getResources().getString(R.string.settings_ble_battery_value_placeholder);
        currentConnectionStrength = "--";
    }

    private void unbindService() {
        try {
            hostActivity.unbindService(serviceConnection);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "BluetoothService was already unbound");
        }
    }

    private void stopService() {
        hostActivity.stopService(new Intent(hostActivity, BluetoothService.class));
    }

    private void goBackToScanning() {
        connected = false;
        connectedDeviceInfoHandler.removeCallbacksAndMessages(updateSelectedDeviceInfoRunnable);
        if (bluetoothLeScanner == null && bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        unbindService();
        stopService();

        clearSavedDevice();
        showLoading();
        connectedDeviceInfoHandler.postDelayed(startBluetoothScanRunnable, 2*BluetoothService.CLOSE_DELAY_AFTER_DISCONNECT_MS);
    }
}
