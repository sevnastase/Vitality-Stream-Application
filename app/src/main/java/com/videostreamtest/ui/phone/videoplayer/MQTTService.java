package com.videostreamtest.ui.phone.videoplayer;

import static com.videostreamtest.service.database.DatabaseRestService.TAG;

import static java.lang.Thread.sleep;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;

public class MQTTService extends Service {
    MQTTManager mqttManager;
    private final IBinder binder = new LocalBinder();
    boolean connected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialization
        mqttManager = MQTTManager.getInstance("tcp://178.62.194.237:1883", "AndroidClient", "praxtour", "Prax2r!", true);
        Log.d(TAG, "MQTT Manager initialised");

        mqttManager.setDataUpdateListener(motoLifeData -> {
            Intent intent = new Intent("com.videostreamtest.MQTT_DATA_UPDATE");
            intent.putStringArrayListExtra("motoLifeData", motoLifeData);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Connect to MQTT and subscribe to topics
        try {
            mqttManager.connect();
            connected = true;
            Log.d(TAG, "MQTT Manager connected");
            // Subscribe to topics once connected
            mqttManager.subscribe("Chinesport/Motolife/LWT", 1);
            Log.d(TAG, "Subscribed to ../LWT");
            mqttManager.subscribe("Chinesport/Motolife/240000006", 1);
            Log.d(TAG, "Subscribed to ../240000006");
        } catch (MqttException e) {
            e.printStackTrace();
        }


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up connections
        try {
            mqttManager.disconnect();
            Log.d(TAG, "MQTT Manager disconnected");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public class LocalBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        return binder;
    }

//    public ArrayList<String> getData() throws JSONException {
//        String receivedMessage = mqttManager.getReceivedMessage();
//
//        if (!connected || receivedMessage == null) {
//            ArrayList<String> tempData = new ArrayList<>(Collections.nCopies(5, "0"));
//            return tempData;
//        } else {
//            return mqttManager.getMotoLifeData(receivedMessage);
//        }
//    }
}


