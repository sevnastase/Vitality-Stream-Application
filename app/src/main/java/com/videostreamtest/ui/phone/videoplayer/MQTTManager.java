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

    /**
     * Constructor method assigning the characteristics defining an MQTTManager object.
     *
     * @param brokerUrl             The URL of the MQTT broker (host) needed to connect
     * @param clientId              clientID unique to each device conencting to broker, not necessary
     * @param username              Username needed to connect to broker
     * @param password              Password needed to connect to broker
     * @param automaticReconnect    Boolean whether to automatically reconnect to the broker if connection is lost
     */
    private MQTTManager(String brokerUrl, String clientId, String username, String password, boolean automaticReconnect) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.automaticReconnect = automaticReconnect;
    }

    /**
     * Makes sure that only one instance of the MQTTManager exists at a time (The app connects to the
     * broker as a client, and there is only one instance of this client).
     *
     * @return instance         The new instance of the MQTTManager object, or the existing one if it exists
     */
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

    /**
     * Connects to the broker as a client with the username, password. If a message arrives via MQTT
     * it is stored and fed to the getMotoLifeData to handle the message.
     *
     * @throws MqttException
     */
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

    /**
     * After connecting to the broker, subscribe to the various topics eg .../LWT or .../[Serial Nr.]
     *
     * @param topic             The topic to connect to
     * @param qos               Quality of Service protocol for the messages
     * @throws MqttException
     */
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

    /**
     * Notifies when new MQTT message comes in.
     */
    public interface DataUpdateListener {
        void onDataUpdate(ArrayList<String> motoLifeData);
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }

//    public String getReceivedMessage() {
//        return receivedMessage;
//    }

    /**
     * The getMotoLifeData method checks the type of message (Check MotoLife integration documentation)
     * received by the MotoLife via the MQTT protocol. Depending on the message, either display the
     * data in the film as requested, or start the currently highlighted film (startleg or startarm),
     * or deal with the pause/spasm/resume
     *
     * @param message           The received message from the MotoLife
     * @return messageData      Returns the list of messages that were provided by the JSON MQTT message
     * @throws JSONException
     */
    public ArrayList<String> getMotoLifeData(String message) throws JSONException {
        ArrayList<String> messageData = new ArrayList<>();

        try {
            // Parse the payload
            JSONObject motoLifeData = new JSONObject(message);

            // Extract "Value" object
            Object valueObj = motoLifeData.get("Value");

            if (valueObj instanceof JSONObject) {
                JSONObject value = (JSONObject) valueObj;
                Log.d(TAG, "value: " + value);

                boolean isSpeed = value.has("Speed");

                if (isSpeed) {
                    // Extract individual data
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
                }
            } else if (valueObj instanceof String) {
                // Handle String "Value"
                String value = (String) valueObj;
                if ("StartLeg".equals(value)) {
                    messageData.add("StartLeg");
                } else if ("StartArm".equals(value)) {
                    // Handle "startarm" similarly
                    messageData.add("StartArm");
                }
            }
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
