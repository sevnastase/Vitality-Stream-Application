package com.videostreamtest.service.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.videostreamtest.R;
import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.ui.phone.profiles.ProfilesActivity;
import com.videostreamtest.utils.ApplicationSettings;

import java.util.HashSet;
import java.util.Set;

public class BleService extends Service {
    private static final String TAG = BleService.class.getSimpleName();

    private static final int ONGOING_NOTIFICATION_ID = 9999;
    private static final String CHANNEL_DEFAULT_IMPORTANCE = "csc_ble_channel";
    private static final String MAIN_CHANNEL_NAME = "CscService";

    // bluetooth API
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    // notification subscribers
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    // last wheel and crank (speed/cadence) information to send to CSCProfile
    private long cumulativeWheelRevolution = 0;
    private long cumulativeCrankRevolution = 0;
    private int lastWheelEventTime = 0;
    private int lastCrankEventTime = 0;
    private int lastCumCrankEventTime = 0;

    // for UI updates
    private long lastSpeedTimestamp = 0;
    private long lastCadenceTimestamp = 0;
    private long lastHRTimestamp = 0;
    private long lastSSDistanceTimestamp = 0;
    private long lastSSSpeedTimestamp = 0;
    private long lastSSStrideCountTimestamp = 0;
    private float lastSpeed = 0;
    private int lastCadence = 0;
    private int lastHR = 0;
    private long lastSSDistance = 0;
    private float lastSSSpeed = 0;
    private long lastStridePerMinute = 0;

    // for onCreate() failure case
    private boolean initialised = false;

    private int cadenceMeasurements[] = new int[10];
    private int cadenceMeasurementIndex = 0;

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        connectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                bluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        connectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.d(TAG, "onServicesDiscovered received: " + status);
                    Log.d(TAG, "Services Discovered: " + gatt.getServices().size());
                    if (gatt.getServices().size() >0) {
                        broadcastData(CadenceSensorConstants.BIKE_CADENCE_STATUS, "SEARCHING");
                    }

                    String serviceStatus = "DEAD";
                    for(BluetoothGattService bluetoothGattService : gatt.getServices()) {
                        Log.d(TAG, "Service Type: "+bluetoothGattService.getType() + " Service UUID: "+bluetoothGattService.getUuid());
                        if (CSCProfile.CSC_SERVICE.equals(bluetoothGattService.getUuid())) {

                            Log.d(TAG, "CSC SERVICE Charas: "+bluetoothGattService.getCharacteristics().size());
                            for (BluetoothGattCharacteristic characteristic : bluetoothGattService.getCharacteristics()) {
                                Log.d(TAG, "CHARACTERISTIC FOUND: "+characteristic.getUuid());
                                Log.d(TAG, "CHARACTERISTIC FOUND WITH VALUE: "+characteristic.getStringValue(0));
                            }
                            gatt.setCharacteristicNotification(bluetoothGattService.getCharacteristic(CSCProfile.CSC_MEASUREMENT), true);
                            Log.d(TAG, "CSC SERVICE FOUND, ACTIVATED NOTIFICATIONS NOW!");
                            for (BluetoothGattCharacteristic characteristic : bluetoothGattService.getCharacteristics()) {
                                Log.d(TAG, "CHARACTERISTIC FOUND: "+characteristic.getUuid());
                                Log.d(TAG, "CHARACTERISTIC FOUND WITH VALUE: "+characteristic.getStringValue(0));
                            }

                            Log.d(TAG, "DESCRIPTORS: "+bluetoothGattService.getCharacteristic(CSCProfile.CSC_MEASUREMENT).getDescriptors().size());
                            for (BluetoothGattDescriptor descriptor : bluetoothGattService.getCharacteristic(CSCProfile.CSC_MEASUREMENT).getDescriptors()) {
                                Log.d(TAG, "DESCRIPTOR FOUND: "+descriptor.getUuid());
                            }

                            BluetoothGattDescriptor descriptor = bluetoothGattService.getCharacteristic(CSCProfile.CSC_MEASUREMENT).getDescriptor(CSCProfile.CLIENT_CONFIG);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            serviceStatus = "SERVICE FOUND";
                        }
                    }
                    broadcastData(CadenceSensorConstants.BIKE_CADENCE_STATUS, serviceStatus);
                }

//                @Override
//                // Result of a characteristic read operation
//                public void onCharacteristicRead(BluetoothGatt gatt,
//                                                 BluetoothGattCharacteristic characteristic,
//                                                 int status) {
//                    Log.d(TAG, "Result broadcasted! "+characteristic.getStringValue(0));
//                    if (status == BluetoothGatt.GATT_SUCCESS) {
//                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//                    }
//                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    Log.d(TAG, "Notification set on charas: "+characteristic.getUuid().toString());

                    int flag = characteristic.getProperties();
                    int format = -1;

                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        Log.d(TAG, "CSC format UINT16.");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        Log.d(TAG, "CSC format UINT8.");
                    }

                    /**
                     * Profile values:
                     * 1 = Speed 0x1
                     * 2 = Cadence 0x2
                     * 3 = Both 0x3
                     */
                    Log.d(TAG, "PROFILE: "+ characteristic.getIntValue(format, 0));

                    if (characteristic.getIntValue(format, 0) >= 2) {
                        Log.d(TAG, "CHARACTERISTIC VALUE BYTE ARRAY LENGTH: "+characteristic.getValue().length);
                        int cumulativeRotations = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
                        Log.d(TAG, "CUMULATIVE CRANK EVENTS :: "+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1));

                        int cumulativeEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
                        if (lastCumCrankEventTime == 0) {
                            lastCumCrankEventTime = cumulativeEventTime;
                        } else
                        {
                            lastCrankEventTime = cumulativeEventTime - lastCumCrankEventTime;
                            lastCumCrankEventTime = cumulativeEventTime;
                            if (lastCrankEventTime > 0) {
                                float cyclesPerMilliSecond = 1 / Float.valueOf(""+lastCrankEventTime);
                                float cyclesPerSecond = cyclesPerMilliSecond * 1000;
                                float cyclesPerMinute = cyclesPerSecond * 60;
                                lastCadence = (int) cyclesPerMinute;
                            } else {
                                lastCadence = 0;
                            }
                            updateCadenceMeasurementList(lastCadence);
                            broadcastData(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, getMeasuredCadence());
                        }
                    }
                }
            };

    @Override
    public void onCreate() {
        Log.d(TAG, "BLE Service started");
        super.onCreate();

        // Bluetooth LE
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        // continue without proper Bluetooth support
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            Log.e(TAG, "Bluetooth LE isn't supported. This won't run");
            stopSelf();
            return;
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");

            for (BluetoothDevice bluetoothDevice : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
                Log.d(TAG, bluetoothDevice.getName());

                Log.d(TAG, String.valueOf(bluetoothDevice.getType()));
                if (bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                    Log.d(TAG, "Device "+bluetoothDevice.getName()+" supports BLE");
                }
                //TODO: Check if there are BLE devices connected which have the CSC Profile
                //TODO: Show BLE Devices which are nearby and support the CSC Profile
                //TODO: After onCLick the device connects
                if (bluetoothDevice.getName().contains("CAD-")) {
                    bluetoothGatt = bluetoothDevice.connectGatt(this, true, gattCallback);
                }
            }
        }
        initialised = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BLE Service onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_DEFAULT_IMPORTANCE, MAIN_CHANNEL_NAME);

            // Create the PendingIntent
            PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    new Intent(this.getApplicationContext(), ProfilesActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // build a notification
            Notification notification =
                    new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                            .setContentTitle(getText(R.string.app_name))
                            .setContentText("Active")
                            .setAutoCancel(true)
                            .setContentIntent(notifyPendingIntent)
                            .setTicker(getText(R.string.app_name))
                            .build();

            startForeground(ONGOING_NOTIFICATION_ID, notification);
        } else {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Active")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build();

            startForeground(ONGOING_NOTIFICATION_ID, notification);
        }
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (initialised) {
            // stop BLE
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
//                stopServer();
//                stopAdvertising();
            }
            unregisterReceiver(mBluetoothReceiver);
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.

        if (CSCProfile.HR_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                        stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
//                    startAdvertising();
//                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
//                    stopServer();
//                    stopAdvertising();
                    break;
            }
        }
    };

    private void updateCadenceMeasurementList(final int lastCadenceMeasured) {
        int listSize = cadenceMeasurements.length;
        if (cadenceMeasurementIndex < listSize) {
            cadenceMeasurements[cadenceMeasurementIndex] = lastCadenceMeasured;
        } else {
            cadenceMeasurementIndex = 0;
            cadenceMeasurements[cadenceMeasurementIndex] = lastCadenceMeasured;
        }
        cadenceMeasurementIndex++;
    }

    private int getMeasuredCadence() {
        int cadence = 0;
        int cumulativeMeasurements = 0;

        for (int measurement: cadenceMeasurements) {
            if (measurement > 0) {
                cadence += measurement;
                cumulativeMeasurements++;
            }
        }

        if (cumulativeMeasurements < 2) {
            return 0;
        }

        return cadence / cumulativeMeasurements;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }
        return true;
    }

    /**
     * Broadcast data with key/value pair of String:String
     * @param key String
     * @param value String
     */
    private void broadcastData(final String key, final String value) {
        Intent broadcastIntent = new Intent(ApplicationSettings.COMMUNICATION_INTENT_FILTER);
        broadcastIntent.putExtra(key, value);
        sendBroadcast(broadcastIntent);
    }
    /**
     * Broadcast data with key/value pair of String:int
     * @param key String
     * @param value String
     */
    private void broadcastData(final String key, final int value) {
        Intent broadcastIntent = new Intent(ApplicationSettings.COMMUNICATION_INTENT_FILTER);
        broadcastIntent.putExtra(key, value);
        sendBroadcast(broadcastIntent);
    }
}
