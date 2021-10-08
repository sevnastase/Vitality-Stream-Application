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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.ui.phone.helpers.BleHelper;
import com.videostreamtest.ui.phone.profiles.ProfilesActivity;
import com.videostreamtest.utils.ApplicationSettings;

import java.util.ArrayList;
import java.util.List;

public class BleService extends Service {
    private static final String TAG = BleService.class.getSimpleName();

    private Binder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    private static final int ONGOING_NOTIFICATION_ID = 9999;
    private static final String CHANNEL_DEFAULT_IMPORTANCE = "csc_ble_channel";
    private static final String MAIN_CHANNEL_NAME = "CscService";

    private List<BluetoothDefaultDevice> bluetoothDefaultDeviceList;

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
    private List<BluetoothGattService> mRegisteredServices = new ArrayList<>();
    private List<BluetoothGattCharacteristic> mRegisteredCharacteristics = new ArrayList<>();

    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    // last wheel and crank (speed/cadence) information to send to CSCProfile
    private long cumulativeWheelRevolution = 0;
    private long cumulativeCrankRevolution = 0;
    private float crankRevolution = 0;

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

    private int cadenceMeasurements[] = new int[8];
    private int cadenceMeasurementIndex = 0;

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {

                private long lastCumulativeCrankRevolution = 0;
                private BluetoothGattCharacteristic mbatteryCharacteristic;

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        connectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        if (gatt.getDevice()!=null) {
                            Log.i(TAG, "Connected to GATT server {" + gatt.getDevice().getName() + "}");
                        }
                        Log.i(TAG, "Attempting to start service discovery:" +
                                gatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        connectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                        gatt.close();
                        bluetoothGatt = null;
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.d(TAG, "onServicesDiscovered received: " + status);
                    Log.d(TAG, "Services Discovered: " + gatt.getServices().size());

                    String serviceStatus = "ACTIVE";
                    for(BluetoothGattService bluetoothGattService : gatt.getServices()) {
                        Log.d(TAG, "Service Type: "+bluetoothGattService.getType() + " Service UUID: "+bluetoothGattService.getUuid());

                        if (CSCProfile.BATTERY_SERVICE.equals(bluetoothGattService.getUuid())) {
                            mRegisteredServices.add(bluetoothGattService);
                            Log.d(TAG, "BATTERY SERVICE Charas: "+bluetoothGattService.getCharacteristics().size());
                            mbatteryCharacteristic = bluetoothGattService.getCharacteristic(CSCProfile.BATTERY_MEASUREMENT);
                        }

                        if (CSCProfile.RSC_SERVICE.equals(bluetoothGattService.getUuid())) {
                            mRegisteredServices.add(bluetoothGattService);
                            Log.d(TAG, "RSC SERVICE Charas: "+bluetoothGattService.getCharacteristics().size());
                            gatt.setCharacteristicNotification(bluetoothGattService.getCharacteristic(CSCProfile.RSC_MEASUREMENT), true);
                            BluetoothGattDescriptor descriptor = bluetoothGattService.getCharacteristic(CSCProfile.RSC_MEASUREMENT).getDescriptor(CSCProfile.CLIENT_CONFIG);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }

                        if (CSCProfile.CSC_SERVICE.equals(bluetoothGattService.getUuid())) {
                            mRegisteredServices.add(bluetoothGattService);
                            Log.d(TAG, "CSC SERVICE Charas: "+bluetoothGattService.getCharacteristics().size());

                            boolean activateCSC = gatt.setCharacteristicNotification(bluetoothGattService.getCharacteristic(CSCProfile.CSC_MEASUREMENT), true);

                            if (activateCSC) {
                                Log.d(TAG, "CSC SERVICE FOUND, ACTIVATED NOTIFICATIONS NOW!");
                            }

                            BluetoothGattDescriptor descriptor = bluetoothGattService.getCharacteristic(CSCProfile.CSC_MEASUREMENT).getDescriptor(CSCProfile.CLIENT_CONFIG);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }

                    broadcastData(CadenceSensorConstants.BIKE_CADENCE_STATUS, serviceStatus);
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {

                        if (characteristic.getUuid().equals(CSCProfile.BATTERY_MEASUREMENT)) {
                            Log.d(TAG,"BATTERY_SERVICE: "+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0));
                            List<BluetoothDefaultDevice> bluetoothDefaultDevices = PraxtourDatabase.getDatabase(getApplicationContext()).bluetoothDefaultDeviceDao().getRawBluetoothDefaultDevice(1);
                            if (bluetoothDefaultDevices!= null && bluetoothDefaultDevices.size()>0) {
                                bluetoothDefaultDevices.get(0).setBleBatterylevel(""+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0));
                                PraxtourDatabase.getDatabase(getApplicationContext()).bluetoothDefaultDeviceDao().insert(bluetoothDefaultDevices.get(0));
                            };
                        }

                        Log.d(TAG, "READ SUCCES UUID: "+characteristic.getUuid()+" > "+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0));
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    } else {
                        Log.e(TAG, "Status code: "+status);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    Log.d(TAG, "Notification set on chars: " + characteristic.getUuid().toString());

                    Log.d(TAG, "Device Name: " + gatt.getDevice().getName());
                    Log.d(TAG, "Device Address: " + gatt.getDevice().getAddress());

                    if (bluetoothDeviceAddress!= null && bluetoothDeviceAddress != "" &&
                            gatt.getDevice().getAddress()==bluetoothDeviceAddress) {
                        Log.d(TAG, "Selected Device Triggered");

                        if (characteristic.getUuid().equals(CSCProfile.RSC_MEASUREMENT)) {
                            Log.d(TAG, "RUNNING SERVICE CHAR CHANGED");

                            int offset = 0;
                            final int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
                            final boolean instantaneousStrideLengthPresent = (flags & 0x01) != 0;
                            final boolean totalDistancePresent = (flags & 0x02) != 0;
                            final boolean statusRunning = (flags & 0x04) != 0;
                            offset += 1;

                            final float speed = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset) / 256.f; // [m/s]
                            offset += 2;
                            final int cadence = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
                            offset += 1;

                            Log.d(TAG, "instantaneousStrideLengthPresent: "+instantaneousStrideLengthPresent + " > totalDistancePresent: "+totalDistancePresent+" statusRunning > "+statusRunning+" Speed: "+speed+" cadence: "+cadence);

                            Integer strideLength = null;
                            if (instantaneousStrideLengthPresent) {
                                strideLength = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                                offset += 2;
                            }
                            Log.d(TAG, "strideLength: "+strideLength);

                            Float totalDistance = null;
                            if (totalDistancePresent) {
                                totalDistance = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
                                // offset += 4;
                            }
                            Log.d(TAG, "totalDistance: "+totalDistance);
                            updateCadenceMeasurementList(cadence);
                            broadcastData(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, getLastMeasuredCadenceValue());
                        }

                        if (characteristic.getUuid().equals(CSCProfile.CSC_MEASUREMENT)) {
                            Log.d(TAG, "CYCLING SERVICE CHAR CHANGED");

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
                            Log.d(TAG, "PROFILE: " + characteristic.getIntValue(format, 0));

                            if (characteristic.getIntValue(format, 0) >= 2) {
                                int cumulativeRotations = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
                                if (lastCumulativeCrankRevolution > 0) {
                                    crankRevolution = cumulativeRotations - lastCumulativeCrankRevolution;
                                }
                                lastCumulativeCrankRevolution = cumulativeRotations;

                                int cumulativeEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
                                if (lastCumCrankEventTime == 0) {
                                    lastCumCrankEventTime = cumulativeEventTime;
                                } else {
                                    if (lastCumCrankEventTime>cumulativeEventTime) {
                                        int maxValue = 65536;
                                        lastCrankEventTime = (maxValue - lastCumCrankEventTime) + cumulativeEventTime;
                                        Log.d(TAG, "Threshold overload!");
                                        Log.d(TAG, "lastCumCrankEventTime {"+lastCumCrankEventTime+"?} > cumulativeEventTime {"+cumulativeEventTime+"}");
                                    }
                                    else {
                                        lastCrankEventTime = cumulativeEventTime - lastCumCrankEventTime;
                                    }
                                    lastCumCrankEventTime = cumulativeEventTime;

                                    if (crankRevolution > 0 && lastCrankEventTime > 0) {
                                        float cyclesPerMilliSecond = crankRevolution / Float.valueOf("" + lastCrankEventTime);
                                        float cyclesPerSecond = cyclesPerMilliSecond * 1000;
                                        float cyclesPerMinute = cyclesPerSecond * 60;
                                        lastCadence = (int) cyclesPerMinute;
                                    } else {
                                        lastCadence = 0;
                                    }
                                    Log.d(TAG, "CADENCE: "+lastCadence);
                                    Log.d(TAG, "cumulativeRotations: "+cumulativeRotations);
                                    Log.d(TAG, "cumulativeEventTime: "+cumulativeEventTime);

                                    updateCadenceMeasurementList(lastCadence);
                                    broadcastData(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, getLastMeasuredCadenceValue());
                                }
                            }
                        }
                        gatt.readRemoteRssi();
                        if (mbatteryCharacteristic !=null) {
                            gatt.readCharacteristic(mbatteryCharacteristic);
                        }
                    } else {
                        Log.d(TAG, "Non-Selected Device Triggered...");
                    }
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    SharedPreferences sharedPreferences = getSharedPreferences("app",MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(ApplicationSettings.DEFAULT_BLE_DEVICE_CONNECTION_STRENGTH_KEY, BleHelper.getRssiStrengthIndicator(getApplicationContext(), rssi));
                    editor.commit();

                    List<BluetoothDefaultDevice> bluetoothDefaultDevices = PraxtourDatabase.getDatabase(getApplicationContext()).bluetoothDefaultDeviceDao().getRawBluetoothDefaultDevice(1);
                    if (bluetoothDefaultDevices!= null && bluetoothDefaultDevices.size()>0) {
                        bluetoothDefaultDevices.get(0).setBleSignalStrength(BleHelper.getRssiStrengthIndicator(getApplicationContext(), rssi));
                        PraxtourDatabase.getDatabase(getApplicationContext()).bluetoothDefaultDeviceDao().insert(bluetoothDefaultDevices.get(0));
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BLE Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (!checkBluetoothSupport(bluetoothAdapter)) {
            Log.e(TAG, "Bluetooth LE isn't supported. This won't run");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        }
        initialised = true;
        Log.d(TAG, "Bluetooth enabled...");

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

        bluetoothDefaultDeviceList = new ArrayList<>();
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            bluetoothDefaultDeviceList = PraxtourDatabase.getDatabase(getApplicationContext()).bluetoothDefaultDeviceDao().getRawBluetoothDefaultDevice(1);
            Log.d(TAG, "BLE DEFAULT DEVICE Size: "+bluetoothDefaultDeviceList.size());
            String bleDeviceAddress = "";
            if (bluetoothDefaultDeviceList.size()>0) {
                bleDeviceAddress = bluetoothDefaultDeviceList.get(0).getBleAddress();
                Log.d(TAG, "Address/Name of Default BLE Device: "+bluetoothDefaultDeviceList.get(0).getBleAddress()+"/"+bluetoothDefaultDeviceList.get(0).getBleName());
            }

            final String finalizedBleDeviceAddress = bleDeviceAddress;
            bluetoothDeviceAddress = bleDeviceAddress;

//            try {
//
//                final BluetoothDevice sensorDevice = bluetoothAdapter.getRemoteDevice(bleDeviceAddress);
//                if (bluetoothGatt != null && bluetoothGatt.getDevice() !=null
//                && bluetoothGatt.getDevice().getAddress() !=null && bluetoothGatt.getDevice().getAddress() != bleDeviceAddress) {
//                    bluetoothGatt.disconnect();
//                }
//                if (bluetoothGatt == null) {
//                    bluetoothGatt = sensorDevice.connectGatt(getApplicationContext(), true, gattCallback, BluetoothDevice.TRANSPORT_LE);
//                }
//            } catch (IllegalArgumentException exception) {
//                Log.w(TAG, "Device not found with provided address.");
//            }

            scanner = bluetoothAdapter.getBluetoothLeScanner();

            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {

                    if(result != null && result.getDevice()!=null && result.getDevice().getAddress() != null) {
                        if (finalizedBleDeviceAddress.equals(result.getDevice().getAddress())) {
                            if (bluetoothGatt == null) {
                                bluetoothGatt = result.getDevice().connectGatt(getApplicationContext(), true, gattCallback, BluetoothDevice.TRANSPORT_LE);
                            }
                        }
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    Log.d(TAG, "ScanBatchResult :: "+results.size());
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.d(TAG, "ScanFailedResult :: "+errorCode);
                }
            };

            scanner.startScan(scanCallback);

        });

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service stopped/destroyed.");
        if (initialised) {
            // stop BLE
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothGatt!=null) {
                bluetoothGatt.disconnect();
            }
            if (scanner != null) {
                scanner.stopScan(scanCallback);
            }
            stopForeground(true);
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

    private boolean isSensorHalted()
    {
        int cumulativeMeasurements = 0;
        for (int measurement: cadenceMeasurements) {
            if (measurement > 0) {
                cumulativeMeasurements++;
            }
        }
        return (cumulativeMeasurements < 2);
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

    private int getLastMeasuredCadenceValue() {
        int cadenceValue = 0;

        //Set starting point to get last value above 0
        int measurementIndex = cadenceMeasurementIndex;
        if (measurementIndex == 0) {
            measurementIndex = cadenceMeasurements.length -1;
        } else {
            measurementIndex = cadenceMeasurementIndex -1;
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

        if (isSensorHalted()) {
            return 0;
        } else {
            return cadenceValue;
        }
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
