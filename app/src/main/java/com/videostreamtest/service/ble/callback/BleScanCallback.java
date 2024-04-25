package com.videostreamtest.service.ble.callback;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.videostreamtest.data.model.BleDeviceInfo;
import com.videostreamtest.service.ble.CSCProfile;
import com.videostreamtest.ui.phone.productpicker.fragments.ble.BleDeviceInformationAdapter;
import com.videostreamtest.ui.phone.splash.SplashActivity;
import com.videostreamtest.utils.BlePermission;

import java.util.List;

public class BleScanCallback extends ScanCallback {
    private static final String TAG = BleScanCallback.class.getSimpleName();

    //private final String blePermNeeded = "Bluetooth permissions are needed to use this application";
    private final String blePermNeeded = "BLE perm needed";

    private final Activity activity;

    private final Context context;

    private BleDeviceInformationAdapter bleDeviceInformationAdapter;

    public BleScanCallback(BleDeviceInformationAdapter bleDeviceInformationAdapter, Activity activity) {
        this.bleDeviceInformationAdapter = bleDeviceInformationAdapter;
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (bleDeviceInformationAdapter == null) {
            Log.d(TAG, "No BleDeviceInformationAdapter found.");
            return;
        }

        checkBLEConnectPermission("onScanResult");

        if (result != null && result.getDevice() != null && result.getDevice().getName() != null) {
            Log.d(TAG, "ScanResult NAME:: " + result.getDevice().getName());
            if (result.getScanRecord().getServiceUuids() != null) {
                Log.d(TAG, "ScanResult SIZE :: " + result.getScanRecord().getServiceUuids().size());
                if (result.getScanRecord().getServiceUuids().size() > 0) {
                    for (ParcelUuid parcelUuid : result.getScanRecord().getServiceUuids()) {
                        Log.d(TAG, parcelUuid.toString());

                        if (parcelUuid.getUuid().equals(CSCProfile.RSC_SERVICE)) {
                            Log.d(TAG, "ScanResult :: Running sensor found ::> " + result.getDevice().getName());
                            if (deviceAlreadyScanned(result.getDevice())) {
                                updateBleDeviceInfoListing(result.getDevice(), result.getRssi());
                            } else {
                                addNearbyBleDeviceToList(result.getDevice(), result.getRssi(), "RUNNING");
                            }
                        }
                        if (parcelUuid.getUuid().equals(CSCProfile.CSC_SERVICE)) {
                            Log.d(TAG, "ScanResult :: CYCLING sensor found ::> " + result.getDevice().getName());
                            if (deviceAlreadyScanned(result.getDevice())) {
                                updateBleDeviceInfoListing(result.getDevice(), result.getRssi());
                            } else {
                                addNearbyBleDeviceToList(result.getDevice(), result.getRssi(), "CYCLING");
                            }
                        }
                        Log.d(TAG, "RSSI: " + result.getRssi());
                    }
                }
            }
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        Log.d(TAG, "ScanBatchResult :: " + results.size());
    }

    @Override
    public void onScanFailed(int errorCode) {
        Log.d(TAG, "ScanFailedResult :: " + errorCode);
    }

    private void checkBLEConnectPermission(String from) {
        // Split on Android v12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, blePermNeeded, Toast.LENGTH_LONG).show();
                BlePermission.ask(activity, Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, blePermNeeded, Toast.LENGTH_LONG).show();
                BlePermission.ask(activity, Manifest.permission.BLUETOOTH);
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context,  blePermNeeded, Toast.LENGTH_LONG).show();
                BlePermission.ask(activity, Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
    }

    private boolean deviceAlreadyScanned(final BluetoothDevice bluetoothDevice) {
        if (bleDeviceInformationAdapter == null) {
            return false;
        }
        if (bleDeviceInformationAdapter.getItemCount() > 0) {
            for (BleDeviceInfo bleDeviceInfo : bleDeviceInformationAdapter.getAllBleDeviceInfo()) {
                checkBLEConnectPermission("deviceAlreadyScanned");

                if (bluetoothDevice.getName().toLowerCase().equals(bleDeviceInfo.getBluetoothDevice().getName().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addNearbyBleDeviceToList(final BluetoothDevice bluetoothDevice, final int rssi, final String sensorType) {
        if (bleDeviceInformationAdapter ==null) {
            return;
        }
        final BleDeviceInfo bleDeviceInfo = new BleDeviceInfo();
        bleDeviceInfo.setBluetoothDevice(bluetoothDevice);
        bleDeviceInfo.setConnectionStrength(rssi);
        bleDeviceInfo.setDeviceType(sensorType);
        bleDeviceInformationAdapter.addBleDeviceInfo(bleDeviceInfo);
    }

    private void updateBleDeviceInfoListing(final BluetoothDevice bluetoothDevice, final int rssi) {
        if (bleDeviceInformationAdapter ==null) {
            return;
        }
        if (bleDeviceInformationAdapter.getItemCount()>0) {
            for (BleDeviceInfo bleDeviceInfo: bleDeviceInformationAdapter.getAllBleDeviceInfo()) {
                checkBLEConnectPermission("updateBleDeviceInfoListing");

                if (bluetoothDevice.getName().toLowerCase().equals(bleDeviceInfo.getBluetoothDevice().getName().toLowerCase())) {
                    bleDeviceInfo.setBluetoothDevice(bluetoothDevice);
                    bleDeviceInfo.setConnectionStrength(rssi);
                    bleDeviceInformationAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
