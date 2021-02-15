package com.videostreamtest.ui.phone.videoplayer;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.videostreamtest.R;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.service.ant.AntPlusBroadcastReceiver;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.ui.phone.result.ResultActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.DistanceLookupTable;
import com.videostreamtest.utils.RpmVectorLookupTable;
import com.videostreamtest.workers.AvailableRoutePartsServiceWorker;

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

    private RecyclerView routePartsRecyclerview;
    private RoutePartsAdapter availableRoutePartsAdapter;

    private String videoUri = "http://46.101.137.215:5080/WebRTCApp/streams/989942527862373986107078.mp4";
    private int movieId = 0;
    private String accountKey;

    private AntPlusBroadcastReceiver antPlusBroadcastReceiver;

    private LinearLayout statusDialog;
    private RelativeLayout statusBar;
    private RelativeLayout loadingView;
    private int minSecondsLoadingView = 7;
    private boolean isLoading = true;

    private TextView rpmValue;
    private int[] lastRpmMeasurements = new int[5];
    private int currentMeasurementIteration = 0;

    private float totalMetersRoute = 0;

    private boolean routePaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisInstance = this;
        setContentView(R.layout.activity_videoplayer);

//        castContext = CastContext.getSharedInstance(this);

        playerView = findViewById(R.id.playerView);
        rpmValue = findViewById(R.id.movieRpm);

        statusDialog = findViewById(R.id.status_dialog_videoplayer);
        loadingView = findViewById(R.id.loading_view);
        statusBar = findViewById(R.id.route_content_overview);

        final TextView movieTitle = findViewById(R.id.movieTitle);
        SharedPreferences myPreferences = getSharedPreferences("app",0);
        movieTitle.setText(myPreferences.getString("selectedMovieTitle","Title not found"));
        movieId = myPreferences.getInt("selectedMovieId",0);
        totalMetersRoute = myPreferences.getFloat("selectedMovieTotalDistance", 0f);

        accountKey = myPreferences.getString("apiKey", null);

        //init recyclerview of route parts
        routePartsRecyclerview = findViewById(R.id.recyclerview_route_content_movieparts);
        routePartsRecyclerview.setHasFixedSize(true);
        //Maak lineaire layoutmanager en zet deze op horizontaal
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        routePartsRecyclerview.setLayoutManager(layoutManager);

        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);

        updateVideoPlayerScreen(0);

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

        waitUntilVideoIsReady();

        if (ApplicationSettings.DEVELOPER_MODE) {
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

    public void updateDeviceStatusField(String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView deviceStatusField = findViewById(R.id.antDeviceStatusField);
                deviceStatusField.setText("AntDeviceStatus: "+text);
            }
        });
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
                //Only show pause screen while the video is playing
                if (!isLoading) {
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

    public void setDeadDeviceParams() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateVideoPlayerScreen(0);
                updateDistanceText(true);
                toggleDeadDeviceScreen();
            }
        });
    }

    public void toggleDeadDeviceScreen() {
        Log.d(TAG, "Show Dead Device Display");
        final TextView pauseTitle = findViewById(R.id.status_dialog_title);
        pauseTitle.setText(getString(R.string.dead_device_screen_title));
        final TextView pauseMessage = findViewById(R.id.status_dialog_message);
        pauseMessage.setText(getString(R.string.dead_device_screen_message));
        final ImageButton finishFlag = findViewById(R.id.status_dialog_finished_image);
        finishFlag.setVisibility(View.GONE);

        LinearLayout routeParts = findViewById(R.id.route_layout_content_movieparts);
        routeParts.setVisibility(View.GONE);
        Button backToOverview = findViewById(R.id.status_dialog_return_home_button);
        backToOverview.requestFocus();

        player.setPlayWhenReady(false);
        player.pause();
        player.getPlaybackState();
        playerView.hideController();
        playerView.setUseController(false);
        toggleStatusScreen();
    }

    public void togglePauseScreen() {
        //Set new state of videoplayer
        routePaused = !routePaused;

        final TextView pauseTitle = findViewById(R.id.status_dialog_title);
        pauseTitle.setText(getString(R.string.pause_screen_title));
        final TextView pauseMessage = findViewById(R.id.status_dialog_message);
        pauseMessage.setText(getString(R.string.pause_screen_message));
        final ImageButton finishFlag = findViewById(R.id.status_dialog_finished_image);
        finishFlag.setVisibility(View.GONE);

        if (routePaused) {
            LinearLayout routeParts = findViewById(R.id.route_layout_content_movieparts);
            routeParts.setVisibility(View.GONE);
            Button backToOverview = findViewById(R.id.status_dialog_return_home_button);
            backToOverview.requestFocus();
        } else {
            LinearLayout routeParts = findViewById(R.id.route_layout_content_movieparts);
            routeParts.setVisibility(View.VISIBLE);
        }

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

        LinearLayout routeParts = findViewById(R.id.route_layout_content_movieparts);
        routeParts.setVisibility(View.GONE);

        finishFlag.requestFocus();

        toggleStatusScreen();
        playerView.setUseController(false);
        playerView.hideController();
    }

    public void goToFrameNumber(int frameNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (player.isCurrentWindowSeekable()) {
                    long positionSecond = 0;

                    playerView.setVisibility(View.GONE);
                    statusBar.setVisibility(View.GONE);
                    TextView loadingMessage = findViewById(R.id.loading_message);
                    loadingMessage.setText(getString(R.string.loading_message));
                    loadingView.setVisibility(View.VISIBLE);
                    player.pause();
                    playerView.hideController();

                    if (frameNumber > 30) {
                        positionSecond = frameNumber / 30;
                        player.seekTo(positionSecond*1000);
                    } else {
                        player.seekTo(0);
                    }

                    waitUntilVideoIsReady(3);
                    setFocusOnCurrentRoutePart();
                }
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

    private void toggleStatusScreen() {
        if(statusDialog.getVisibility() == View.GONE) {
            statusDialog.setVisibility(View.VISIBLE);
        } else {
            statusDialog.setVisibility(View.GONE);
        }
    }

    private void playVideo() {
        playerView.setVisibility(View.VISIBLE);
        statusBar.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        player.play();
        playerView.hideController();
    }

    private void setFocusOnCurrentRoutePart() {
        if(availableRoutePartsAdapter != null) {
            int currentPositionS = (int)(player.getCurrentPosition() / 1000);
            int currentFrameNumber = currentPositionS * 30;
            availableRoutePartsAdapter.setSelectedMoviePart(currentFrameNumber);
            routePartsRecyclerview.getAdapter().notifyDataSetChanged();
        }
    }

    private void waitUntilVideoIsReady() {
        waitUntilVideoIsReady(this.minSecondsLoadingView);
    }

    private void waitUntilVideoIsReady(final int minSecondsLoadingView) {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            int currentSecond = 0;
            @Override
            public void run() {
                if (player != null) {
                    if ( (currentSecond >= minSecondsLoadingView) && (player.getPlaybackState() == Player.STATE_READY)) {
                        isLoading = false;
                        playVideo();
                    } else {
                        isLoading = true;
                        Log.d(TAG, "CurrentSecondWaiting: "+currentSecond);
                        Log.d(TAG, "Player State: "+player.getPlaybackState());
                        currentSecond++;
                        handler.postDelayed(this::run, 1000);
                    }
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private void updateDistanceText(final boolean isDeadDevice) {
        final TextView distance = findViewById(R.id.movieDistance);
        if (isDeadDevice) {
            distance.setText(toString().format(getString(R.string.video_screen_distance), 0));
        } else {
            if (player != null) {
                final float mps = DistanceLookupTable.getMeterPerSecond(totalMetersRoute, player.getDuration() / 1000);
                final int currentMetersDone = (int) (mps * (player.getCurrentPosition() / 1000));
                distance.setText(toString().format(getString(R.string.video_screen_distance), currentMetersDone));
            }
            else {
                distance.setText(toString().format(getString(R.string.video_screen_distance), 0));
            }
        }
    }

    /**
     * Default value isDeadDevice = false
     */
    private void updateDistanceText() {
        updateDistanceText(false);
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
        prepareMediaSource(Uri.parse(videoUri));
        initializeRouteParts(accountKey, movieId);
        startSensorDataReceiver();
    }

    private void startSensorDataReceiver() {
        //Register the antplus data broadcast receiver
        antPlusBroadcastReceiver = new AntPlusBroadcastReceiver();

        IntentFilter filter = new IntentFilter("com.fitstream.ANTDATA");
        this.registerReceiver(antPlusBroadcastReceiver, filter);
    }

    private void initializePlayer() {
        if (player == null) {
            // Create the player
            final MediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);
            //DefaultLoadControl.Builder.setPrioritizeTimeOverSizeThresholds(true)
            DefaultLoadControl defaultLoadControl = new DefaultLoadControl.Builder().setPrioritizeTimeOverSizeThresholds(true).build();
            player = new SimpleExoPlayer.Builder(this).setLoadControl(defaultLoadControl).setMediaSourceFactory(mediaSourceFactory).build();

            // Set speed of the video (hence buffering /streaming speed )
            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
            player.setPlaybackParameters(playbackParameters);

            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            //Set player on playerview
            playerView.setPlayer(player);
        }
    }

    public void initializeRouteParts(final String apikey, final int movieId) {
        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);
        networkData.putInt("movieId", movieId);

        OneTimeWorkRequest routeMoviepartsRequest = new OneTimeWorkRequest.Builder(AvailableRoutePartsServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("available-movieparts")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(routeMoviepartsRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(routeMoviepartsRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("movieparts-list");

                        try {
                            final ObjectMapper objectMapper = new ObjectMapper();
                            MoviePart movieParts[] = objectMapper.readValue(result, MoviePart[].class);
                            //pass profiles to adapter
                            availableRoutePartsAdapter = new RoutePartsAdapter(movieParts);
                            //set adapter to recyclerview
                            routePartsRecyclerview.setAdapter(availableRoutePartsAdapter);
                            //set recyclerview visible
                            routePartsRecyclerview.setVisibility(View.VISIBLE);
                        } catch (JsonMappingException e) {
                            e.printStackTrace();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void prepareMediaSource(Uri mUri) {
        final MediaItem mediaItem = MediaItem.fromUri(mUri);
        player.setMediaItem(mediaItem);
        Log.d(TAG,"Player preparing!");
        player.prepare();

        player.addListener(new VideoPlayerEventListener());
        player.addListener(new Player.EventListener()
        {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == ExoPlayer.STATE_READY) {
                    Log.d(TAG,"Player ready to start playing!");
                }
            }
        });
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