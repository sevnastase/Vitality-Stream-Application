package com.videostreamtest.ui.phone.helpers;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.videostreamtest.R;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.utils.ApplicationSettings;

import java.util.ArrayList;
import java.util.List;

public class BleHelper {

    private static List<BluetoothGatt> bluetoothGattList = new ArrayList<>();

    public static String getRssiStrengthIndicator(final Context context, final int rssi) {
        String indication = context.getString(R.string.signal_strength_indicator_no_signal_value);
        int normalizedRssi = rssi * -1;
        if (normalizedRssi<=50) {
            indication = context.getString(R.string.signal_strength_indicator_excellent_signal_value);
        }
        if (normalizedRssi>50 && normalizedRssi <=60) {
            indication = context.getString(R.string.signal_strength_indicator_very_good_signal_value);
        }
        if (normalizedRssi>60 && normalizedRssi <=70) {
            indication = context.getString(R.string.signal_strength_indicator_good_signal_value);
        }
        if (normalizedRssi>70 && normalizedRssi <=80) {
            indication = context.getString(R.string.signal_strength_indicator_low_signal_value);
        }
        if (normalizedRssi>80 && normalizedRssi <=90) {
            indication = context.getString(R.string.signal_strength_indicator_very_low_signal_value);
        }
        return indication;
    }

    public static void startBleService(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE);
        final String deviceAddress = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY, "NONE");
        if (deviceAddress != null && !deviceAddress.equals("NONE")) {
            Intent bleService = new Intent(context, BleService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(bleService);
            } else {
                context.startService(bleService);
            }
        }
    }

    public static void addBluetoothGatt(final BluetoothGatt bluetoothGatt) {
        if (bluetoothGattList!=null && bluetoothGattList.size()<1) {
            bluetoothGattList.add(bluetoothGatt);
        }
    }

    public static void removeBluetoothGatt() {
        if (bluetoothGattList!=null && bluetoothGattList.size()<1) {
            bluetoothGattList.remove(0);
        }
    }

    public static BluetoothGatt getCurrentBluetoothGatt() {
        if (bluetoothGattList!=null && bluetoothGattList.size()<1) {
            return bluetoothGattList.get(0);
        }
        return null;
    }
}
