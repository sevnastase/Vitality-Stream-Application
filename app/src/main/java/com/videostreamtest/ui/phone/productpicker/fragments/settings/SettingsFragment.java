package com.videostreamtest.ui.phone.productpicker.fragments.settings;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.productpicker.fragments.settings.audio.AudioSettingsFragment;
import com.videostreamtest.ui.phone.productpicker.fragments.settings.preferences.AppPreferencesFragment;
import com.videostreamtest.ui.phone.productpicker.fragments.settings.privacypolicy.PrivacyPolicyFragment;
import com.videostreamtest.ui.phone.productpicker.fragments.settings.wifi.WifiSettingsFragment;

public class SettingsFragment extends Fragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private Button audioButton;
    private Button privacyPolicyButton;
    private Button wifiButton;
    private Button appPreferencesButton;
    private final Class<? extends Fragment> DEFAULT_SETTINGS_SCREEN = AudioSettingsFragment.class;

    private Button[] allButtons;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        audioButton = view.findViewById(R.id.audio_button);
        privacyPolicyButton = view.findViewById(R.id.privacy_policy_button);
        wifiButton = view.findViewById(R.id.wifi_button);
        appPreferencesButton = view.findViewById(R.id.app_preferences_button);

        allButtons = new Button[]{audioButton, privacyPolicyButton, wifiButton, appPreferencesButton};

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // default state
        goToFragment(DEFAULT_SETTINGS_SCREEN);
        focus(audioButton);

        audioButton.setOnClickListener(v -> {
            focus(audioButton);
            goToFragment(AudioSettingsFragment.class);
        });
        privacyPolicyButton.setOnClickListener(v -> {
            focus(privacyPolicyButton);
            goToFragment(PrivacyPolicyFragment.class);
        });
        wifiButton.setOnClickListener(v -> {
            focus(wifiButton);
            goToFragment(WifiSettingsFragment.class);
        });
        appPreferencesButton.setOnClickListener(v -> {
            focus(appPreferencesButton);
            goToFragment(AppPreferencesFragment.class);
        });

        super.onViewCreated(view, savedInstanceState);
    }

    private void focus(Button button) {
        button.requestFocus();
        button.setPaintFlags(button.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        for (Button b : allButtons) {
            if (button.equals(b)) continue;
            unfocus(b);
        }
    }

    private void unfocus(Button button) {
        button.setPaintFlags(button.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
    }

    /**
     * Replaces fragment in {@link} R.id.settings_framelayout with
     * the fragment corresponding {@param fragmentClass}.
     */
    private void goToFragment(Class<? extends Fragment> fragmentClass) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.settings_framelayout, fragmentClass, null)
                    .commit();
        }
    }
}