package com.videostreamtest.ui.phone.productpicker.fragments.settings.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.videostreamtest.R;

public class AudioSettingsFragment extends Fragment {

    private static final String TAG = AudioSettingsFragment.class.getSimpleName();

    private SeekBar volumeSlider;
    private TextView preferredStartVolumeTextView;
    private int preferredStartVolume;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_audio, container, false);

        volumeSlider = view.findViewById(R.id.audio_slider);
        preferredStartVolumeTextView = view.findViewById(R.id.default_volume_textview);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initUi();

        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int currentValue = seekBar.getProgress();
                if (currentValue < 5) {
                    currentValue = 5;
                }

                if (currentValue > 95) {
                    currentValue = 100;
                }

                // rounds to nearest 10
                preferredStartVolume = (int) (Math.round(currentValue / 10.0) * 10);
                updateDefaultVolumeDisplayed();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveDefaultVolume();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        initUi();
    }

    private void initUi() {
        preferredStartVolume = getSavedDefaultVolume();
        if (preferredStartVolume < 0) {
            preferredStartVolume = 50; // default
            saveDefaultVolume();
        }

        updateDefaultVolumeDisplayed();
        volumeSlider.setProgress(preferredStartVolume);
    }

    private void updateDefaultVolumeDisplayed() {
        if (preferredStartVolume <= 0 || preferredStartVolume > 100) {
            return;
        }
        preferredStartVolumeTextView.setText(String.valueOf(preferredStartVolume));
    }

    private void saveDefaultVolume() {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("app", Context.MODE_PRIVATE).edit();
        editor.putInt("defaultVolume", preferredStartVolume);
        editor.apply();
    }

    /**
     * Returns -1 if there is no saved volume.
     */
    private int getSavedDefaultVolume() {
        return getActivity().getSharedPreferences("app", Context.MODE_PRIVATE).getInt("defaultVolume", -1);
    }
}