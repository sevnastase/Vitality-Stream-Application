package com.videostreamtest.service.ble;

import android.Manifest;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.ui.phone.helpers.BleHelper;
import com.videostreamtest.ui.phone.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;

import java.util.List;

import static android.content.Context.BLUETOOTH_SERVICE;

public class BleWrapper {
    private static final String TAG = BleWrapper.class.getSimpleName();

    private Context context;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback bluetoothGattCallback;

    //CADENCE MEASUREMENT SMOOTHEN ALGORITHM
    private int cadenceMeasurements[] = new int[8];
    private int cadenceMeasurementIndex = 0;

    //CADENCE CACHE FOR CALCULATING RPM VALUE
    private int lastCadence = 0;
    private float crankRevolution = 0;
    private int lastCrankEventTime = 0;
    private int lastCumCrankEventTime = 0;

    public BleWrapper() {
    }

    public boolean initBle(final Context context) {
        this.context = context;
        bluetoothManager = (BluetoothManager) this.context.getSystemService(BLUETOOTH_SERVICE);
        if(bluetoothManager == null) {
            return false;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }
        return true;
    }

    public void connectBleDevice(final String bleDeviceAddress) {
        if (VideoplayerActivity.getInstance() != null) {
            PermissionHelper.requestPermission(context, VideoplayerActivity.getInstance());
        }
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No ACCES_COARSE_LOCATION permission.");
        }
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH_ADMIN permission.");
        }
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH permission.");
        }
        bluetoothGattCallback =
                new BluetoothGattCallback() {

                    private long lastCumulativeCrankRevolution = 0;
                    private BluetoothGattCharacteristic mbatteryCharacteristic;

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                        int newState) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.i(TAG, "Connected to GATT server.");
                            if (gatt.getDevice() != null) {
                                Log.i(TAG, "Connected to GATT server {" + gatt.getDevice().getName() + "}");
                            }
                            Log.i(TAG, "Attempting to start service discovery:" +
                                    gatt.discoverServices());

                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.i(TAG, "Disconnected from GATT server.");
                            bluetoothGatt.close();
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
                                Log.d(TAG, "BATTERY SERVICE Charas: "+bluetoothGattService.getCharacteristics().size());
                                mbatteryCharacteristic = bluetoothGattService.getCharacteristic(CSCProfile.BATTERY_MEASUREMENT);
                            }

                            if (CSCProfile.RSC_SERVICE.equals(bluetoothGattService.getUuid())) {
                                Log.d(TAG, "RSC SERVICE Charas: "+bluetoothGattService.getCharacteristics().size());
                                gatt.setCharacteristicNotification(bluetoothGattService.getCharacteristic(CSCProfile.RSC_MEASUREMENT), true);
                                BluetoothGattDescriptor descriptor = bluetoothGattService.getCharacteristic(CSCProfile.RSC_MEASUREMENT).getDescriptor(CSCProfile.CLIENT_CONFIG);
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }

                            if (CSCProfile.CSC_SERVICE.equals(bluetoothGattService.getUuid())) {
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
                    }

                    @Override
                    // Result of a characteristic read operation
                    public void onCharacteristicRead(BluetoothGatt gatt,
                                                     BluetoothGattCharacteristic characteristic,
                                                     int status) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {

                            if (characteristic.getUuid().equals(CSCProfile.BATTERY_MEASUREMENT)) {
                                Log.d(TAG,"BATTERY_SERVICE: "+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0));
                                List<BluetoothDefaultDevice> bluetoothDefaultDevices = PraxtourDatabase.getDatabase(context.getApplicationContext()).bluetoothDefaultDeviceDao().getRawBluetoothDefaultDevice(1);
                                if (bluetoothDefaultDevices!= null && bluetoothDefaultDevices.size()>0) {
                                    bluetoothDefaultDevices.get(0).setBleBatterylevel(""+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0));
                                    PraxtourDatabase.getDatabase(context.getApplicationContext()).bluetoothDefaultDeviceDao().insert(bluetoothDefaultDevices.get(0));
                                };
                            }

                            Log.d(TAG, "READ SUCCES UUID: "+characteristic.getUuid()+" > "+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0));
                        } else {
                            Log.e(TAG, "Status code: "+status);
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "Notification set on chars: " + characteristic.getUuid().toString());

                        Log.d(TAG, "Device Name: " + gatt.getDevice().getName());
                        Log.d(TAG, "Device Address: " + gatt.getDevice().getAddress());
                        Log.d(TAG, "Device Address Given: " + bleDeviceAddress);

                        if (bleDeviceAddress!= null && bleDeviceAddress != "" &&
                                gatt.getDevice().getAddress().equals(bleDeviceAddress)) {
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
                                broadcastData(getLastMeasuredCadenceValue());
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
                                        broadcastData(getLastMeasuredCadenceValue());
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
                        SharedPreferences sharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(ApplicationSettings.DEFAULT_BLE_DEVICE_CONNECTION_STRENGTH_KEY, BleHelper.getRssiStrengthIndicator(context, rssi));
                        editor.commit();

                        List<BluetoothDefaultDevice> bluetoothDefaultDevices = PraxtourDatabase.getDatabase(context.getApplicationContext()).bluetoothDefaultDeviceDao().getRawBluetoothDefaultDevice(1);
                        if (bluetoothDefaultDevices!= null && bluetoothDefaultDevices.size()>0) {
                            bluetoothDefaultDevices.get(0).setBleSignalStrength(BleHelper.getRssiStrengthIndicator(context.getApplicationContext(), rssi));
                            PraxtourDatabase.getDatabase(context.getApplicationContext()).bluetoothDefaultDeviceDao().insert(bluetoothDefaultDevices.get(0));
                        }
                    }
                };
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                if(result != null && result.getDevice()!=null && result.getDevice().getAddress() != null) {
                    if (bleDeviceAddress.equals(result.getDevice().getAddress())) {
                        Handler handler = new Handler(Looper.getMainLooper());

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (bluetoothGatt == null) {
                                    try {
                                        bluetoothGatt = result.getDevice().connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                                    } catch (IllegalArgumentException exception) {
                                        Log.w(TAG, "Device not found with provided address. > "+exception.getLocalizedMessage());
                                    }
                                    Log.d(TAG, "Stopping scan."); //Appropriate only if you want to find and connect just one device.
                                    scanner.stopScan(scanCallback);
                                }
                            }
                        });

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
    }

    public boolean connectDefaultBleDevice() {
        if (context == null) {
            Log.w(TAG, "Context not initialized, forgot to call initBle()?");
            return false;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE);
        final String bleDeviceAddress = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY, "NONE");
        if (bleDeviceAddress == "NONE") {
            Log.w(TAG, "bleDeviceAddress not set, forgot to select default device through settings menu?");
            return false;
        }
        connectBleDevice(bleDeviceAddress);
        return true;
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
//            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }

    //COMMUNICATE TO VIDEOPLAYER ACTIVITY
    private void broadcastData(final int value) {
        VideoplayerActivity.getInstance().updateVideoPlayerScreen(value);
        /* ONLY FOR VIDEO SPEED!
         * When the rpm is above 0 ( there is activity) ) and
         * when rpm is below minimum speed
         * set rpm on static minimum speed
         */
        int rpmReceived = value;
        if (rpmReceived > 0 && rpmReceived < 50) {
            rpmReceived = 50;
        }
        if (value > 100) {
            rpmReceived = 100;
        }
        VideoplayerActivity.getInstance().updateVideoPlayerParams(rpmReceived);
    }

    //CADENCE MEASUREMENTS
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

}
