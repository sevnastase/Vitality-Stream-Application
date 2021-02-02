package com.videostreamtest.ui.phone.videoplayer;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.videostreamtest.R;
import com.videostreamtest.service.ant.AntPlusBroadcastReceiver;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.ui.phone.result.ResultActivity;
import com.videostreamtest.utils.DistanceLookupTable;
import com.videostreamtest.utils.RpmVectorLookupTable;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoplayerActivity extends AppCompatActivity {

    private static final String TAG = VideoplayerActivity.class.getSimpleName();

    private static VideoplayerActivity thisInstance;

    private CastContext castContext;

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private String videoUri = VideoPlayerConfig.DEFAULT_VIDEO_URL;
    private AntPlusBroadcastReceiver antPlusBroadcastReceiver;

    private LinearLayout statusDialog;

    private boolean kioskmode = false;

    private TextView rpmValue;
    private int[] lastRpmMeasurements = new int[5];
    private int currentMeasurementIteration = 0;

    private float totalMetersRoute = 0;

    private boolean routePaused = false;

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

        statusDialog = findViewById(R.id.status_dialog_videoplayer);

        final TextView movieTitle = findViewById(R.id.movieTitle);
        SharedPreferences myPreferences = getSharedPreferences("app",0);
        movieTitle.setText(myPreferences.getString("selectedMovieTitle","Title not found"));
        totalMetersRoute = myPreferences.getFloat("selectedMovieTotalDistance", 0f);

        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);

        setUp();

        //Pause screen init
        final Button backToOverview = findViewById(R.id.status_dialog_return_home_button);
        backToOverview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                releasePlayer();
                finish();
            }
        });

        // START TEST CODE PAUSE SCREEN
//        Runnable showPauseScreen = new Runnable() {
//            public void run() {
//                updateVideoPlayerScreen(0);
//                updateVideoPlayerScreen(0);
//                updateVideoPlayerScreen(0);
//                updateVideoPlayerScreen(0);
//                updateVideoPlayerScreen(0);
//
//                updateVideoPlayerParams(0);
//
//                Runnable hidePauseScreen = new Runnable() {
//                    public void run() {
//                        updateVideoPlayerScreen(60);
//                        updateVideoPlayerScreen(60);
//                        updateVideoPlayerScreen(60);
//
//                        updateVideoPlayerParams(60);
//                    }
//                };
//                new Handler(Looper.getMainLooper()).postDelayed( hidePauseScreen, 8000 );
//            }
//        };
//        new Handler(Looper.getMainLooper()).postDelayed( showPauseScreen, 8000 );
        // END TEST CODE
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (kioskmode) {
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (player != null) {
                        //progressbar.setProgress((int) ((exoPlayer.getCurrentPosition()*100)/exoPlayer.getDuration()));
                        rpmValue.setText(toString().format(getString(R.string.video_screen_rpm), 60));
                        updateDistanceText();
                        handler.postDelayed(this::run, 1000);
                    }
                }
            };
            handler.postDelayed(runnable, 0);
        }
    }

    public static VideoplayerActivity getInstance() {
        return thisInstance;
    }

    public void updateSeekbar() {
        int currentProgresss = (int) (player.getCurrentPosition() * 1.0f / player.getDuration() * 100);
    }

    public void updateVideoPlayerScreen(int rpm) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //First update the measurements with the latest sensor data
                updateLastCadenceMeasurement(rpm);

                /* Update the on-screen data */
                //Update RPM
                rpmValue.setText(toString().format(getString(R.string.video_screen_rpm), rpm));
                //Update distance
                updateDistanceText();

                /* Pause mechanism  */
                //If the average measurement is 0 and the route is not paused then pause and show pause screen
                if (getAverageCadenceMeasurements() == 0 && !routePaused) {
                    togglePauseScreen();
                } else {
                    //If the route is paused and the average measurement is higher then 0 then unpause en remove pause screen
                    if (routePaused && getAverageCadenceMeasurements() > 0) {
                        togglePauseScreen();
                    }
                }
            }
        });
    }

    public void updateVideoPlayerParams(int rpm) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //If the route is not paused then pass params to the videoplayer
                if(!routePaused) {
                    //Setting the speed of the player based on our cadence rpm reading
                    PlaybackParameters playbackParameters = new PlaybackParameters(RpmVectorLookupTable.getPlaybackspeed(rpm), PlaybackParameters.DEFAULT.pitch);
                    player.setPlaybackParameters(playbackParameters);
                }
            }
        });
    }

    public void togglePauseScreen() {
        final TextView pauseTitle = findViewById(R.id.status_dialog_title);
        pauseTitle.setText(getString(R.string.pause_screen_title));
        final TextView pauseMessage = findViewById(R.id.status_dialog_message);
        pauseMessage.setText(getString(R.string.pause_screen_message));
        final ImageButton finishFlag = findViewById(R.id.status_dialog_finished_image);
        finishFlag.setVisibility(View.GONE);

        routePaused = !routePaused;
        player.setPlayWhenReady(!player.getPlayWhenReady());
        player.getPlaybackState();
        playerView.hideController();
        toggleStatusScreen();
    }

    public void showFinishScreen() {
        final TextView message = findViewById(R.id.status_dialog_title);
        message.setText(getString(R.string.finish_screen_title));
        final TextView pauseMessage = findViewById(R.id.status_dialog_message);
        pauseMessage.setText("");

        final ImageButton finishFlag = findViewById(R.id.status_dialog_finished_image);
        finishFlag.setBackground(getDrawable(R.drawable.finish_alpha));
        finishFlag.setVisibility(View.VISIBLE);

        toggleStatusScreen();
        playerView.hideController();
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

    private void toggleStatusScreen() {
        if(statusDialog.getVisibility() == View.GONE) {
            statusDialog.setVisibility(View.VISIBLE);
        } else {
            statusDialog.setVisibility(View.GONE);
        }
    }

    private void updateDistanceText() {
        if (player != null) {
            final TextView distance = findViewById(R.id.movieDistance);
            final float mps = DistanceLookupTable.getMeterPerSecond(totalMetersRoute, player.getDuration() / 1000);
            final int currentMetersDone = (int) (mps * (player.getCurrentPosition() / 1000));
            distance.setText(toString().format(getString(R.string.video_screen_distance), currentMetersDone));
        }
    }

    private void updateLastCadenceMeasurement(final int rpm){
        if (currentMeasurementIteration < lastRpmMeasurements.length) {
            lastRpmMeasurements[currentMeasurementIteration] = rpm;
            currentMeasurementIteration++;
        } else {
            currentMeasurementIteration = 0;
            lastRpmMeasurements[currentMeasurementIteration] = rpm;
            currentMeasurementIteration++;
        }
    }

    private int getAverageCadenceMeasurements() {
        int total = 0;
        for (int measurementIndex = 0; measurementIndex < lastRpmMeasurements.length; measurementIndex++) {
            total += lastRpmMeasurements[measurementIndex];
        }
        if (total > 0) {
            return total / lastRpmMeasurements.length;
        } else {
            return 0;
        }
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
        playerView.hideController();
    }

    private void initializePlayer() {
        if (player == null) {
            // Create the player
            final MediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);
            player = new SimpleExoPlayer.Builder(this).setMediaSourceFactory(mediaSourceFactory).build();

            // Set speed of the video (hence buffering /streaming speed )
            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
            player.setPlaybackParameters(playbackParameters);

            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            //Set player on playerview
            playerView.setPlayer(player);
        }
    }

    private void buildMediaSource(Uri mUri) {
        final MediaItem mediaItem = MediaItem.fromUri(mUri);
        player.setMediaItem(mediaItem);
        player.prepare();
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