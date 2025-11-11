package com.videostreamtest.ui.phone.videoplayer.instance.impl;

import android.content.Context;
import android.net.Uri;
import android.view.View;

//import androidx.media3.common.MediaItem;
//import androidx.media3.common.Player;
//import androidx.media3.exoplayer.ExoPlayer;
//import androidx.media3.ui.PlayerView;

import com.videostreamtest.ui.phone.videoplayer.instance.Videoplayer;

public class GoogleExoPlayer implements Videoplayer {

//    private Context context;
//    private ExoPlayer exoPlayer;
//    private PlayerView videoLayout;
//
//    private boolean isFinished = false;

    @Override
    public void pause() {
//        if (exoPlayer != null) {
//            exoPlayer.pause();
//        }
    }

    @Override
    public boolean isPaused() {
//        if (exoPlayer != null) {
//            return !exoPlayer.isPlaying();
//        }
        return false;
    }

    @Override
    public void setRate(float playbackSpeedRate) {
//        if (exoPlayer != null) {
//            exoPlayer.setPlaybackSpeed(playbackSpeedRate);
//        }
    }

    @Override
    public void setVolume(float volumeLevel) {
//        if (exoPlayer != null) {
//            exoPlayer.setVolume(volumeLevel);
//            float volume = volumeLevel*100;
//            exoPlayer.setDeviceVolume((int)volume);
//        }
    }

    @Override
    public void play() {
//        if (exoPlayer != null) {
//            exoPlayer.play();
//            videoLayout.hideController();
//        }
    }

    @Override
    public void init(final Context context) {
//        this.context = context;
//        LogHelper.WriteLogRule(context, AccountHelper.getAccountToken(context), "Google ExoPlayer is initialized...", "DEBUG", "");
//        exoPlayer = new ExoPlayer.Builder(context)
//                .build();
//        exoPlayer.setPlayWhenReady(true);
//        if (this.videoLayout != null) {
//            this.videoLayout.setPlayer(exoPlayer);
//        }
//
//        exoPlayer.addListener(new Player.Listener(){
//
//            @Override
//            public void onPlaybackStateChanged(int playbackState) {
//                if (playbackState == Player.STATE_ENDED) {
//                    isFinished = true;
//                }
//            }
//
//            @Override
//            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
//
//            }
//        });
    }

    @Override
    public void setVideo(final String videoUri) {
//        MediaItem mediaItem  = MediaItem.fromUri(videoUri);
//        exoPlayer.setMediaItem(mediaItem);
//        exoPlayer.prepare();
//        exoPlayer.pause();
    }

    @Override
    public void setVideo(final Uri videoUri) {
        setVideo(videoUri.toString());
    }

    @Override
    public void attachViewElement(View viewElement) {
//        this.videoLayout = (PlayerView) viewElement;
//        if (this.exoPlayer != null) {
//            this.videoLayout.setPlayer(exoPlayer);
//        }
    }

    @Override
    public void setVisible(boolean visible) {
//        if (this.videoLayout != null) {
//            if (visible) {
//                this.videoLayout.setVisibility(View.VISIBLE);
//            } else {
//                this.videoLayout.setVisibility(View.INVISIBLE);
//            }
//        }
    }

    @Override
    public void destructVideoplayer() {
//        if (exoPlayer != null) {
//            exoPlayer.release();
//            exoPlayer = null;
//        }
    }

    @Override
    public long getCurrentPosition() {
//        if (exoPlayer != null) {
//            exoPlayer.getCurrentPosition();
//        }
        return -1;
    }

    @Override
    public long getTotalDuration() {
//        if (exoPlayer != null) {
//            exoPlayer.getDuration();
//        }
        return -1;
    }

    @Override
    public boolean isVideoLoaded() {
//        if(exoPlayer != null) {
//            return !exoPlayer.isLoading();
//        }
        return false;
    }

    @Override
    public boolean isPlaying() {
//        if (exoPlayer != null) {
//            return exoPlayer.isPlaying();
//        }
        return false;
    }

    @Override
    public boolean isMediaEndReached() {
//        if (exoPlayer != null) {
//            return isFinished;
//        }
        return false;
    }

    @Override
    public void goToPosition(long positionMs) {
//        if (exoPlayer != null) {
//            exoPlayer.pause();
//            exoPlayer.seekTo(positionMs);
//            exoPlayer.prepare();
//            exoPlayer.play();
//        }
    }
}
