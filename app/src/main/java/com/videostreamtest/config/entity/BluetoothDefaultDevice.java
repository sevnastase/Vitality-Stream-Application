package com.videostreamtest.config.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "default_ble_device_table")
public class BluetoothDefaultDevice {
    @PrimaryKey
    @ColumnInfo(name="ble_id")
    private Integer bleId;

    @ColumnInfo(name="ble_address")
    private String bleAddress;
    @ColumnInfo(name="ble_name")
    private String bleName;
    @ColumnInfo(name="ble_signal_strength")
    private String bleSignalStrength;
    @ColumnInfo(name="ble_battery_level")
    private String bleBatterylevel;
    @ColumnInfo(name="ble_sensor_type")
    private String bleSensorType;

    public Integer getBleId() {
        return bleId;
    }

    public void setBleId(Integer bleId) {
        this.bleId = bleId;
    }

    public String getBleAddress() {
        return bleAddress;
    }

    public void setBleAddress(String bleAddress) {
        this.bleAddress = bleAddress;
    }

    public String getBleName() {
        return bleName;
    }

    public void setBleName(String bleName) {
        this.bleName = bleName;
    }

    public String getBleSignalStrength() {
        return bleSignalStrength;
    }

    public void setBleSignalStrength(String bleSignalStrength) {
        this.bleSignalStrength = bleSignalStrength;
    }

    public String getBleBatterylevel() {
        return bleBatterylevel;
    }

    public void setBleBatterylevel(String bleBatterylevel) {
        this.bleBatterylevel = bleBatterylevel;
    }

    public String getBleSensorType() {
        return bleSensorType;
    }

    public void setBleSensorType(String bleSensorType) {
        this.bleSensorType = bleSensorType;
    }
}
