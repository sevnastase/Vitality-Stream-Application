package com.videostreamtest.utils.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.annotation.NonNull;

import javax.annotation.Nullable;

public class BluetoothUtil {
    /**
     * Returns the instance of {@link BluetoothAdapter} if available.
     * If the device has no bluetooth adapter, this method return {@code null}.
     */
    @Nullable
    public static BluetoothAdapter getBluetoothAdapter(@NonNull Context context) {
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        return bluetoothManager.getAdapter();
    }

    public static boolean isBluetoothAvailableOnDevice(@NonNull Context context) {
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        return bluetoothAdapter != null;
    }
}
