package com.videostreamtest.service.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;

public class MQTTService extends Service {

    public static final String TAG = MQTTService.class.getSimpleName();
    private MQTTManager mqttManager;
    private final IBinder binder = new LocalBinder();
    private final int MQTT_QOS = 0; // USED TO BE 1

    private String ip;
    private String port;
    private int serialNumber;
    private Thread initThread;

    /** Disconnected either by default or after a user instruction to disconnect. */
    public static final int DISCONNECTED = 0;

    /** This String should be passed every time that this service is stopped manually. */
    public static final String ACTION_STOP_USER = "com.example.ACTION_STOP_USER";

    /** Connected. */
    public static final int CONNECTED = 1;

    /** Disconnected due to an error, not as a result of user instruction. */
    public static final int LOST_CONNECTION = 9;
    private int connectionStatus = DISCONNECTED;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * This is the first method called when the {@code this} is created.
     * First, it checks if the intent instructs the service to stop. If so, the service stops
     * gracefully and notes that a user-instructed disconnect happened. Otherwise,
     * this method sets up the MQTT connection.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP_USER.equals(intent.getAction())){
            handleStopCommand();
        } else {
            initThread = new Thread(() -> {
                retrieveCredentials(intent);
                Log.d(TAG, "Retrieved credentials");
                boolean success = initializeMqttManager();
                if (!success) {
                    return;
                }
                Log.d(TAG, "MQTTManager initialized");
                connectToMqttBroker();
                Log.d(TAG, "Connected to MQTT broker");
            });

            initThread.start();
        }

        return START_REDELIVER_INTENT; // This is important as it remembers the intent when the app
                                       // runs out of memory and needs to restart the service.
    }

    private void handleStopCommand() {
        connectionStatus = DISCONNECTED;
        stopSelf();
    }

    private void retrieveCredentials(Intent intent) {
        Log.d(TAG, "Retrieving MQTT credentials...");
        ip = intent.getStringExtra("ip");
        port = intent.getStringExtra("port");
        serialNumber = intent.getIntExtra("serialNumber", -1);
    }

    private boolean initializeMqttManager() {
        if (ip != null && port != null) {
            String brokerUrl = "tcp://" + ip + ":" + port;
            Log.d(TAG, "Trying MQTT on " + brokerUrl);

            try {
                mqttManager = MQTTManager.getInstance(
                        brokerUrl,
                        "AndroidClient",
                        "praxtour",
                        "Prax2r!",
                        true
                );

                mqttManager.setDataUpdateListener(motoLifeData -> handleDataUpdate(motoLifeData));
                return true;
            } catch (Exception e) {
                Log.d(TAG, "Could not initialize MQTT Manager", e);
                stopSelfWithError();
                return false;
            }
        } else {
            Log.d(TAG, "IP or port is null. MQTT Manager not initialized.");
            stopSelfWithError();
            return false;
        }
    }

    private void connectToMqttBroker() {
        if (mqttManager == null) {
            stopSelfWithError();
            return;
        }

        try {
            mqttManager.connect();
            connectionStatus = CONNECTED;
            broadcastConnectedStatus();

            // Subscribe to topics once connected
            mqttManager.subscribe("Chinesport/Motolife/LWT", MQTT_QOS);
            Log.d(TAG, "Subscribed to ../LWT");
            mqttManager.subscribe("Chinesport/Motolife/" + serialNumber, MQTT_QOS);
            Log.d(TAG, "Subscribed to ../" + serialNumber);
        } catch (MqttException e) {
            stopSelfWithError();
        }
    }

    private void broadcastConnectedStatus() {
        Intent broadcastIntent = new Intent("MQTT_CONNECTION_STATUS");
        broadcastIntent.putExtra("connectionStatus", connectionStatus);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mqttManager != null) {
                mqttManager.disconnect();
            }
        } catch (MqttException e) {
            if (connectionStatus == CONNECTED) { // FIXME: this will never execute because connectionStatus is changed beforehand
                Toast.makeText(getApplicationContext(), "Could not disconnect", Toast.LENGTH_SHORT).show();
            }
            //TODO: like this if a connection is attempted again the app will run out of memory and crash
            //FIXME
        }
        Log.d(TAG, "MQTTService stopped");
        broadcastConnectedStatus();
    }

    private void handleDataUpdate(ArrayList<String> motoLifeData) {
        Log.d(TAG, "motoLifeData: " + motoLifeData);

        if (motoLifeData == null || motoLifeData.isEmpty()) {
            return;
        }

        if (motoLifeData.contains("StartLeg") || motoLifeData.contains("StartArm")) {
            sendLocalBroadcast("com.videostreamtest.ACTION_START_FILM");
        } else if (motoLifeData.contains("Pause")) {
            sendLocalBroadcast("com.videostreamtest.ACTION_PAUSE_FILM");
        } else if (motoLifeData.contains("Stop")) {
            sendLocalBroadcast("com.videostreamtest.ACTION_STOP_FILM");
        } else if (motoLifeData.contains("Resume")) {
            sendLocalBroadcast("com.videostreamtest.ACTION_RESUME_FILM");
        } else if (motoLifeData.contains("Finish")) {
            sendLocalBroadcast("com.videostreamtest.ACTION_FINISH_FILM");
        } else if (isToggleRoutepartsCommand(motoLifeData)) {
            Intent intent = new Intent("com.videostreamtest.ACTION_TOGGLE_ROUTEPARTS");
            intent.putExtra("toggleValue", getRoutepartCommandValue(motoLifeData));
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else if (isJumpCommand(motoLifeData) && motoLifeData.get(0).length() >= 5) {
            Intent intent = new Intent("com.videostreamtest.ACTION_JUMP");
            intent.putExtra("routepartNr", String.valueOf(motoLifeData.get(0).toCharArray()[4]));
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else if (isArrowCommand(motoLifeData)) {
            Intent intent = new Intent("com.videostreamtest.ACTION_ARROW");
            intent.putExtra("direction", getArrowDirection(motoLifeData));
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            // Handle other data updates
            Intent intent = new Intent("com.videostreamtest.MQTT_DATA_UPDATE");
            intent.putStringArrayListExtra("motoLifeData", motoLifeData);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private void sendLocalBroadcast(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private boolean isToggleRoutepartsCommand(ArrayList<String> motoLifeData) {
        return !motoLifeData.isEmpty() && motoLifeData.get(0).toLowerCase().contains("togglerouteparts");
    }

    private boolean getRoutepartCommandValue(ArrayList<String> motoLifeData) {
        return "1".equals(String.valueOf((motoLifeData.get(0).toCharArray()[motoLifeData.get(0).length() - 1])));
    }

    private boolean isJumpCommand(ArrayList<String> motoLifeData) {
        return !motoLifeData.isEmpty() && motoLifeData.get(0).toLowerCase().contains("jump");
    }

    private boolean isArrowCommand(ArrayList<String> motoLifeData) {
        return !motoLifeData.isEmpty() && motoLifeData.get(0).toLowerCase().contains("arrow");
    }

    private String getArrowDirection(ArrayList<String> motoLifeData) {
        char[] commandCharArray = motoLifeData.get(0).toLowerCase().toCharArray();
        StringBuilder direction = new StringBuilder();

        for (int i = "Arrow".length(); i < commandCharArray.length; i++) {
            direction.append(commandCharArray[i]);
        }

        return direction.toString();
    }

    private void stopSelfWithError() {
        connectionStatus = LOST_CONNECTION;
        stopSelf();
    }

    public class LocalBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }
}
