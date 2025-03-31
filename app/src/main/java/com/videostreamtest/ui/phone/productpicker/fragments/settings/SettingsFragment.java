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
import com.videostreamtest.ui.phone.productpicker.fragments.settings.privacypolicy.PrivacyPolicyFragment;
import com.videostreamtest.ui.phone.productpicker.fragments.settings.wifi.WifiSettingsFragment;

public class SettingsFragment extends Fragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private Button audioButton;
    private Button privacyPolicyButton;
    private Button wifiButton;
    private final Class<? extends Fragment> DEFAULT_SETTINGS_SCREEN = AudioSettingsFragment.class;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        audioButton = view.findViewById(R.id.audio_button);
        privacyPolicyButton = view.findViewById(R.id.privacy_policy_button);
        wifiButton = view.findViewById(R.id.wifi_button);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // default state
        goToFragment(DEFAULT_SETTINGS_SCREEN);
        focus(audioButton);

        audioButton.setOnClickListener(v -> {
            unfocus(new Button[]{privacyPolicyButton, wifiButton});
            focus(audioButton);
            goToFragment(AudioSettingsFragment.class);
        });
        privacyPolicyButton.setOnClickListener(v -> {
            unfocus(new Button[]{audioButton, wifiButton});
            focus(privacyPolicyButton);
            goToFragment(PrivacyPolicyFragment.class);
        });
        wifiButton.setOnClickListener(v -> {
            unfocus(new Button[]{audioButton, privacyPolicyButton});
            focus(wifiButton);
            goToFragment(WifiSettingsFragment.class);
        });

        super.onViewCreated(view, savedInstanceState);
    }

    private void focus(Button button) {
        button.requestFocus();
        button.setPaintFlags(button.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    private void unfocus(Button button) {
        button.setPaintFlags(button.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
    }

    private void unfocus(Button[] buttons) {
        for (Button button : buttons) {
            unfocus(button);
        }
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