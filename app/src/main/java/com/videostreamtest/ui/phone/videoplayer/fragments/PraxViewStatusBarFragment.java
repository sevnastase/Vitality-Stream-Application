package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

public class PraxViewStatusBarFragment extends AbstractPraxStatusBarFragment {
    private static final String TAG = PraxViewStatusBarFragment.class.getSimpleName();

    private TextView statusbarRpmValue;

    @Override
    protected void initializeLayout(View view) {
        super.initializeLayout(view);

        statusbarRpmValue = view.findViewById(R.id.statusbar_rpm_value);

        addUsedViews(new View[]{
                view.findViewById(R.id.statusbar_rpm_box),
                view.findViewById(R.id.statusbar_volume_buttons_box)
        });

        if (startedFromMotolife) {
            addUsedViews(new View[]{
                    view.findViewById(R.id.statusbar_motolife_power_box),
                    view.findViewById(R.id.chinesport_logo_imageview),
                    view.findViewById(R.id.motolife_info_layout)
            });
        } else {
            addUsedViews(new View[]{
                    view.findViewById(R.id.statusbar_stop_button)
            });
        }
    }

    @Override
    protected void setupFunctionality(View view) {
        super.setupFunctionality(view);

        movieProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int newProgress;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    newProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onDragProgressbar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onDragProgressbar = false;
                goToSecond(newProgress);
            }
        });
    }

    @Override
    protected void useVideoPlayerViewModel(View view) {
        super.useVideoPlayerViewModel(view);

        //RPM data related
        videoPlayerViewModel.getRpmData().observe(getViewLifecycleOwner(), rpmData ->{
            statusbarRpmValue.setText(String.format(getString(R.string.video_screen_rpm), rpmData));
        });
    }

    private void goToSecond(final int newProgress) {
        Log.d(TAG, "newProgress: "+newProgress);
        int framesPerSecond = 30;
        int frameNumber = (newProgress/1000) * framesPerSecond;
        Log.d(TAG, "framenumber: "+frameNumber);
        if (VideoplayerExoActivity.getInstance() != null) {
            VideoplayerExoActivity.getInstance().goToFrameNumber(frameNumber);
        } else if (VideoplayerActivity.getInstance() != null) {
            VideoplayerActivity.getInstance().goToFrameNumber(frameNumber);
        } else {
            Log.d(TAG, "No instances of videoplayers found");
        }
    }
}
