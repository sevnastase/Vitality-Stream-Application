package com.videostreamtest.ui.phone.videoplayer.instance.impl;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.videoplayer.instance.Videoplayer;
import com.videostreamtest.utils.VideoLanLib;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.List;

public class VlcPlayer implements Videoplayer {
    private static final String TAG = VlcPlayer.class.getSimpleName();

    private Context context;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private VLCVideoLayout videoLayout;

    private boolean isMediaDoneLoading = false;
    private boolean isMediaPaused = false;
    private boolean isMediaPlaying = false;
    private boolean isMediaDone = false;

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public boolean isPaused() {
        return isMediaPaused;
    }

    @Override
    public void setRate(final float playbackSpeedRate) {
        if (mediaPlayer!= null) {
            mediaPlayer.setRate(playbackSpeedRate);
        }
    }

    @Override
    public void setVolume(float volumeLevel) {
        if (mediaPlayer != null) {
            float level = volumeLevel * 100;
            mediaPlayer.setVolume((int) level);
        }
    }

    @Override
    public void play() {
        if (mediaPlayer == null && mediaPlayer.getVLCVout().areViewsAttached()) {
            return;
        }
        mediaPlayer.setVideoTrackEnabled(true);
        int sw = ((Activity) context).getWindow().getDecorView().getWidth();
        int sh = ((Activity) context).getWindow().getDecorView().getHeight();

        //FIX FOR IIYAMA ProLite T2452MTS MONITOR
        if (sh == 1008) {
            sh = 1080;
        }

        // sanity check
        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        mediaPlayer.getVLCVout().setWindowSize(sw, sh);
        mediaPlayer.setAspectRatio("16:9");
        mediaPlayer.play();
    }

    @Override
    public void init(final Context context) {
        this.context = context;

        LogHelper.WriteLogRule(context, AccountHelper.getAccountToken(context), "VLC Player is initialized...", "DEBUG", "");

        //LINK LIBVLC
        libVLC = VideoLanLib.getLibVLC(context);

        //MEDIAPLAYER
        mediaPlayer = new MediaPlayer(libVLC);

        mediaPlayer.setVideoTrackEnabled(true);

        //LISTEN TO EVENTS OF THE MEDIAPLAYER / MEDIA
        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {

                if (event.type != MediaPlayer.Event.Buffering) {
                    isMediaDoneLoading = true;
                }

                if (event.type == MediaPlayer.Event.Paused) {
                    isMediaPaused = true;
                    isMediaPlaying = false;
                }

                if (event.type == MediaPlayer.Event.Playing) {
                    isMediaPaused = false;
                    isMediaPlaying = true;
                }

                if (event.type == MediaPlayer.Event.EndReached) {
                    //showFinishScreen();
                    mediaPlayer.release();
                    isMediaDone = true;
                }

                if (mediaPlayer!= null) {
                    if (mediaPlayer.getVideoTracksCount() > 0) {
                        int id = -1;
                        for (MediaPlayer.TrackDescription trackDescription : mediaPlayer.getVideoTracks()) {
                            if (trackDescription.id > id) {
                                id = trackDescription.id;
                            }
                        }
                        if (id > 0 && mediaPlayer.getVideoTrack() != id) {
                            mediaPlayer.setVideoTrack(id);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void setVideo(final String videoUriString) {
        if (mediaPlayer != null) {
            if (mediaPlayer.getMedia() == null) {
                final Media media = new Media(libVLC, videoUriString);
                mediaPlayer.setMedia(media);
                media.release();
            }
        }
    }

    @Override
    public void setVideo(final Uri videoUri) {
        if (mediaPlayer != null) {
            if (mediaPlayer.getMedia() == null) {
                final Media media = new Media(libVLC, videoUri);
                mediaPlayer.setMedia(media);
                media.release();
            }
        }
    }

    @Override
    public void attachViewElement(final View videoLayout) {
        this.videoLayout = (VLCVideoLayout) videoLayout;
        if (mediaPlayer != null) {
            mediaPlayer.attachViews(this.videoLayout, null, false, false);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.videoLayout != null) {
            attachViewElement(this.videoLayout);
            if (visible) {
                this.videoLayout.setVisibility(View.VISIBLE);
            } else {
                this.videoLayout.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void destructVideoplayer() {
        if (mediaPlayer!=null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (mediaPlayer!= null) {
            return mediaPlayer.getTime();
        } else {
            return -1;
        }
    }

    @Override
    public long getTotalDuration() {
        if (mediaPlayer != null && mediaPlayer.getMedia() != null) {
            return mediaPlayer.getMedia().getDuration();
        } else  {
            return -1;
        }
    }

    @Override
    public boolean isVideoLoaded() {
        return isMediaDoneLoading;
//        return (mediaPlayer.isSeekable() && mediaPlayer.getLength() != -1);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public boolean isMediaEndReached() {
        return isMediaDone;
    }

    @Override
    public void goToPosition(long positionMs) {
        if (positionMs > 0) {
            mediaPlayer.pause();
            mediaPlayer.setTime(positionMs);
            mediaPlayer.play();
        }
    }
}
