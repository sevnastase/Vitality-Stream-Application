package com.videostreamtest.data.model;

public class BluetoothDeviceInfo {
    String address;

    String name;
    int connectionStrength;

    public BluetoothDeviceInfo(String address, String name, int connectionStrength) {
        this.address = address;
        this.name = name;
        this.connectionStrength = connectionStrength;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getConnectionStrength() {
        return connectionStrength;
    }

    public void setConnectionStrength(int connectionStrength) {
        this.connectionStrength = connectionStrength;
    }
}
