package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.BluetoothDefaultDevice;

import java.util.List;

@Dao
public interface BluetoothDefaultDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BluetoothDefaultDevice bluetoothDefaultDevice);

    @Update
    void update(BluetoothDefaultDevice bluetoothDefaultDevice);

    @Delete
    void delete(BluetoothDefaultDevice bluetoothDefaultDevice);

    @Query("SELECT * FROM default_ble_device_table bt WHERE ble_id = :bleId")
    LiveData<List<BluetoothDefaultDevice>> getBluetoothDefaultDevice(final Integer bleId);

    @Query("SELECT * FROM default_ble_device_table bt WHERE ble_id = :bleId")
    List<BluetoothDefaultDevice> getRawBluetoothDefaultDevice(final Integer bleId);
}
