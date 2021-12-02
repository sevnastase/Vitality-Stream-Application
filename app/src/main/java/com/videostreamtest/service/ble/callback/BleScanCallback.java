package com.videostreamtest.service.ble.callback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.util.Log;

import com.videostreamtest.data.model.BleDeviceInfo;
import com.videostreamtest.service.ble.CSCProfile;
import com.videostreamtest.ui.phone.productview.fragments.messagebox.ble.BleDeviceInformationAdapter;

import java.util.List;

public class BleScanCallback extends ScanCallback {
    private static final String TAG = BleScanCallback.class.getSimpleName();

    private BleDeviceInformationAdapter bleDeviceInformationAdapter;

    public BleScanCallback(BleDeviceInformationAdapter bleDeviceInformationAdapter) {
        this.bleDeviceInformationAdapter = bleDeviceInformationAdapter;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (bleDeviceInformationAdapter == null) {
            Log.d(TAG, "No BleDeviceInformationAdapter found.");
            return;
        }

        if(result != null && result.getDevice()!=null && result.getDevice().getName()!=null) {
            Log.d(TAG, "ScanResult NAME:: " + result.getDevice().getName());
            if (result.getScanRecord().getServiceUuids()!= null) {
                Log.d(TAG, "ScanResult SIZE :: " + result.getScanRecord().getServiceUuids().size());
                if (result.getScanRecord().getServiceUuids().size() > 0 ) {
                    for (ParcelUuid parcelUuid: result.getScanRecord().getServiceUuids()) {
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
                        Log.d(TAG, "RSSI: "+result.getRssi());
                    }
                }
            }
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        Log.d(TAG, "ScanBatchResult :: "+results.size());
    }

    @Override
    public void onScanFailed(int errorCode) {
        Log.d(TAG, "ScanFailedResult :: "+errorCode);
    }

    private boolean deviceAlreadyScanned(final BluetoothDevice bluetoothDevice) {
        if (bleDeviceInformationAdapter == null) {
            return false;
        }
        if (bleDeviceInformationAdapter.getItemCount()>0) {
            for (BleDeviceInfo bleDeviceInfo: bleDeviceInformationAdapter.getAllBleDeviceInfo()) {
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
                if (bluetoothDevice.getName().toLowerCase().equals(bleDeviceInfo.getBluetoothDevice().getName().toLowerCase())) {
                    bleDeviceInfo.setBluetoothDevice(bluetoothDevice);
                    bleDeviceInfo.setConnectionStrength(rssi);
                    bleDeviceInformationAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
