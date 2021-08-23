package com.videostreamtest.ui.phone.productview.fragments.messagebox;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.service.ble.callback.BleScanCallback;
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

    private Button closeButton;
    private TextView deviceLabel;
    private RecyclerView showBleDevicesRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ble_devices_block, container, false);

        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

//        closeButton = view.findViewById(R.id.current_connected_device_close_button);
        deviceLabel = view.findViewById(R.id.messagebox_connected_device_label);
        showBleDevicesRecyclerView = view.findViewById(R.id.messagebox_available_ble_devices);

        deviceConnectionStrengthLabel = view.findViewById(R.id.current_connected_device_connection_strength_label);
        deviceBatterylevelLabel = view.findViewById(R.id.current_connected_device_battery_label);
        deviceNameLabel = view.findViewById(R.id.current_connected_device_label);

//        closeButton.setOnClickListener(onClickView -> {
//            closeMessageBox();
//        });

//        initOnFocusChangeCloseMessageboxButtonListener();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
//        closeButton.requestFocus();
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
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No ACCESS_BACKGROUND_LOCATION permission.");
        }

        loadBluetoothDefaultDeviceInformation();

        if (checkBluetoothSupport(bluetoothAdapter)) {

            scanner = bluetoothAdapter.getBluetoothLeScanner();

            final BleDeviceInformationAdapter bleDeviceInformationAdapter = new BleDeviceInformationAdapter(productViewModel);
            bleScanCallback = new BleScanCallback(bleDeviceInformationAdapter);
            scanner.startScan(bleScanCallback);

            showBleDevicesRecyclerView.setHasFixedSize(true);

            //Maak lineaire layoutmanager en zet deze op horizontaal
            LinearLayoutManager layoutManager
                    = new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false);

            //Zet de layoutmanager erin
            showBleDevicesRecyclerView.setLayoutManager(layoutManager);
            showBleDevicesRecyclerView.setAdapter(bleDeviceInformationAdapter);

        } else {
            showWarningBleNotSupported();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scanner != null) {
            scanner.stopScan(bleScanCallback);
        }
    }

    private void loadBluetoothDefaultDeviceInformation() {
        BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        LinearLayout linearLayoutConnectionDeviceSummary = getView().findViewById(R.id.overlay_connection_info_box);
        if (bluetoothAdapter!= null) {
            bluetoothAdapter.enable();
            linearLayoutConnectionDeviceSummary.setVisibility(View.VISIBLE);

            productViewModel.getBluetoothDefaultDevices().observe(getViewLifecycleOwner(), bluetoothDefaultDevices -> {
                if (bluetoothDefaultDevices != null && bluetoothDefaultDevices.size()>0) {
                    BluetoothDefaultDevice bluetoothDefaultDevice =  bluetoothDefaultDevices.get(0);
                    if (bluetoothDefaultDevice.getBleName() != null && !bluetoothDefaultDevice.getBleName().isEmpty()) {
                        deviceNameLabel.setText(bluetoothDefaultDevice.getBleName());
                        if (!bluetoothDefaultDevice.getBleBatterylevel().isEmpty() && bluetoothDefaultDevice.getBleBatterylevel()!="") {
                            deviceBatterylevelLabel.setText(bluetoothDefaultDevice.getBleBatterylevel() + "%");
                        }
                        if (bluetoothDefaultDevice.getBleSignalStrength() != null && !bluetoothDefaultDevice.getBleSignalStrength().isEmpty()) {
                            deviceConnectionStrengthLabel.setText(bluetoothDefaultDevice.getBleSignalStrength());
                        }
                    } else {
                        deviceNameLabel.setText("No device");
                        deviceConnectionStrengthLabel.setText("No device");
                        deviceBatterylevelLabel.setText("0%");
                    }
                }
            });
        } else {
            linearLayoutConnectionDeviceSummary.setVisibility(View.GONE);
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

    private void showCurrentConnectedDevice(final TextView deviceLabel) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("app" , Context.MODE_PRIVATE);
        String deviceName = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_NAME_KEY,"");
        if (deviceName != null && !deviceName.isEmpty()) {
            deviceLabel.setText("Current: "+deviceName);
        }
    }

//    private void initOnFocusChangeCloseMessageboxButtonListener() {
//        closeButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//
//                closeButton.setSelected(true);
//                if (hasFocus) {
//                    final Drawable border = getContext().getDrawable(R.drawable.imagebutton_blue_border);
//                    closeButton.setBackground(border);
//                    closeButton.setBackgroundTintMode(PorterDuff.Mode.ADD);
//                } else {
//                    final Drawable border = getContext().getDrawable(R.drawable.imagebutton_red_border);
//                    closeButton.setBackground(border);
//                    closeButton.setBackgroundTintMode(PorterDuff.Mode.SRC_OVER);
//                }
//            }
//        });
//    }
}
