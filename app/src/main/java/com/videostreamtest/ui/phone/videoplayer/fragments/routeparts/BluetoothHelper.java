package com.videostreamtest.ui.phone.videoplayer.fragments.routeparts;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
public class BluetoothHelper {

    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private BluetoothDeviceListener listener;

    // Define a request code for requesting Bluetooth permissions
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    public interface BluetoothDeviceListener {
        void onDeviceFound(BluetoothDevice device);
        void onDevicePaired(BluetoothDevice device);
        // ... other callback methods as needed
    }

    public BluetoothHelper(Context context, BluetoothDeviceListener listener) {
        this.context = context;
        this.listener = listener;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            // Check for permissions before starting discovery
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                context.registerReceiver(receiver, filter);
                bluetoothAdapter.startDiscovery();
            } else {
                // Request the necessary permissions if not already granted
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_FINE_LOCATION);
            }
        }
    }

    public void stopDiscovery() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            context.unregisterReceiver(receiver);
            if (bluetoothAdapter != null) {
                bluetoothAdapter.cancelDiscovery();
            }
        } else {
            // Log or handle the case where you don't have permission.
        }
    }

    // Call this method from the fragment to handle the permission result
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the discovery
                startDiscovery();
            } else {
                // Permission was denied, handle the failure
                // You could notify the user that the permission is necessary and retry
            }
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                listener.onDeviceFound(device);
            }
        }
    };

}
