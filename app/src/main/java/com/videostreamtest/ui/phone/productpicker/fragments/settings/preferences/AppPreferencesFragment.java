package com.videostreamtest.ui.phone.productpicker.fragments.settings.preferences;

import static com.videostreamtest.constants.PraxConstants.UserPreferences.PRIMARY_INPUT_REMOTE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.videostreamtest.R;

public class AppPreferencesFragment extends Fragment {

    private static final String TAG = AppPreferencesFragment.class.getSimpleName();

    private ImageButton tvRemoteHelpButton;
    private SwitchMaterial tvRemoteSwitch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_app_preferences, container, false);

        tvRemoteHelpButton = view.findViewById(R.id.tv_remote_help_button);
        tvRemoteSwitch = view.findViewById(R.id.tv_remote_switch);

        tvRemoteHelpButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getActivity())
                    .setTitle("TV remote vs. Touch preference")
                    .setMessage("If your device has touch screen available, we do our best to support both touch " +
                            "and remote control actions. \n" +
                            "However, you can manually toggle this option if you are using a TV remote.")
                    .setPositiveButton("OK", null)
                    .create().show();
        });

        tvRemoteSwitch.setOnClickListener(v -> {
            boolean newValue = !tvRemoteSwitch.isActivated();
            tvRemoteSwitch.setActivated(newValue);
            SharedPreferences.Editor edit = getActivity().getSharedPreferences("app", Context.MODE_PRIVATE).edit();
            edit.putBoolean(PRIMARY_INPUT_REMOTE, newValue);
            edit.apply();
        });

        tvRemoteSwitch.setActivated(
                getActivity()
                        .getSharedPreferences("app", Context.MODE_PRIVATE)
                        .getBoolean(PRIMARY_INPUT_REMOTE, false)
        );

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}