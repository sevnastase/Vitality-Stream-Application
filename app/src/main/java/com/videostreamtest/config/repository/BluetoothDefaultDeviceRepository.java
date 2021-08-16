package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.BluetoothDefaultDeviceDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;

import java.util.List;

public class BluetoothDefaultDeviceRepository {
    private BluetoothDefaultDeviceDao bluetoothDefaultDeviceDao;

    public BluetoothDefaultDeviceRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        bluetoothDefaultDeviceDao = praxtourDatabase.bluetoothDefaultDeviceDao();
    }

    public LiveData<List<BluetoothDefaultDevice>> getBluetoothDefaultDevice() {
        return bluetoothDefaultDeviceDao.getBluetoothDefaultDevice(1);
    }

    public void insertBluetoothDefaultDevice(final BluetoothDefaultDevice bluetoothDefaultDevice) {
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            bluetoothDefaultDeviceDao.insert(bluetoothDefaultDevice);
        });
    }
}
