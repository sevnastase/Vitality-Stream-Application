package com.videostreamtest.ui.phone.videoplayer;
import static com.videostreamtest.service.database.DatabaseRestService.TAG;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class MQTTManager {
    private MqttClient client;
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;
    private String receivedMessage;
    private boolean automaticReconnect;
    private static MQTTManager instance;
    private ArrayList<String> motoLifeData = new ArrayList<String>(Collections.nCopies(5, "0"));
    private DataUpdateListener dataUpdateListener;


    private MQTTManager(String brokerUrl, String clientId, String username, String password, boolean automaticReconnect) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.automaticReconnect = automaticReconnect;
    }

    public static synchronized MQTTManager getInstance(String brokerUrl, String clientId, String username, String password, boolean automaticReconnect) {
        if (instance == null) {
            instance = new MQTTManager(brokerUrl, clientId, username, password, automaticReconnect);
        } else {
            if (!instance.brokerUrl.equals(brokerUrl) || !instance.clientId.equals(clientId) || !instance.username.equals(username)
                || !instance.password.equals(password) || instance.automaticReconnect != automaticReconnect) {
                throw new IllegalStateException("MQTTManager is already initialized with different parameters.");
            }
        }
        return instance;
    }

    public void connect() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(automaticReconnect);

        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Called when the client lost the connection to the broker
                cause.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // Called when a message arrives from the server
                // that matches any subscription made by the client
                String payload = new String(message.getPayload());
                Log.d(TAG, "Message received from topic: " + topic + ":\\n\\t" + payload);

                if (!topic.equals("Chinesport/Motolife/LWT")) {
                    receivedMessage = payload;
                    if (!receivedMessage.isEmpty() && receivedMessage != null) {
                        motoLifeData = getMotoLifeData(receivedMessage);

                        if (dataUpdateListener != null) {
                            dataUpdateListener.onDataUpdate(motoLifeData);
                        }
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Called when a outgoing publish is complete
                Log.d(TAG, "Delivery complete for token: " + token.getResponse());
            }
        });

        client.connect(options);
        Log.d(TAG, "MQTT Manager connecting...");
    }

    public void subscribe(String topic, int qos) throws MqttException {
        Log.d(TAG, "subscribe method");
        client.subscribe(topic, qos);
        Log.d(TAG, "Subscribed to topic: " + topic);
    }

    public void disconnect() throws MqttException {
        if (client != null) {
            client.disconnect();
            Log.d(TAG, "MQTT Manager disconnecting...");
        }
    }

    public interface DataUpdateListener {
        void onDataUpdate(ArrayList<String> motoLifeData);
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }

//    public String getReceivedMessage() {
//        return receivedMessage;
//    }

    public ArrayList<String> getMotoLifeData(String message) throws JSONException {
        ArrayList<String> messageData = new ArrayList<>();

        try {
            // Parse the payload
            JSONObject motoLifeData = new JSONObject(message);

            // Extract "Value" object
            JSONObject value = motoLifeData.getJSONObject("Value");
            Log.d(TAG, "value: " + value);
//            if (value instanceof String) {
//                if (value == "StartLeg") {
//                    messageData.add("StartLeg");
//                }
//            }
//            boolean isSpeed = value.keys().next().startsWith("Speed");
//
//            if (!isSpeed) {
//                // Check if one of the other commands have been sent other than motolife data
//                checkValue(value, messageData);
//            } else {
//                // Extract individual data
//                String speed = value.getString("Speed");
//                String power = value.getString("Power");
//                String mode = value.getString("Mode");
//                String direction = value.getString("Direction");
//                String time = value.getString("Time");
//
//                messageData.add(speed);
//                messageData.add(power);
//                messageData.add(mode);
//                messageData.add(direction);
//                messageData.add(time);
//            }

            String speed = value.getString("Speed");
            String power = value.getString("Power");
            String mode = value.getString("Mode");
            String direction = value.getString("Direction");
            String time = value.getString("Time");

            messageData.add(speed);
            messageData.add(power);
            messageData.add(mode);
            messageData.add(direction);
            messageData.add(time);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return messageData;
    }

    private static void checkValue(JSONObject value, ArrayList<String> messageData) throws JSONException {
        try {
            if (value.has("StartLeg")) {
                String startLeg = value.getString("StartLeg");
                messageData.add(startLeg);
            } else if (value.has("StartArm")) {
                String startArm = value.getString("StartArm");
                messageData.add(startArm);
            } else if (value.has("Stop")) {
                String stop = value.getString("Stop");
                messageData.add(stop);
            } else if (value.has("Jump1")) {
                String jump = value.getString("Jump1");
                messageData.add(jump);
            } else if (value.has("Jump2")) {
                String jump = value.getString("Jump2");
                messageData.add(jump);
            } else if (value.has("Jump3")) {
                String jump = value.getString("Jump3");
                messageData.add(jump);
            } else if (value.has("Jump4")) {
                String jump = value.getString("Jump4");
                messageData.add(jump);
            } else if (value.has("Jump5")) {
                String jump = value.getString("Jump5");
                messageData.add(jump);
            } else if (value.has("Jump6")) {
                String jump = value.getString("Jump6");
                messageData.add(jump);
            } else if (value.has("Pause")) {
                String pause = value.getString("Pause");
                messageData.add(pause);
            } else if (value.has("Spasm")) {
                String spasm = value.getString("Spasm");
                messageData.add(spasm);
            } else if (value.has("Resume")) {
                String resume = value.getString("Resume");
                messageData.add(resume);
            } else if (value.has("End")) {
                String end = value.getString("End");
                messageData.add(end);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public interface ConnectionCallback {
//        void onConnected();
//        void onConnectionFailed(Exception e);
//    }
}
