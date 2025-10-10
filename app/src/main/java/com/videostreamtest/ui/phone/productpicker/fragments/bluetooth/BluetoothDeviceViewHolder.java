package com.videostreamtest.ui.phone.productpicker.fragments.bluetooth;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;

import org.jetbrains.annotations.NotNull;

public class BluetoothDeviceViewHolder extends RecyclerView.ViewHolder {
    final static String TAG = BluetoothDeviceViewHolder.class.getSimpleName();
    Button connectButton;
    TextView deviceNameTextView;
    TextView connectionStrengthTextView;

    public BluetoothDeviceViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);

        connectButton = itemView.findViewById(R.id.single_ble_device_connect_button);
        deviceNameTextView = itemView.findViewById(R.id.single_ble_device_name);
        connectionStrengthTextView = itemView.findViewById(R.id.single_ble_device_connection_strength);
    }
}
