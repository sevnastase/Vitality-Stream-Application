package com.videostreamtest.ui.phone.videoplayer.fragments.alerts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.videostreamtest.R;

import org.jetbrains.annotations.NotNull;

public class NoAudioAlertFragment extends Fragment {
    private static final String TAG = NoAudioAlertFragment.class.getSimpleName();

    //Views
    private TextView alertTitleView;
    private TextView alertContentView;
    private Button alertConfirmationButton;
    private ImageView alertImageView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_no_audio_alert, container, false);
        alertTitleView = view.findViewById(R.id.alert_dialog_title);
        alertContentView = view.findViewById(R.id.alert_dialog_message);
        alertConfirmationButton = view.findViewById(R.id.alert_dialog_return_home_button);
        alertImageView = view.findViewById(R.id.alert_dialog_image);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alertTitleView.setText(R.string.videoplayer_no_audio_alert_title);
        alertContentView.setText(R.string.videoplayer_no_audio_alert_content);
        alertConfirmationButton.setText(R.string.videoplayer_no_audio_alert_button_confirm_text);
        alertConfirmationButton.setOnClickListener((clickedView) -> {
            Fragment searchFragment = getActivity().getSupportFragmentManager().findFragmentByTag("NoAudioAlert");
            if (searchFragment != null) {
                getActivity().getSupportFragmentManager().beginTransaction().remove(searchFragment).commit();
                getActivity().finish();
            }
        });
        alertConfirmationButton.requestFocus();
    }
}
