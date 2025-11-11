package com.videostreamtest.service.bluetooth;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.helpers.PermissionHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothService extends Service {
    private static final String TAG = BluetoothService.class.getSimpleName();

    public final static int CONNECTION_DELAY_MS = 4000;
    public final static int CLOSE_DELAY_AFTER_DISCONNECT_MS = 3000;
    public final static String ACTION_GATT_STARTED_CONNECTING =
            "com.videostreamtest.bluetooth.ACTION_GATT_STARTED_CONNECTING";
    public final static String ACTION_GATT_CONNECTED =
            "com.videostreamtest.bluetooth.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.videostreamtest.bluetooth.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.videostreamtest.bluetooth.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_RSSI_DATA_AVAILABLE =
            "com.videostreamtest.bluetooth.ACTION_RSSI_DATA_AVAILABLE";
    public final static String ACTION_BATTERY_DATA_AVAILABLE =
            "com.videostreamtest.bluetooth.ACTION_BATTERY_DATA_AVAILABLE";
    public final static String ACTION_RPM_DATA_AVAILABLE =
            "com.videostreamtest.bluetooth.ACTION_RPM_DATA_AVAILABLE";
    public final static String ACTION_SAVE_DEVICE =
            "com.videostreamtest.bluetooth.ACTION_SAVE_DEVICE";
    public final static String ACTION_TOAST_MESSAGE =
            "com.videostreamtest.bluetooth.ACTION_TOAST_MESSAGE";
    public final static String EXTRA_DATA =
            "com.videostreamtest.bluetooth.EXTRA_DATA";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;

    // DATA
    private float crankRevolution = 0;
    private int lastCrankEventTime = 0;
    private int lastCumCrankEventTime = 0;
    private int lastCadence = 0;
    private int cadenceMeasurements[] = new int[8];
    private int cadenceMeasurementIndex = 0;
    private final Handler gattOperationHandler = new Handler(Looper.getMainLooper());
    private final Runnable rssiUpdateRunnable = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            BluetoothGatt btGatt = BluetoothService.this.bluetoothGatt;
            if (btGatt != null && PermissionHelper.hasBluetoothPermissions(BluetoothService.this)) {
                try {
                    btGatt.readRemoteRssi();
                } catch (Exception e) {
                    Log.d(TAG, "RSSI read failed: " + e.getMessage());
                }
            }
            gattOperationHandler.postDelayed(this, CONNECTION_DELAY_MS);
        }
    };

    private class ConnectRunnable implements Runnable {
        private final BluetoothDevice bluetoothDevice;
        ConnectRunnable(BluetoothDevice bluetoothDevice) {
            this.bluetoothDevice = bluetoothDevice;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            if (!PermissionHelper.hasBluetoothPermissions(PraxtourApplication.getAppContext())) return;
            Log.d(TAG, "Trying to connect to " + bluetoothDevice.getAddress());
            bluetoothGatt = bluetoothDevice.connectGatt(getApplicationContext(), false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
            broadcastUpdate(ACTION_GATT_STARTED_CONNECTING);
        }
    }

    private ConnectRunnable connectRunnable;

    // BLUETOOTH
    private int connectionState;
    private boolean isConnecting;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String selectedDeviceAddress;

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        // Measured in unit of "1/1024 seconds"
        private int prevCumulativeCrankRevolution = 0;

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (!PermissionHelper.hasBluetoothPermissions(BluetoothService.this)) return;

            String message = "";
            BluetoothDevice bluetoothDevice = gatt.getDevice();
            isConnecting = false;

            if (bluetoothDevice != null && bluetoothDevice.getName() != null) {
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "\t connection to" + bluetoothDevice.getName() + "successful");
                    connectionState = STATE_CONNECTED;
                    bluetoothGatt = gatt;
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    ArrayList<String> info = new ArrayList<>(Arrays.asList(bluetoothDevice.getAddress(), bluetoothDevice.getName()));
                    broadcastStringListDataWithAction(EXTRA_DATA, info, ACTION_SAVE_DEVICE);
                    message = "Connected to " + bluetoothDevice.getName() + "!";
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected " + bluetoothDevice.getName());
                    gattOperationHandler.removeCallbacks(rssiUpdateRunnable);
                    connectionState = STATE_DISCONNECTED;
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    message = "Disconnected from " + bluetoothDevice.getName() + "!";

                    gattOperationHandler.postDelayed(() -> {
                        try { close(); } catch (Exception ignored) {}
                    }, CLOSE_DELAY_AFTER_DISCONNECT_MS);
                }
            }

            if (!message.isEmpty()) {
                broadcastStringDataWithAction(EXTRA_DATA, message, ACTION_TOAST_MESSAGE);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (!PermissionHelper.hasBluetoothPermissions(BluetoothService.this)) return;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> gattServices = getSupportedGattServices();
                if (gattServices == null) return;
                UUID serviceUuid;
                for (BluetoothGattService gattService : gattServices) {
                    serviceUuid = gattService.getUuid();
                    if (CSCProfile.CSC_SERVICE.equals(serviceUuid)) {
                        Log.d(TAG, "Cycling service found!");
                        BluetoothGattCharacteristic cyclingCharacteristic = gattService.getCharacteristic(CSCProfile.CSC_MEASUREMENT);
                        if (cyclingCharacteristic != null) {
                            setCharacteristicNotification(cyclingCharacteristic, true);
                        }
                    } else if (CSCProfile.BATTERY_SERVICE.equals(serviceUuid)) {
                        Log.d(TAG, "Battery service found!");
                        readCharacteristic(gattService.getCharacteristic(CSCProfile.BATTERY_MEASUREMENT));
                    } else {
                        Log.d(TAG, "Irrelevant service found, UUID: " + serviceUuid.toString());
                        continue;
                    }
                }

                gattOperationHandler.postDelayed(rssiUpdateRunnable, CONNECTION_DELAY_MS);
            }
        }

        /** Result of {@link BluetoothService#readCharacteristic(BluetoothGattCharacteristic)}. */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (CSCProfile.BATTERY_MEASUREMENT.equals(characteristic.getUuid())) {
                    int level = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    broadcastIntDataWithAction(EXTRA_DATA, level, ACTION_BATTERY_DATA_AVAILABLE);
                }
            }
        }

        /** Triggers for characteristics for which notifications were enabled using
         * {@link BluetoothService#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)} */
        @Override
        @SuppressLint("MissingPermission")
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged()");
            if (!PermissionHelper.hasBluetoothPermissions(BluetoothService.this)) return;

            BluetoothDevice device = gatt.getDevice();
            if (device == null) return;

            String deviceAddress = device.getAddress();
            String deviceName = device.getName();
            if (deviceAddress == null || !deviceAddress.equals(selectedDeviceAddress)) return;
            Log.d(TAG, "Selected Device Triggered");

            Log.d(TAG, "Notification set on chars: " + characteristic.getUuid().toString());
            Log.d(TAG, "Device Name: " + deviceName);
            Log.d(TAG, "Device Address: " + gatt.getDevice().getAddress());

            if (CSCProfile.CSC_MEASUREMENT.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data == null || data.length == 0) return;

                int offset = 0;

                int flags = data[offset++] & 0xFF;

                boolean wheelDataPresent = (flags & 0x01) != 0;
                boolean crankDataPresent = (flags & 0x02) != 0;

                if (wheelDataPresent) {
                    offset += 6;
                }

                if (crankDataPresent) {
                    int cumulativeCrankRevs = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                    offset += 2;
                    // Measured in unit of "1/1024 seconds"
                    int lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);

                    Log.d(TAG, "cumulativeCrankRevs: " + cumulativeCrankRevs);
                    Log.d(TAG, "lastCrankEventTime: " + lastCrankEventTime);

                    if (prevCumulativeCrankRevolution > 0) {
                        int revDiff = cumulativeCrankRevs - prevCumulativeCrankRevolution;
                        if (revDiff < 0) revDiff += 65536; // 16-bit rollover
                        int timeDiff = lastCrankEventTime - lastCumCrankEventTime;

                        // Handle 16-bit rollover
                        if (timeDiff < 0) {
                            timeDiff += 65536;
                            Log.d(TAG, "Event time rollover detected");
                        }

                        // Convert time units from 1/1024 s → seconds
                        double timeSeconds = timeDiff / 1024.0;

                        if (revDiff > 0 && timeSeconds > 0) {
                            double cadenceRpm = (revDiff / timeSeconds) * 60.0;
                            lastCadence = (int) cadenceRpm;
                        } else {
                            lastCadence = 0;
                        }

                        Log.d(TAG, "CADENCE: " + lastCadence);
                        updateCadenceMeasurementList(lastCadence);
                        broadcastIntDataWithAction(
                                CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE,
                                getLastMeasuredCadenceValue(),
                                ACTION_RPM_DATA_AVAILABLE
                        );
                    }

                    prevCumulativeCrankRevolution = cumulativeCrankRevs;
                    lastCumCrankEventTime = lastCrankEventTime;
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastIntDataWithAction(EXTRA_DATA, rssi, ACTION_RSSI_DATA_AVAILABLE);
            }
        }
    };

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        selectedDeviceAddress = intent.getStringExtra("selected_device_address");

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private Binder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");
        gattOperationHandler.removeCallbacks(rssiUpdateRunnable);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        gattOperationHandler.removeCallbacksAndMessages(null);
        try { disconnect(); } catch (Exception ignored) {}
        // close() will be called upon successful disconnect

        stopSelf();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }
    public BluetoothService() {

    }

    public boolean init() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(TAG, "No bluetooth adapter");
            return false;
        }

        return true;
    }

    @SuppressLint("MissingPermission")
    public boolean connect(String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.d(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (!PermissionHelper.hasBluetoothPermissions(this)) return false;

        if (isConnecting) return false;

        try {
            Log.d(TAG, "Attempting connection to address " + address);
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
//            close(); // close any previous connections
            isConnecting = true;
            connectRunnable = new ConnectRunnable(device);
            gattOperationHandler.postDelayed(connectRunnable, CONNECTION_DELAY_MS);
            return true;
        } catch (IllegalArgumentException exception) {
            Log.d(TAG, "Device not found with provided address.");
            bluetoothGatt = null;
            isConnecting = false;
            gattOperationHandler.removeCallbacks(connectRunnable);
            return false;
        }
    }

    /**
     * Should only be called after {@code onServicesDiscovered()} of {@code this#bluetoothGattCallback}
     * has been called.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }

    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!PermissionHelper.hasBluetoothPermissions(this)) return;
        if (bluetoothGatt == null) {
            Log.d(TAG, "BluetoothGatt not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (!PermissionHelper.hasBluetoothPermissions(this) || bluetoothGatt == null) {
            return;
        }

        if (bluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {
            Log.d(TAG, String.format("REQUEST SENT TO %s notifications for characteristic %s"
                    , enabled ? "<ENABLE>" : "<DISABLE>", characteristic.getUuid().toString()));
        } else {
            Log.d(TAG, String.format("FAILED TO SEND REQUEST TO %s notifications for characteristic %s"
                    , enabled ? "<ENABLE>" : "<DISABLE>", characteristic.getUuid().toString()));
        }

        if (CSCProfile.CSC_MEASUREMENT.equals(characteristic.getUuid())) {
            Log.d(TAG, "Getting descriptor for cycling");
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CSCProfile.CLIENT_CONFIG);
            if (descriptor != null) {
                Log.d(TAG, "Writing descriptor");
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
            }
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastStringListDataWithAction(final String key, final ArrayList<String> values, final String action) {
        Intent broadcastIntent = new Intent(action);
        broadcastIntent.putStringArrayListExtra(key, values);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastStringDataWithAction(final String key, final String value, final String action) {
        Intent broadcastIntent = new Intent(action);
        broadcastIntent.putExtra(key, value);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastIntDataWithAction(final String key, final int value, final String action) {
        Intent broadcastIntent = new Intent(action);
        broadcastIntent.putExtra(key, value);
        sendBroadcast(broadcastIntent);
    }

    private void updateCadenceMeasurementList(final int lastCadenceMeasured) {
        int listSize = cadenceMeasurements.length;
        if (cadenceMeasurementIndex >= listSize) {
            cadenceMeasurementIndex = 0;
        }
        cadenceMeasurements[cadenceMeasurementIndex] = lastCadenceMeasured;
        cadenceMeasurementIndex++;
    }

    private int getLastMeasuredCadenceValue() {
        int cadenceValue = 0;

        // Set starting point to get last value above 0
        int measurementIndex = cadenceMeasurementIndex;
        if (measurementIndex == 0) {
            measurementIndex = cadenceMeasurements.length - 1;
        } else {
            measurementIndex = cadenceMeasurementIndex - 1;
        }

        for (int cIndex = 0; cIndex < cadenceMeasurements.length; cIndex++ ) {
            if (cadenceValue > 0) {
                break;
            }
            if (cadenceMeasurements[measurementIndex] > 0 ) {
                cadenceValue = cadenceMeasurements[measurementIndex];
            }
            if (measurementIndex == 0){
                measurementIndex = cadenceMeasurements.length -1;
            } else {
                measurementIndex--;
            }
        }

        return isSensorHalted() ? 0 : cadenceValue;
    }

    private boolean isSensorHalted() {
        int cumulativeMeasurements = 0;
        for (int measurement: cadenceMeasurements) {
            if (measurement > 0) {
                cumulativeMeasurements++;
            }
        }
        return (cumulativeMeasurements < 2);
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        Log.d(TAG, "Disconnecting GATT");
        if (bluetoothGatt != null && PermissionHelper.hasBluetoothPermissions(this)) {
            bluetoothGatt.disconnect();
        }
    }

    @SuppressLint("MissingPermission")
    private void close() {
        Log.d(TAG, "Closing GATT");
        if (bluetoothGatt != null) {
            try { bluetoothGatt.close(); } catch (Exception ignored) {}
            bluetoothGatt = null;
        }
    }
}