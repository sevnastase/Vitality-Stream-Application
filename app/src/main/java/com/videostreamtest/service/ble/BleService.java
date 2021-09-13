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
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.StateSet;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BleService extends Service {
    private static final String TAG = BleService.class.getSimpleName();

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
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();
    private List<BluetoothGattService> mRegisteredServices = new ArrayList<>();

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
                        Log.i(TAG, "Attempting to start service discovery:" +
                                gatt.discoverServices());

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

                        if (CSCProfile.BATTERY_SERVICE.equals(bluetoothGattService.getUuid())) {
                            mRegisteredServices.add(bluetoothGattService);
                            Log.d(TAG, "BATTERY SERVICE Charas: "+bluetoothGattService.getCharacteristics().size());
//                            gatt.setCharacteristicNotification(bluetoothGattService.getCharacteristic(CSCProfile.BATTERY_MEASUREMENT), true);
//                            BluetoothGattDescriptor descriptor = bluetoothGattService.getCharacteristic(CSCProfile.BATTERY_MEASUREMENT).getDescriptor(CSCProfile.CLIENT_CONFIG);
//                            if (descriptor != null) {
//                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                boolean writeDescriptor = gatt.writeDescriptor(descriptor);
//                                if (writeDescriptor) {
//                                    Log.d(TAG, "WRITTEN DESCRIPTOR ON BATTERY SERVICE");
//                                }
//                            }
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
                            serviceStatus = "SERVICE FOUND";
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
                        //TODO: USE OBJECT AND STRINGIFY THE OBJECT(JSON) FOR USE IN THE BROADCASTRECEIVER
//                        broadcastData(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, getLastMeasuredCadenceValue());
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
    //                        Log.d(TAG, "CHARACTERISTIC VALUE BYTE ARRAY LENGTH: "+characteristic.getValue().length);

                            int cumulativeRotations = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
                            if (lastCumulativeCrankRevolution > 0) {
                                crankRevolution = cumulativeRotations - lastCumulativeCrankRevolution;
                            }
                            lastCumulativeCrankRevolution = cumulativeRotations;

                            int cumulativeEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
                            if (lastCumCrankEventTime == 0) {
                                lastCumCrankEventTime = cumulativeEventTime;
                            } else {
                                lastCrankEventTime = cumulativeEventTime - lastCumCrankEventTime;
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

                                updateCadenceMeasurementList(lastCadence);
                                broadcastData(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, getLastMeasuredCadenceValue());
                            }
                        }
                    }
                    gatt.readRemoteRssi();
                    if (mbatteryCharacteristic !=null) {
                        gatt.readCharacteristic(mbatteryCharacteristic);
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
        Log.d(TAG, "BLE Service started");

        // Bluetooth LE
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        bluetoothAdapter = bluetoothManager.getAdapter();
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

            //Check already bound devices for supports
//            for (BluetoothDevice bluetoothDevice : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
//                Log.d(TAG, bluetoothDevice.getName());
//
//                Log.d(TAG, String.valueOf(bluetoothDevice.getType()));
//                if (bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
//                    Log.d(TAG, "Device "+bluetoothDevice.getName()+" supports BLE");
//                }
//                //TODO: Check if there are BLE devices connected which have the CSC Profile
//                //TODO: Show BLE Devices which are nearby and support the CSC Profile
//                //TODO: After onCLick the device connects
//
//                ParcelUuid parcelUuidList[] = bluetoothDevice.getUuids();
//                if (parcelUuidList != null && parcelUuidList.length>0) {
//                    for (ParcelUuid parcelUuid : parcelUuidList) {
//                        Log.d(TAG, "ParcelUuid : "+parcelUuid);
//                        if (parcelUuid.getUuid().equals(CSCProfile.CSC_SERVICE)) {
//                            Log.d(TAG, "Bike Cadence Found ! > "+bluetoothDevice.getName());
//                        }
//                        if (parcelUuid.getUuid().equals(CSCProfile.RSC_SERVICE)) {
//                            Log.d(TAG, "Running Cadence Found! > "+bluetoothDevice.getName());
//                        }
//                    }
//                }
//
////                if (bluetoothDevice.getName().contains("CAD-")) {
////                    bluetoothGatt = bluetoothDevice.connectGatt(this, true, gattCallback);
////                }
//            }

//            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
//
//            scanner.startScan(new ScanCallback() {
//                @Override
//                public void onScanResult(int callbackType, ScanResult result) {
////                    super.onScanResult(callbackType, result);
////                    Log.d(TAG, "ScanResult :: "+result.toString());
//
//                    if (bluetoothGatt != null) {
//                        scanner.stopScan(this);
//                        return;
//                    }
//
//                    if(result != null && result.getDevice()!=null && result.getDevice().getName()!=null) {
//                        Log.d(TAG, "ScanResult NAME:: " + result.getDevice().getName());
//                        if (result.getScanRecord().getServiceUuids()!= null) {
//                            Log.d(TAG, "ScanResult SIZE :: " + result.getScanRecord().getServiceUuids().size());
//                            if (result.getScanRecord().getServiceUuids().size() > 0 ) {
//                                for (ParcelUuid parcelUuid: result.getScanRecord().getServiceUuids()) {
//                                    Log.d(TAG, parcelUuid.toString());
//                                    //UNKNOWN UUID GATT SERVICE > 0000fc00-0000-1000-8000-00805f9b34fb
//                                    if (parcelUuid.getUuid().equals(CSCProfile.RSC_SERVICE)) {
//                                        Log.d(TAG, "ScanResult :: Running sensor found ::> " + result.getDevice().getName());
//                                    }
//                                    if (parcelUuid.getUuid().equals(CSCProfile.CSC_SERVICE)) {
//                                        Log.d(TAG, "ScanResult :: CYCLING sensor found ::> " + result.getDevice().getName());
////                                        bluetoothGatt = result.getDevice().connectGatt(getApplicationContext(), true, gattCallback);
//                                    }
//                                    if (result.getDevice().getName().toLowerCase().contains("move")) {
//                                        bluetoothGatt = result.getDevice().connectGatt(getApplicationContext(), true, gattCallback);
//                                    }
//                                    Log.d(TAG, "RSSI: "+result.getRssi());
//                                }
//                            }
//                        }
//                    }
//                }
//
//                //0000180a-0000-1000-8000-00805f9b34fb
//                //0000fc00-0000-1000-8000-00805f9b34fb
//
//                @Override
//                public void onBatchScanResults(List<ScanResult> results) {
////                    super.onBatchScanResults(results);
//                    Log.d(TAG, "ScanBatchResult :: "+results.size());
//                }
//
//                @Override
//                public void onScanFailed(int errorCode) {
////                    super.onScanFailed(errorCode);
//                    Log.d(TAG, "ScanFailedResult :: "+errorCode);
//                }
//            });

        }
        initialised = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
//        bluetoothAdapter.disable();
        if (bluetoothAdapter == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

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

            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

            scanner.startScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (bluetoothGatt != null && bluetoothGatt.getDevice().getAddress().equals(finalizedBleDeviceAddress)) {
                        scanner.stopScan(this);
                        return;
                    }

                    if(result != null && result.getDevice()!=null && result.getDevice().getAddress()!=null) {
                        if (finalizedBleDeviceAddress.equals(result.getDevice().getAddress())) {
                            bluetoothGatt.disconnect();
                            bluetoothGatt = result.getDevice().connectGatt(BleService.this, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
                        }
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
//                    super.onBatchScanResults(results);
                    Log.d(TAG, "ScanBatchResult :: "+results.size());
                }

                @Override
                public void onScanFailed(int errorCode) {
//                    super.onScanFailed(errorCode);
                    Log.d(TAG, "ScanFailedResult :: "+errorCode);
                }
            });

        });

//        if (!bleDeviceAddress.isEmpty() && bleDeviceAddress != "") {
//            final String deviceAddress = bleDeviceAddress;

//        SharedPreferences sharedPreferences = getSharedPreferences("app", MODE_PRIVATE);
//        if (sharedPreferences.contains(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY) && !sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY, "").isEmpty()) {
//            final String bleDeviceAddress = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY,"");
//            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
//
//            scanner.startScan(new ScanCallback() {
//                @Override
//                public void onScanResult(int callbackType, ScanResult result) {
//                    if (bluetoothGatt != null && bluetoothGatt.getDevice().getAddress().equals(bleDeviceAddress)) {
//                        scanner.stopScan(this);
//                        return;
//                    }
//
//                    if(result != null && result.getDevice()!=null && result.getDevice().getAddress()!=null) {
//                        if (bleDeviceAddress.equals(result.getDevice().getAddress())) {
//                            bluetoothGatt = result.getDevice().connectGatt(BleService.this, true, gattCallback);
//                        }
//                    }
//                }
//
//                //0000180a-0000-1000-8000-00805f9b34fb
//                //0000fc00-0000-1000-8000-00805f9b34fb
//
//                @Override
//                public void onBatchScanResults(List<ScanResult> results) {
////                    super.onBatchScanResults(results);
//                    Log.d(TAG, "ScanBatchResult :: "+results.size());
//                }
//
//                @Override
//                public void onScanFailed(int errorCode) {
////                    super.onScanFailed(errorCode);
//                    Log.d(TAG, "ScanFailedResult :: "+errorCode);
//                }
//            });
//        }

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
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothGatt!=null) {
                bluetoothGatt.disconnect();
//                stopServer();
//                stopAdvertising();
            }
            unregisterReceiver(mBluetoothReceiver);
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
