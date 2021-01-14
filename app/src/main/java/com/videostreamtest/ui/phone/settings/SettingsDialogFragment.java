package com.videostreamtest.ui.phone.settings;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.videostreamtest.R;
import com.videostreamtest.service.ant.AntPlusService;


public class SettingsDialogFragment extends DialogFragment {
    private static SettingsDialogFragment thisSettingsDialogFragment;
    private SettingsAntPlusBroadcastReceiver settingsAntPlusBroadcastReceiver;
    private Intent antplusService;

    private Button antplusServiceButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_settings_dialog, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        thisSettingsDialogFragment = this;
        setupView(view);
        setupFilters(view);
        setupViewListeners(view);
    }

    public static SettingsDialogFragment getInstance() {
        return thisSettingsDialogFragment;
    }

    public void updateText(final String cadenceValue) {
        SettingsDialogFragment.getInstance().getActivity().runOnUiThread(new Runnable() {
            public void run() {
                TextView textV1 = getActivity().findViewById(R.id.cadence_sensor_value_textview);
                textV1.setText(cadenceValue);
            }
        });
    }

    private void setupView(@NonNull View view) {
        getDialog().setTitle("Sensor connection");
        setCancelable(true);

        SharedPreferences myPreferences = view.getContext().getSharedPreferences("app",0);
        String apiKey = myPreferences.getString("apiKey", "unauthorized");
        String antServiceStatus = myPreferences.getString("antServiceStatus", "Off");

        antplusServiceButton = view.findViewById(R.id.antplus_button);

        //Define Ant+ service
        antplusService = new Intent(view.getContext(), AntPlusService.class);

        //Define broadcastReceiver
        settingsAntPlusBroadcastReceiver = new SettingsAntPlusBroadcastReceiver();

    }

    private void setupFilters(@NonNull View view) {

    }

    private void setupViewListeners(@NonNull View view) {
        //Close button
        Button closeButton = view.findViewById(R.id.close_settings_dialog_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        //Start ANT+ service
        antplusServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAntService();
            }
        });
    }

    private void toggleAntService() {
        if (antplusServiceButton.getText().toString().equalsIgnoreCase("off")) {
            getActivity().startService(antplusService);
            antplusServiceButton.setBackgroundColor(Color.parseColor("#ADFF2F"));
            antplusServiceButton.setText("On");

            //start receiver
            IntentFilter filter = new IntentFilter("com.fitstream.ANTDATA");
            getActivity().registerReceiver(settingsAntPlusBroadcastReceiver, filter);
        } else {
            getActivity().unregisterReceiver(settingsAntPlusBroadcastReceiver);
            getActivity().stopService(antplusService);

            antplusServiceButton.setBackgroundColor(Color.parseColor("#8B0000"));
            antplusServiceButton.setText("Off");
        }
    }
}
