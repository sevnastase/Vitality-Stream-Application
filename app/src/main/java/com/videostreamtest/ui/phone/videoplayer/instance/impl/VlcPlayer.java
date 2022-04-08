package com.videostreamtest.ui.phone.videoplayer.instance.impl;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.videostreamtest.ui.phone.videoplayer.instance.Videoplayer;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.List;

public class VlcPlayer implements Videoplayer {
    private static final String TAG = VlcPlayer.class.getSimpleName();

    private Context context;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;

    private boolean isMediaDoneLoading = false;
    private boolean isMediaPaused = false;
    private boolean isMediaPlaying = false;
    private boolean isMediaDone = false;

    private boolean isBestStreamLoaded = false;

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
        mediaPlayer.setRate(playbackSpeedRate);
    }

    @Override
    public void play() {
        mediaPlayer.play();
    }

    @Override
    public void init(final Context context) {
        this.context = context;

        //VLC CONSOLE PARAMETERS
        final List<String> args = new ArrayList<>();
        args.add("-vvv");
        args.add("--sout-all");
        //LOCAL PLAY
        args.add("--file-caching=30000");
        //STREAMING
        args.add("--drop-late-frames");
        args.add("--network-caching=20000");
//        args.add("--live-caching=30000"); for live streaming camera/microphone

        //CREATE LIBVLC
        libVLC = new LibVLC(context, args);

        //MEDIAPLAYER
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.setVideoTrackEnabled(true);

        int sw = ((Activity) context).getWindow().getDecorView().getWidth();
        int sh = ((Activity) context).getWindow().getDecorView().getHeight();

        // sanity check
        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        mediaPlayer.getVLCVout().setWindowSize(sw, sh);

        //LISTEN TO EVENTS OF THE MEDIAPLAYER / MEDIA
        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                Log.d(TAG, "TYPE : "+event.type);
                Log.d(TAG, "TYPE BUFFERING: "+MediaPlayer.Event.Buffering);
                Log.d(TAG, "TYPE PLAYING: "+MediaPlayer.Event.Playing);

                if (event.type != MediaPlayer.Event.Buffering && isBestStreamLoaded) {
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
                    isMediaDone = true;
                }

                if (mediaPlayer!= null) {
                    if (mediaPlayer.getVideoTracksCount() > 0) {
                        int id = -1;
                        for (MediaPlayer.TrackDescription trackDescription : mediaPlayer.getVideoTracks()) {
                            if (trackDescription.id > id) {
                                id = trackDescription.id;
                            }
                            Log.d(TAG, "name:" + trackDescription.name + " :: id:" + trackDescription.id);
                        }
                        if (id > 0 && mediaPlayer.getVideoTrack() != id) {
                            mediaPlayer.setVideoTrack(id);
                        }
                    }
                    if (mediaPlayer.getCurrentVideoTrack()!=null) {
                        Log.d(TAG, "Height :: " + mediaPlayer.getCurrentVideoTrack().height);
                        if (mediaPlayer.getCurrentVideoTrack().height >= 720) {
                            isBestStreamLoaded = true;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void setVideo(String videoUriString) {
        if (mediaPlayer != null) {
            if (mediaPlayer.getMedia() == null) {
                final Media media = new Media(libVLC, videoUriString);
                mediaPlayer.setMedia(media);
                media.release();
            }
        }
    }

    @Override
    public void setVideo(Uri videoUri) {
        if (mediaPlayer != null) {
            if (mediaPlayer.getMedia() == null) {
                final Media media = new Media(libVLC, videoUri);
                mediaPlayer.setMedia(media);
                media.release();
            }
        }
    }

    @Override
    public void attachViewElement(View videoLayout) {
        if (mediaPlayer != null) {
            mediaPlayer.attachViews((VLCVideoLayout) videoLayout, null, false, false);
        }
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
        return isMediaDoneLoading;
    }

    @Override
    public boolean isPlaying() {
        return isMediaPlaying;
    }

    @Override
    public boolean isMediaEndReached() {
        return isMediaDone;
    }

    @Override
    public void goToPosition(long positionMs) {

    }
}
