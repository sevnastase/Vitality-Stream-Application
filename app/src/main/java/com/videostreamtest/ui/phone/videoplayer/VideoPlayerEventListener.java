package com.videostreamtest.ui.phone.videoplayer;

import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

@Deprecated
public class VideoPlayerEventListener implements Player.Listener {

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                break;
            case Player.STATE_ENDED:
                Log.d("VideoPlayerActivity", "STATE_ENDED RECORDED");
                VideoplayerActivity.getInstance().showFinishScreen();
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_READY:
                if (playWhenReady) {
                    Log.d("VideoPlayerActivity", "STATE_READY RECORDED :: PLAY");
                } else {
                    Log.d("VideoPlayerActivity", "STATE_READY RECORDED :: PAUSE");
                }
                break;
            default:
                break;
        }
    }
}
