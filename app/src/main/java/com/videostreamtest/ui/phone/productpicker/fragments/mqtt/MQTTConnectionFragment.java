package com.videostreamtest.ui.phone.productpicker.fragments.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.videostreamtest.R;
import com.videostreamtest.service.mqtt.MQTTService;
import com.videostreamtest.helpers.DataHolder;

import java.util.Objects;

public class MQTTConnectionFragment extends Fragment {

    private static final String TAG = MQTTConnectionFragment.class.getSimpleName();
    private EditText ipInputField;
    private EditText portInputField;
    private EditText serialNumberInputField;
    private Button connectButton;
    private Button disconnectButton;
    private ProgressBar loadingWheel;
    private LinearLayout connectionBox;
    private TextView connectedSerialNumberTextView;
    private BroadcastReceiver mqttReceiver;

    private String ip;
    private String port;
    private int serialNumber;
    private final String SP_MQTT_NAME = "Motolife login credentials";
    private final String SP_MQTT_IP = "Motolife ip";
    private final String SP_MQTT_PORT = "Motolife port";
    private final String SP_MQTT_SRNR = "Motolife serial number";
    private final String SP_MQTT_LAST_CONNECTED_STATUS = "Motolife connected";
    private final String DEFAULT_MQTT_IP = "192.168.4.1";
    private final String DEFAULT_MQTT_PORT = "1883";

    private Runnable networkCheckerRunnable;

    private BroadcastReceiver networkReceiver;

    private Thread autoLoginThread;
    private Handler autoLoginHandler;

    /**
     * Readies the fragment for user actions. The connect button initiates a connection
     * on the URL (IP:port) given in the text input fields. No checks are needed as input is
     * restricted in the xml file. While that call is made, a loading wheel appears and the fragment
     * is unusable until there is a result. Upon a result is received via the {@code mqttReceiver},
     * action is taken.
     * If there is a successful login, the app remembers the good credentials and the next the
     * fragment is created, it initiates a login on the saved URL.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mqtt_connection, container, false);

        // Hides keyboard
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        ipInputField = view.findViewById(R.id.motolife_ip_input);
        portInputField = view.findViewById(R.id.motolife_port_input);
        serialNumberInputField = view.findViewById(R.id.motolife_serial_number_input);
        connectButton = view.findViewById(R.id.motolife_connect_button);
        disconnectButton = view.findViewById(R.id.motolife_disconnect_button);
        loadingWheel = view.findViewById(R.id.motolife_loading_wheel);
        connectionBox = view.findViewById(R.id.overlay_motolife_connection_box_background);
        connectedSerialNumberTextView = view.findViewById(R.id.connected_sr_number_textview);

        retrieveSavedLoginInfo();

        connectButton.setOnClickListener(v -> {
            if (!DataHolder.getInstance().isMotolifeConnected()) {
                showLoadingWheel();
                ip = ipInputField.getText().toString();
                port = portInputField.getText().toString();
                serialNumber = Integer.parseInt(serialNumberInputField.getText().toString());
                hideKeyboard();
                connectMQTT();
            }
        });

        disconnectButton.setOnClickListener(v -> {
            Intent stopIntent = new Intent(getActivity(), MQTTService.class);
            stopIntent.setAction(MQTTService.ACTION_STOP_USER);
            Objects.requireNonNull(getActivity()).startService(stopIntent);
            fillInSavedLoginInfo();
        });

        // These serve to keep the connect button disabled
        // for as long as the user has not entered a value in each field
        // NOTE: port observing is disabled, see {@link this#checkInputFields()}
        ipInputField.addTextChangedListener(checkInputFields());
        portInputField.addTextChangedListener(checkInputFields());
        serialNumberInputField.addTextChangedListener(checkInputFields());

        mqttReceiver = new BroadcastReceiver() {
            /**
             * Called when {@link MQTTService} sends a broadcast.
             * If connection is established, this method will save the given credentials in
             * {@link SharedPreferences}.
             * In either case, a message will be displayed to the user to give feedback about what
             * happened (connection timeout, successful connect/disconnect).
             * The method also modifies the app-persistent variable motolifeConnected.
             *
             * @modifies {@link DataHolder}.motolifeConnected
             */
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received MQTT broadcast");
                int connectionStatus = intent.getIntExtra("connectionStatus", -1);
                String feedbackMsg;

                if (connectionStatus == MQTTService.CONNECTED) {
                    feedbackMsg = "Connection successful";
                    saveLoginCredentials();
                    showDisconnectButton();
                    goToProductPickerFragment();
                    DataHolder.getInstance().setMotolifeConnected(true);
                } else if (connectionStatus == MQTTService.LOST_CONNECTION) {
                    handleDisconnect();
                    feedbackMsg = "Could not connect to MOTOlife";
                } else if (connectionStatus == MQTTService.DISCONNECTED) {
                    handleDisconnect();
                    feedbackMsg = "Successfully disconnected";
                } else {
                    feedbackMsg = "Contact Greg: MQTT connection";
                }

                Toast.makeText(getContext().getApplicationContext(), feedbackMsg, Toast.LENGTH_LONG).show();
            }
        };

        // TODO: test on physical device
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received network broadcast");
                boolean available = intent.getBooleanExtra("Network connection", false);

                if (available) {
                    if (DataHolder.getInstance().isMotolifeConnected()) {
                        showDisconnectButton();
                    } else {
                        showConnectionBox();
                    }
                } else {
                    Intent stopIntent = new Intent(getActivity(), MQTTService.class);
                    getActivity().stopService(stopIntent);
                    DataHolder.getInstance().setMotolifeConnected(false);
                    showNoNetworkMessage();
                }
            }
        };

        IntentFilter mqttReceiverFilter = new IntentFilter("MQTT_CONNECTION_STATUS");
        getActivity().registerReceiver(mqttReceiver, mqttReceiverFilter);

        IntentFilter networkReceiverFilter = new IntentFilter("NETWORK_CONNECTION_STATUS");
        getActivity().registerReceiver(networkReceiver, networkReceiverFilter);

        disableConnectButton();

        if (retrieveLastConnectedStatus() && !DataHolder.getInstance().isMotolifeConnected()) { // User was logged in
            autoLogin();
        } else if (!DataHolder.getInstance().isMotolifeConnected()) {
            fillInSavedLoginInfo();
        }

        if (DataHolder.getInstance().isMotolifeConnected()) {
            showDisconnectButton();
        } else {
            showConnectionBox();
        }

        return view;
    }

    private boolean retrieveLastConnectedStatus() {
        SharedPreferences sp = getActivity().getSharedPreferences(SP_MQTT_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(SP_MQTT_LAST_CONNECTED_STATUS, false);
    }

    private void autoLogin() {
        autoLoginThread = new Thread(() -> {
            try {
                showLoadingWheel();
                Thread.sleep(2000);
                fillInSavedLoginInfo();
                try {
                    connectMQTT();
                } catch (Exception e) {
                    Log.d(TAG, "Could not auto-connect");
                }
            } catch (InterruptedException ignored) {}
        });

        autoLoginThread.start();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void showLoadingWheel() {
        getActivity().runOnUiThread(() -> {
            loadingWheel.setVisibility(View.VISIBLE);
            connectionBox.setVisibility(View.GONE);
            disconnectButton.setVisibility(View.GONE);
            connectedSerialNumberTextView.setVisibility(View.GONE);
        });
    }

    private void showConnectionBox() {
        getActivity().runOnUiThread(() -> {
            loadingWheel.setVisibility(View.GONE);
            connectionBox.setVisibility(View.VISIBLE);
            disconnectButton.setVisibility(View.GONE);
            connectedSerialNumberTextView.setVisibility(View.GONE);
        });
    }

    private void showDisconnectButton() {
        getActivity().runOnUiThread(() -> {
            loadingWheel.setVisibility(View.GONE);
            connectionBox.setVisibility(View.GONE);
            disconnectButton.setVisibility(View.VISIBLE);
            String textToBeDisplayed = "You are connected to device " + serialNumber;
            connectedSerialNumberTextView.setText(textToBeDisplayed);
            connectedSerialNumberTextView.setVisibility(View.VISIBLE);
        });
    }

    private void showNoNetworkMessage() {
        getActivity().runOnUiThread(() -> {
            loadingWheel.setVisibility(View.GONE);
            connectionBox.setVisibility(View.GONE);
            disconnectButton.setVisibility(View.GONE);
            connectedSerialNumberTextView.setVisibility(View.GONE);
        });
    }

    private void handleDisconnect() {
        showConnectionBox();
        DataHolder.getInstance().setMotolifeConnected(false);
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(SP_MQTT_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(SP_MQTT_LAST_CONNECTED_STATUS, false);
        editor.apply();
    }

    //FIXME
    private void setupAutoLogin() {
        autoLoginThread = new HandlerThread("AutoLoginThread");
        autoLoginThread.start();

        autoLoginHandler = new Handler();
        networkCheckerRunnable = () -> {
            fillInSavedLoginInfo();
        };
        autoLoginHandler.postDelayed(networkCheckerRunnable, 500);
    }

    private TextWatcher checkInputFields() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!ipInputField.getText().toString().isEmpty() &&
                    // !portInputField.getText().toString().isEmpty() &&
                    !serialNumberInputField.getText().toString().isEmpty()) {
                    enableConnectButton();
                } else {
                    disableConnectButton();
                }
            }
        };
    }

    private void disableConnectButton() {
        connectButton.setAlpha((float) 0.5);
        connectButton.setClickable(false);
    }

    private void enableConnectButton() {
        connectButton.setAlpha((float) 1);
        connectButton.setClickable(true);
    }

    private void retrieveSavedLoginInfo() {
        SharedPreferences sp = getActivity().getSharedPreferences(SP_MQTT_NAME, Context.MODE_PRIVATE);
        if (sp == null) {
            Log.d(TAG, "SharedPreferences was null, aborting");
            return;
        }

        ip = sp.getString(SP_MQTT_IP, null);
        port = sp.getString(SP_MQTT_PORT, null);
        try {
            serialNumber = sp.getInt(SP_MQTT_SRNR, -1);
        } catch (ClassCastException e) {
            String storedValue = sp.getString(SP_MQTT_SRNR, "-1");
            serialNumber = storedValue == null ? -1 : Integer.parseInt(storedValue);
        }
    }

    private void fillInSavedLoginInfo() {
        SharedPreferences sp = getActivity().getSharedPreferences(SP_MQTT_NAME, Context.MODE_PRIVATE);
        if (sp == null) {
            Log.d(TAG, "SharedPreferences was null, aborting");
            return;
        }

        ip = sp.getString(SP_MQTT_IP, null);
        port = sp.getString(SP_MQTT_PORT, null);
        try {
            serialNumber = sp.getInt(SP_MQTT_SRNR, -1);
        } catch (ClassCastException e) {
            String storedValue = sp.getString(SP_MQTT_SRNR, "-1");
            serialNumber = storedValue == null ? -1 : Integer.parseInt(storedValue);
        }
        if (ip == null || port == null) {
            Log.d(TAG, "IP or port credentials were null, aborting");
            return;
        }

        getActivity().runOnUiThread(() -> {
            ipInputField.setText(ip);
            portInputField.setText(port);
            serialNumberInputField.setText(String.valueOf(serialNumber));
            hideKeyboard();
        });
    }

    private void connectMQTT() {
        Intent intent = new Intent(getActivity(), MQTTService.class);
        intent.putExtra("ip", DEFAULT_MQTT_IP);
        intent.putExtra("port", DEFAULT_MQTT_PORT);
        intent.putExtra("serialNumber", serialNumber);
        getActivity().startService(intent);
    }

    private void goToProductPickerFragment() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        navController.navigate(R.id.productPickerFragment);
    }

    private void saveLoginCredentials() {
        SharedPreferences sp = getActivity().getSharedPreferences(SP_MQTT_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SP_MQTT_IP, ip);
        editor.putString(SP_MQTT_PORT, port);
        editor.putInt(SP_MQTT_SRNR, serialNumber);
        editor.putBoolean(SP_MQTT_LAST_CONNECTED_STATUS, true);
        editor.apply();
        Log.d(TAG, "Saved " + ip + ":" + port + ", device " + serialNumber);
    }

    private void clearLoginCredentials() {
        SharedPreferences sp = getActivity().getSharedPreferences(SP_MQTT_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.remove(SP_MQTT_IP);
        editor.remove(SP_MQTT_PORT);
        editor.remove(SP_MQTT_SRNR);
        editor.putBoolean(SP_MQTT_LAST_CONNECTED_STATUS, false);

        editor.apply();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getView();
        if (view == null) {
            view = new View(getActivity());
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}