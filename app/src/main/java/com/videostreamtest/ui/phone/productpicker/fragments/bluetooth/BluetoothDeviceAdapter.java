package com.videostreamtest.ui.phone.productpicker.fragments.bluetooth;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.data.model.BleDeviceInfo;
import com.videostreamtest.data.model.BluetoothDeviceInfo;
import com.videostreamtest.ui.phone.helpers.BleHelper;
import com.videostreamtest.ui.phone.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceViewHolder> {
    private final static String TAG = BluetoothDeviceAdapter.class.getSimpleName();

    private List<BluetoothDeviceInfo> bluetoothDevices;
    private int selectedPosition = 0;
    private BluetoothConnectionFragment.OnDeviceSelectedListener onDeviceSelectedListener;

    private ProductViewModel productViewModel;

    public BluetoothDeviceAdapter(final ProductViewModel productViewModel,
                                  final BluetoothConnectionFragment.OnDeviceSelectedListener onDeviceSelectedListener) {
        this.productViewModel = productViewModel;
        this.bluetoothDevices = new ArrayList<>();
        this.onDeviceSelectedListener = onDeviceSelectedListener;
    }

    @NonNull
    @NotNull
    @Override
    public BluetoothDeviceViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_bluetooth_device, parent, false);
        return new BluetoothDeviceViewHolder(view);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull @NotNull BluetoothDeviceViewHolder holder, int position) {
        holder.itemView.setSelected(selectedPosition == position);

        BluetoothDeviceInfo device = bluetoothDevices.get(position);

        Log.d(TAG, String.format("Device name: %s \n Device strength: %s",
                device.getName(),
                BleHelper.getRssiStrengthIndicator(PraxtourApplication.getAppContext(), device.getConnectionStrength())));

        holder.deviceNameTextView.setText(device.getName());
        holder.connectionStrengthTextView.setText(BleHelper.getRssiStrengthIndicator(PraxtourApplication.getAppContext(), device.getConnectionStrength()));

        if (selectedPosition == position) {
            holder.connectButton.requestFocus();
        }

        holder.connectButton.setOnClickListener(view -> {
            onDeviceSelectedListener.connectToDevice(bluetoothDevices.get(position));
        });
    }

    /**
     * Adds the devices from {@code newDevices} that are not already in {@code bluetoothDevices}.
     * After updating the list, it notifies the recyclerview of the update.
     */
    public void addDevices(List<BluetoothDeviceInfo> newDevices) {
        Set<String> existingAddresses = this.bluetoothDevices.stream()
                .map(device -> device.getAddress())
                .collect(Collectors.toSet());

        List<BluetoothDeviceInfo> filteredNewDevices = newDevices.stream()
                .filter(device -> !existingAddresses.contains(device.getAddress()))
                .collect(Collectors.toList());

        if (!filteredNewDevices.isEmpty()) {
            this.bluetoothDevices.addAll(filteredNewDevices);
            notifyDataSetChanged();
        }
    }

    public void clearDeviceList() {
        this.bluetoothDevices.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return bluetoothDevices == null ? 0 : bluetoothDevices.size();
    }
}
