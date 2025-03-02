package com.videostreamtest.service.mqtt;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class MQTTManager {

    public final static String TAG = MQTTManager.class.getSimpleName();
    private MqttClient client;
    private final String brokerUrl;
    private final String clientId;
    private final String username;
    private final String password;
    private final boolean automaticReconnect;
    private String receivedMessage;
    private static MQTTManager instance;
    private ArrayList<String> motoLifeData = new ArrayList<>(Collections.nCopies(5, "0"));
    private DataUpdateListener dataUpdateListener;
    private final int TIMEOUT_SECONDS = 5;

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
     * Makes sure that only one instance of the {@code this} exists at a time.
     * Initializes a new {@link MQTTManager} if the current instance is either null or any of the
     * parameters are different from that of the current instance.
     *
     * @return instance         The new instance of the MQTTManager object, or the existing one if it exists
     */
    public static synchronized MQTTManager getInstance(String brokerUrl, String clientId, String username, String password, boolean automaticReconnect) {
        if (instance == null) {
            instance = new MQTTManager(brokerUrl, clientId, username, password, automaticReconnect);
        } else if (!instance.hasSameCredentials(brokerUrl, clientId, username, password, automaticReconnect)) {
            instance = new MQTTManager(brokerUrl, clientId, username, password, automaticReconnect);
        }
        return instance;
    }

    private boolean hasSameCredentials(String brokerUrl, String clientId, String username, String password, boolean automaticReconnect) {
        return (instance.brokerUrl.equals(brokerUrl) &&
                instance.clientId.equals(clientId) &&
                instance.username.equals(username) &&
                instance.password.equals(password) &&
                instance.automaticReconnect == automaticReconnect);
    }

    /**
     * Connects to the broker as a client with the username, password. If a message arrives via MQTT
     * it is stored and fed to the getMotoLifeData to handle the message.
     *
     * @throws MqttException if the {@link MqttClient} cannot be initialized
     */
    public void connect() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(automaticReconnect);
        options.setConnectionTimeout(TIMEOUT_SECONDS);

        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Log.d(TAG, "Dummy");
//                try {
//                    connect();
//                } catch (MqttException e) {
//                    Log.d(TAG, "Failed to reconnect to client: " + e);
//                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // Called when a message arrives from the server
                // that matches any subscription made by the client
                message.setQos(0);
                String payload = new String(message.getPayload());
                Log.d(TAG, "Message received from topic " + topic + "\n\t" + payload);

                if (!topic.equals("Chinesport/Motolife/LWT")) {
                    receivedMessage = payload;
                    if (!receivedMessage.isEmpty()) {
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
     * This method subscribes to a single topic.
     *
     * @param topic             The topic to connect to
     * @param qos               Quality of Service protocol for the messages
     * @throws MqttException    if MqttClient#subscribe fails
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

    /**
     * The getMotoLifeData method checks the type of message (Check MotoLife integration documentation)
     * received by the MotoLife via the MQTT protocol. Depending on the message, either display the
     * data in the film as requested, or start the currently highlighted film (startleg or startarm),
     * or deal with the pause/spasm/resume
     *
     * @param message           The received message from the MotoLife
     * @return messageData      Returns the list of messages that were provided by the JSON MQTT message
     */
    public ArrayList<String> getMotoLifeData(String message) {
        ArrayList<String> messageData = new ArrayList<>();

        try {
            // Parse the payload
            JSONObject motoLifeData = new JSONObject(message);

            // Extract "Value" object
            Object valueObj = motoLifeData.get("Value");

            if (valueObj.toString().contains("Jump")) {
                messageData.add(valueObj.toString());
            } else if (valueObj.toString().contains("Arrow")) {
                messageData.add(valueObj.toString());
            } else if (valueObj.toString().contains("ToggleRouteparts")) {
                messageData.add(valueObj.toString());
            }

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
                String value = (String) valueObj;
                switch (value) {
                    case "StartLeg":
                        messageData.add("StartLeg");
                        break;
                    case "StartArm":
                        messageData.add("StartArm");
                        break;
                    case "Pause":
                        messageData.add("Pause");
                        break;
                    case "Stop":
                        messageData.add("Stop");
                        break;
                    case "Resume":
                        messageData.add("Resume");
                        break;
                    case "Finish":
                        messageData.add("Finish");
                        break;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, String.valueOf(e.getCause()));
        }

        return messageData;
    }
}
