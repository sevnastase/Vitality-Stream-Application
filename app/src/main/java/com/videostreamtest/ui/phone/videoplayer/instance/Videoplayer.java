package com.videostreamtest.ui.phone.videoplayer.instance;

import android.content.Context;
import android.net.Uri;
import android.view.View;

public interface Videoplayer {

    /**
     * Pause the player
     */
    void pause();

    /**
     * Return if videoplayer is paused
     * @return
     */
    boolean isPaused();

    /**
     * Set PlayBack speed of the videoplayer
     */
    void setRate(final float playbackSpeedRate);

    /**
     * Set PlayBack speed of the videoplayer
     */
    void setVolume(final float volumeLevel);

    /**
     * Play videoplayer
     */
    void play();

    /**
     * Initialize videoplayer
     */
    void init(final Context context);

    /**
     * Set video uri to load the video
     * @param videoUri
     */
    void setVideo(final String videoUri);

    /**
     * Set video uri to load the video
     * @param videoUri
     */
    void setVideo(final Uri videoUri);

    /**
     * Attach the video component to a view element in the Activity Layout.
     * @param viewElement
     */
    void attachViewElement(final View viewElement);

    /**
     * Release memory and destroy videoplayer objects
     */
    void destructVideoplayer();

    /**
     * Get current position in ms
     * @return
     */
    long getCurrentPosition();

    /**
     * Total duration of current loaded media in ms
     * @return
     */
    long getTotalDuration();

    /**
     * Returns if the video is loaded in the videoplayer
     * @return
     */
    boolean isVideoLoaded();

    /**
     * Returns if the video is currently playing
     * @return
     */
    boolean isPlaying();

    /**
     * When the end of the video is reached
     * @return
     */
    boolean isMediaEndReached();

    /**
     * Go to the position value given in ms
     * @param positionMs
     */
    void goToPosition(long positionMs);

    /**
     * Set view visibility
     */
    void setVisible(final boolean visible);
}
