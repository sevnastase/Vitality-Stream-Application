package com.videostreamtest.data.model;

import android.bluetooth.BluetoothDevice;

public class BleDeviceInfo {
    private BluetoothDevice bluetoothDevice;
    private String deviceType;
    private int connectionStrength;

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public int getConnectionStrength() {
        return connectionStrength;
    }

    public void setConnectionStrength(int connectionStrength) {
        this.connectionStrength = connectionStrength;
    }
}
