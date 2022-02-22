package com.videostreamtest.ui.phone.productview.fragments.messagebox.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.service.ble.callback.BleScanCallback;
import com.videostreamtest.ui.phone.helpers.ViewHelper;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.utils.ApplicationSettings;

import org.jetbrains.annotations.NotNull;

import static android.content.Context.BLUETOOTH_SERVICE;

public class BleDeviceInformationBoxFragment extends Fragment {
    private static final String TAG = BleDeviceInformationBoxFragment.class.getSimpleName();

    private BluetoothManager bluetoothManager;
    private BleScanCallback bleScanCallback;
    private BluetoothLeScanner scanner;

    private ProductViewModel productViewModel;

    private TextView deviceConnectionStrengthLabel;
    private TextView deviceBatterylevelLabel;
    private TextView deviceNameLabel;

    private Button disconnectButton;
    private TextView deviceLabel;
    private RecyclerView showBleDevicesRecyclerView;

    private int refreshOverviewCounter =0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ble_devices_block, container, false);

        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        deviceLabel = view.findViewById(R.id.messagebox_connected_device_label);
        showBleDevicesRecyclerView = view.findViewById(R.id.messagebox_available_ble_devices);

        deviceConnectionStrengthLabel = view.findViewById(R.id.current_connected_device_connection_strength_label);
        deviceBatterylevelLabel = view.findViewById(R.id.current_connected_device_battery_label);
        deviceNameLabel = view.findViewById(R.id.current_connected_device_label);

        disconnectButton = view.findViewById(R.id.ble_sensor_disconnect_button);
        disconnectButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    final Drawable border = v.getContext().getDrawable(R.drawable.imagebutton_red_border);
                    disconnectButton.setBackground(border);
                } else {
                    disconnectButton.setBackground(null);
                }
            }
        });

        if (ViewHelper.isTouchScreen(getActivity())) {
            final Drawable border = getActivity().getDrawable(R.drawable.imagebutton_red_border);
            disconnectButton.setBackground(border);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        // Bluetooth Low Energy
        bluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No ACCES_COARSE_LOCATION permission.");
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH_ADMIN permission.");
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH permission.");
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, "No ACCESS_BACKGROUND_LOCATION permission.");
        }

        if (hasBluetoothPermissions()) {
            loadBluetoothDefaultDeviceInformation();
            initDisconnectButtonOnClickListener();
        } else {
            showNoPermissionsMessagefragment();
        }
        //showWarningBle(1, 30000);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scanner != null && bleScanCallback != null) {
            scanner.stopScan(bleScanCallback);
            Log.d(TAG, "BLE Scanning stopped.");
        }
    }

    private void loadBluetoothDefaultDeviceInformation() {
        BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        LinearLayout linearLayoutConnectionDeviceSummary = getView().findViewById(R.id.overlay_connection_info_box);

        if (bluetoothAdapter != null) {
            bluetoothAdapter.enable();
            linearLayoutConnectionDeviceSummary.setVisibility(View.VISIBLE);

            productViewModel.getBluetoothDefaultDevices().observe(getViewLifecycleOwner(), bluetoothDefaultDevices -> {
                if (bluetoothDefaultDevices != null && bluetoothDefaultDevices.size()>0) {
                    LinearLayout searchSensorLayout = getView().findViewById(R.id.overlay_messagebox_connection_info_summary);
                    LinearLayout sensorStatusView = getView().findViewById(R.id.overlay_connection_info_box);

                    if (!bluetoothDefaultDevices.get(0).getBleAddress().equals("NONE")
                            && !bluetoothDefaultDevices.get(0).getBleAddress().equals("") //&& !deviceAddress.equals("NONE")
                        ) {
                        searchSensorLayout.setVisibility(View.GONE);
                        sensorStatusView.setVisibility(View.VISIBLE);
                    } else {
                        searchSensorLayout.setVisibility(View.VISIBLE);
                        sensorStatusView.setVisibility(View.GONE);
                    }

                    if (scanner != null && sensorStatusView.getVisibility() == View.VISIBLE) {
                        scanner.flushPendingScanResults(bleScanCallback);
                        scanner.stopScan(bleScanCallback);
                    } else {
                        startScanForDevices(bluetoothManager.getAdapter());
                    }

                    BluetoothDefaultDevice bluetoothDefaultDevice =  bluetoothDefaultDevices.get(0);
                    if (bluetoothDefaultDevice.getBleName() != null && !bluetoothDefaultDevice.getBleName().isEmpty()
                        && !bluetoothDefaultDevices.get(0).getBleAddress().equals("NONE")) {
                        deviceNameLabel.setText(bluetoothDefaultDevice.getBleName());
                        if (!bluetoothDefaultDevice.getBleBatterylevel().isEmpty() && bluetoothDefaultDevice.getBleBatterylevel()!="") {
                            deviceBatterylevelLabel.setText(bluetoothDefaultDevice.getBleBatterylevel() + "%");
                        }
                        if (bluetoothDefaultDevice.getBleSignalStrength() != null && !bluetoothDefaultDevice.getBleSignalStrength().isEmpty()) {
                            deviceConnectionStrengthLabel.setText(bluetoothDefaultDevice.getBleSignalStrength());
                        }
                    } else {
                        deviceNameLabel.setText(getString(R.string.settings_ble_value_placeholder_no_device));
                        deviceConnectionStrengthLabel.setText(getString(R.string.settings_ble_value_placeholder_no_device));
                        deviceBatterylevelLabel.setText(getString(R.string.settings_ble_battery_value_placeholder));
                    }
                }
            });
        } else {
            linearLayoutConnectionDeviceSummary.setVisibility(View.GONE);
            showWarningBleNotSupported();
        }
    }

    private void closeMessageBox() {
        Fragment searchFragment = getActivity().getSupportFragmentManager().findFragmentByTag("device-information");
        if (searchFragment != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(searchFragment).commit();
        }
    }

    private void showWarningBleNotSupported() {
        deviceLabel.setText("Bluetooth Low Energy not supported or turned on.");
        showBleDevicesRecyclerView.setVisibility(View.GONE);
        TextView unsupportedMessage = getActivity().findViewById(R.id.ble_not_supported_message);
        unsupportedMessage.setVisibility(View.VISIBLE);
    }

    private boolean hasBluetoothPermissions() {
        boolean permissionsAcquired = true;
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No ACCES_COARSE_LOCATION permission.");
            permissionsAcquired = false;
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH_ADMIN permission.");
            permissionsAcquired = false;
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH permission.");
            permissionsAcquired = false;
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ) {
            Log.d(TAG, "No ACCESS_BACKGROUND_LOCATION permission.");
            permissionsAcquired = false;
        }
        return permissionsAcquired;
    }

    private void showNoPermissionsMessagefragment() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        if (navController.getCurrentDestination().getId() == R.id.bleDeviceInformationBoxFragment) {
            navController.navigate(R.id.permissionErrorMessage);
        }
    }

    private boolean checkBluetoothSupport(final BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }
        return true;
    }

    private void startScanForDevices(final BluetoothAdapter bluetoothAdapter) {
        if (checkBluetoothSupport(bluetoothAdapter)) {
            Toast.makeText(getActivity(), getString(R.string.ble_choose_connected_device_toast_message), Toast.LENGTH_LONG).show();
            scanner = bluetoothAdapter.getBluetoothLeScanner();

            final BleDeviceInformationAdapter bleDeviceInformationAdapter = new BleDeviceInformationAdapter(productViewModel);
            bleScanCallback = new BleScanCallback(bleDeviceInformationAdapter);
            scanner.startScan(bleScanCallback);

            /**
             * First idea is to write NONE to the room database of default ble device on logout
             */
            showBleDevicesRecyclerView.setHasFixedSize(true);

            //Maak lineaire layoutmanager en zet deze op horizontaal
            LinearLayoutManager layoutManager
                    = new LinearLayoutManager(getView().getContext(), LinearLayoutManager.VERTICAL, false);

            //Zet de layoutmanager erin
            showBleDevicesRecyclerView.setLayoutManager(layoutManager);
            showBleDevicesRecyclerView.setAdapter(bleDeviceInformationAdapter);
        } else {
            showWarningBleNotSupported();
        }
    }

    private void initDisconnectButtonOnClickListener() {
        disconnectButton.setOnClickListener((viewClicked) ->{
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY, "NONE");
            editor.commit();

            BluetoothDefaultDevice bluetoothDefaultDevice = new BluetoothDefaultDevice();
            bluetoothDefaultDevice.setBleId(1);
            bluetoothDefaultDevice.setBleAddress("NONE");
            bluetoothDefaultDevice.setBleName("");
            bluetoothDefaultDevice.setBleSensorType("");
            bluetoothDefaultDevice.setBleSignalStrength("--");
            bluetoothDefaultDevice.setBleBatterylevel("--");
            productViewModel.insertBluetoothDefaultDevice(bluetoothDefaultDevice);

            Intent bleService = new Intent(getActivity().getApplicationContext(), BleService.class);
            getActivity().startService(bleService);
            startScanForDevices(bluetoothManager.getAdapter());
        });
    }

    private void showWarningBle(int howManyTimes, int howLongInMs) {
        refreshOverviewCounter = 0;
        LinearLayout warningBox = getActivity().findViewById(R.id.overlay_connection_warning_box);
        Handler refreshTimer = new Handler(Looper.getMainLooper());
        Runnable refreshRoutefilmOverview = new Runnable() {
            public void run() {
                if (warningBox !=null) {
                    warningBox.setVisibility(View.VISIBLE);
                    refreshOverviewCounter++;
                    if (refreshOverviewCounter >= howManyTimes) {
                        refreshTimer.removeCallbacks(null);
                    } else {
                        refreshTimer.postDelayed(this, howLongInMs);
                    }
                }
            }
        };
        refreshTimer.postDelayed(refreshRoutefilmOverview, howLongInMs);
    }
}
