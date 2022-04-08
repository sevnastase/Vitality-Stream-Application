package com.videostreamtest.ui.phone.videoplayer.instance.impl;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.videostreamtest.ui.phone.videoplayer.instance.Videoplayer;

public class GoogleExoPlayer implements Videoplayer {
    @Override
    public void pause() {

    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public void setRate(float playbackSpeedRate) {

    }

    @Override
    public void play() {

    }

    @Override
    public void init(final Context context) {

    }

    @Override
    public void setVideo(String videoUri) {

    }

    @Override
    public void setVideo(Uri videoUri) {

    }

    @Override
    public void attachViewElement(View viewElement) {

    }

    @Override
    public void destructVideoplayer() {

    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public long getTotalDuration() {
        return 0;
    }

    @Override
    public boolean isVideoLoaded() {
        return false;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public boolean isMediaEndReached() {
        return false;
    }

    @Override
    public void goToPosition(long positionMs) {

    }
}
