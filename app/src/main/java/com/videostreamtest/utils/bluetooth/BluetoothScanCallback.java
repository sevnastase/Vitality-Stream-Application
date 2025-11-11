package com.videostreamtest.utils.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.util.Log;

import com.videostreamtest.data.model.BluetoothDeviceInfo;
import com.videostreamtest.service.bluetooth.CSCProfile;
import com.videostreamtest.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.productpicker.fragments.bluetooth.BluetoothDeviceAdapter;

import java.util.ArrayList;
import java.util.List;

public class BluetoothScanCallback extends ScanCallback {
    private final static String TAG = BluetoothScanCallback.class.getSimpleName();
    private final Activity activity;
    private final BluetoothDeviceAdapter bluetoothDeviceAdapter;

    public BluetoothScanCallback(Activity activity, BluetoothDeviceAdapter bluetoothDeviceAdapter) {
        this.activity = activity;
        this.bluetoothDeviceAdapter = bluetoothDeviceAdapter;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        if (PermissionHelper.hasBluetoothPermissions(activity)) {
            if(result != null
                    && result.getDevice() != null
                    && result.getDevice().getName() != null
                    && result.getScanRecord() != null) {
                Log.d(TAG, "Found valid device");
                BluetoothDevice bluetoothDevice = result.getDevice();
                List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
                if (uuids != null && !uuids.isEmpty()) {
                    List<BluetoothDeviceInfo> btDeviceInfos = new ArrayList<>();
                    for (ParcelUuid parcelUuid: uuids) {
                        if (parcelUuid.getUuid().equals(CSCProfile.CSC_SERVICE)) {
                            Log.d(TAG, "\t ScanResult :: CYCLING Sensor found ::> " + result.getDevice().getName());
                            btDeviceInfos.add(deviceFromScanResult(bluetoothDevice.getAddress(), bluetoothDevice.getName(), result.getRssi()));
                        }
                    }

                    bluetoothDeviceAdapter.addDevices(btDeviceInfos);
                }
            }
        }
    }

    private BluetoothDeviceInfo deviceFromScanResult(final String address, final String name, final int connectionStrength) {
        return new BluetoothDeviceInfo(address, name, connectionStrength);
    }
}
