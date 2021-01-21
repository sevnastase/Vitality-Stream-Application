package com.videostreamtest.ui.phone.videoplayer;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.videostreamtest.R;
import com.videostreamtest.service.ant.AntPlusBroadcastReceiver;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.ui.phone.result.ResultActivity;
import com.videostreamtest.utils.RpmVectorLookupTable;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoplayerActivity extends AppCompatActivity {

    private static VideoplayerActivity thisInstance;

    private CastContext castContext;

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private String videoUri = VideoPlayerConfig.DEFAULT_VIDEO_URL;
    private AntPlusBroadcastReceiver antPlusBroadcastReceiver;

    private TextView rpmValue;

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Intent antService = new Intent(getApplicationContext(), AntPlusService.class);
                    stopService(antService);
                    releasePlayer();
                    finish();
                    break;
                case MotionEvent.ACTION_UP:
//                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisInstance = this;
        setContentView(R.layout.activity_videoplayer);

//        castContext = CastContext.getSharedInstance(this);

        playerView = findViewById(R.id.playerView);
        rpmValue = findViewById(R.id.movieRpm);


//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        final TextView movieTitle = findViewById(R.id.movieTitle);
        SharedPreferences myPreferences = getSharedPreferences("app",0);
        movieTitle.setText(myPreferences.getString("selectedMovieTitle","Title not found"));

        setUp();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
//        delayedHide(100);
    }

    public static VideoplayerActivity getInstance() {
        return thisInstance;
    }

    public void updateVideoPlayerScreen(int rpm) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rpmValue.setText("RPM: "+rpm);
            }
        });
    }

    public void updateVideoPlayerParams(int rpm) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Setting the speed of the player based on our cadence rpm reading
                PlaybackParameters playbackParameters  = new PlaybackParameters(RpmVectorLookupTable.getPlaybackspeed(rpm), PlaybackParameters.DEFAULT.pitch);
                player.setPlaybackParameters(playbackParameters);
            }
        });
    }

    public void startResultScreen() {
        //Stop Ant+ service
        final Intent antplusService = new Intent(getApplicationContext(), AntPlusService.class);
        stopService(antplusService);
        // Build result screen
        final Intent resultScreen = new Intent(getApplicationContext(), ResultActivity.class);
        startActivity(resultScreen);
        //Release player and finish activity
        releasePlayer();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }

    /*
     Below is the player code from the ExoPlayer from google.
     */

    @Override
    protected void onPause() {
        super.onPause();
        pausePlayer();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        resumePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(antPlusBroadcastReceiver);
        releasePlayer();
    }

    private void setUp() {
        initializePlayer();
        getSelectedVideoUri();
        if (videoUri == null) {
            return;
        }
        buildMediaSource(Uri.parse(videoUri));

        //Register the antplus data broadcast receiver
        antPlusBroadcastReceiver = new AntPlusBroadcastReceiver();

        IntentFilter filter = new IntentFilter("com.fitstream.ANTDATA");
        this.registerReceiver(antPlusBroadcastReceiver, filter);
    }

    private void initializePlayer() {
        if (player == null) {
            // 1. Create a default TrackSelector
            LoadControl loadControl = new DefaultLoadControl(
                    new DefaultAllocator(true, 16),
                    VideoPlayerConfig.MIN_BUFFER_DURATION,
                    VideoPlayerConfig.MAX_BUFFER_DURATION,
                    VideoPlayerConfig.MIN_PLAYBACK_START_BUFFER,
                    VideoPlayerConfig.MIN_PLAYBACK_RESUME_BUFFER, -1, true);

            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector =
                    new DefaultTrackSelector(videoTrackSelectionFactory);
            // 2. Create the player
            player =
                    ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), trackSelector,
                            loadControl);

            // Set speed of the video (hence buffering /streaming speed )
            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
            player.setPlaybackParameters(playbackParameters);
            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN );
            //Set player on playerview
            playerView.setPlayer(player);
        }
    }

    private void buildMediaSource(Uri mUri) {
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, getString(R.string.app_name)), bandwidthMeter);
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mUri);
        // Prepare the player with the source.
        player.prepare(videoSource);
        player.setPlayWhenReady(true);
        player.addListener(new VideoPlayerEventListener());

    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void pausePlayer() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }

    private void resumePlayer() {
        if (player != null) {
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }
    }

    private void getSelectedVideoUri() {
        SharedPreferences myPreferences = getSharedPreferences("app",0);
        final String movieUri = myPreferences.getString("selectedMovieUrl", null);
        this.videoUri = movieUri;
    }

}