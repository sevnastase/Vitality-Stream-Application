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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.EffectSound;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.enums.CommunicationType;
import com.videostreamtest.receiver.CadenceSensorBroadcastReceiver;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.ProductHelper;
import com.videostreamtest.ui.phone.result.ResultActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxFilmStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxFitStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxSpinStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsAdapter;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.RpmVectorLookupTable;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

/**
 * Full-screen videoplayer activity
 */
public class VideoplayerActivity extends AppCompatActivity {
    private static final String TAG = VideoplayerActivity.class.getSimpleName();
    private VideoPlayerViewModel videoPlayerViewModel;

    private static VideoplayerActivity thisInstance;

    private CastContext castContext;

    private PlayerView playerView;
    private SimpleExoPlayer videoPlayer;
    private SimpleExoPlayer backgroundSoundPlayer;
//    private SimpleExoPlayer effectSoundPlayer;

    //TODO: for later replacement or removal
    private RecyclerView routePartsRecyclerview;
    private RoutePartsAdapter availableRoutePartsAdapter;

    private String videoUri;
    private int movieId = 0;
    private CommunicationType communicationType;
    private CommunicationDevice communicationDevice;

    //TODO: Removal and set above items
    private Movie selectedMovie;
    private Product selectedProduct;

    private List<BackgroundSound> backgroundSoundList = new ArrayList<>();
    private List<EffectSound> effectSoundList = new ArrayList<>();

    private boolean isSoundOnDevice = false;
    private boolean isLocalPlay = false;

    private CadenceSensorBroadcastReceiver cadenceSensorBroadcastReceiver;

    private LinearLayout statusDialog;
    private RelativeLayout loadingView;

    private int minSecondsLoadingView = 7;
    private boolean isLoading = true;
    private boolean sensorConnected = false;

    private int[] lastRpmMeasurements = new int[5];
    private int currentMeasurementIteration = 0;

    private boolean routePaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisInstance = this;
        setContentView(R.layout.activity_videoplayer);
        videoPlayerViewModel = new ViewModelProvider(this).get(VideoPlayerViewModel.class);

        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

//        castContext = CastContext.getSharedInstance(this); TODO; Google cast implementation

        playerView = findViewById(R.id.playerView);
        playerView.setUseController(false);

        statusDialog = findViewById(R.id.status_dialog_videoplayer);
        loadingView = findViewById(R.id.loading_view);

        isSoundOnDevice = DownloadHelper.isSoundPresent(getApplicationContext());

        //Calculate rpm lookup table
        RpmVectorLookupTable.getPlaybackspeed(60);

        final Bundle arguments = getIntent().getExtras();
        if (arguments != null) {
            //Need selectedMovie to pass through viewmodel to statusbar fragments
            selectedMovie = new GsonBuilder().create().fromJson(arguments.getString("movieObject", "{}"), Movie.class);
            videoUri = selectedMovie.getMovieUrl();//NOT IMPORTANT ANYMORE AS WE"VE GOT THE MOVIE OBJECT
            movieId = selectedMovie.getId();
            communicationDevice = ConfigurationHelper.getCommunicationDevice(arguments.getString("communication_device"));
            isLocalPlay = arguments.getBoolean("localPlay");

            selectedProduct = new GsonBuilder().create().fromJson(arguments.getString("productObject", "{}"), Product.class);

            Log.d(TAG, "productObject :: " + selectedProduct.getProductName());

            if (selectedProduct.getProductName().contains("PraxFit")) {
                /*
                //INPUT SETTINGS
                CommunicationDevice.BLE (by default, per database te wijzigen naar ANT)
                CommunicationType.RPM (based on rpm realtime)
                */
                this.communicationType = ProductHelper.getCommunicationType(selectedProduct.getCommunicationType());

                videoPlayerViewModel.setSelectedMovie(selectedMovie);

                //BALK OPBOUWEN DOOR GEBRUIK FRAGMENTS
                getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.videoplayer_framelayout_statusbar, PraxFitStatusBarFragment.class, arguments)
                        .commit();

            }
            if (selectedProduct.getProductName().contains("PraxFilm")) {
                /*
                //INPUT SETTINGS
                CommunicationDevice.BLE (by default, per database te wijzigen naar ANT)
                CommunicationType.Active (based on activity of sensor, not adjusting speed in any kind)

                //BALK OPBOUWEN
                Rij 1: Tonen Filmtitel, verstreken(gereden) tijd, volume
                Rij 2: progressie balk (versleepbaar)
                 */
                this.communicationType = ProductHelper.getCommunicationType(selectedProduct.getCommunicationType());
                videoPlayerViewModel.setSelectedMovie(selectedMovie);
                getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.videoplayer_framelayout_statusbar, PraxFilmStatusBarFragment.class, null)
                        .commit();

            }
            if (selectedProduct.getProductName().contains("PraxSpin")) {
                /*
                Set default playing speed to 18 km/h
                Each step is 2 km/h harder or slower
                //INPUT SETTINGS
                CommunicationDevice.NONE
                CommunicationType.NONE (based on activity of sensor, not adjusting speed in any kind)

                //BALK OPBOUWEN
                Rij 1: Tonen Filmtitel, gereden tijd, gereden afstand, nog te rijden afstand, gem.snelheid (+/- optie).
                Rij 2: T1 - T6 routeparts laden
                 */
                this.communicationType = ProductHelper.getCommunicationType(selectedProduct.getCommunicationType());
                videoPlayerViewModel.setSelectedMovie(selectedMovie);

                getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.videoplayer_framelayout_statusbar, PraxSpinStatusBarFragment.class, arguments)
                        .commit();

                //Pass movie details with a second based timer
                Handler praxSpinHandler = new Handler();
                Runnable runnableMovieDetails = new Runnable() {
                    @Override
                    public void run() {
                        if (videoPlayer != null) {
                            videoPlayerViewModel.setMovieSpendDurationSeconds(videoPlayer.getCurrentPosition());
                            videoPlayerViewModel.setMovieTotalDurationSeconds(videoPlayer.getDuration());

                            praxSpinHandler.postDelayed(this::run, 1000);
                        }
                    }
                };
                praxSpinHandler.postDelayed(runnableMovieDetails, 0);

                videoPlayerViewModel.getKmhData().observe(this, kmhData ->{
                    if (kmhData != null && videoPlayer != null) {
                        Log.d(TAG, "SOUND PLAYER :: " + videoPlayer.getVolume());
                        Log.d(TAG, "SOUND DEVICE :: " + videoPlayer.getDeviceVolume());
                        Log.d(TAG, "MIN SOUND DEVICE :: " + videoPlayer.getDeviceInfo().minVolume);
                        Log.d(TAG, "MAX SOUND DEVICE :: " + videoPlayer.getDeviceInfo().maxVolume);
                        // Set speed of the video (hence buffering /streaming speed )
                        PlaybackParameters playbackParameters  = new PlaybackParameters(RpmVectorLookupTable.getPlayBackSpeedFromKmh(kmhData), PlaybackParameters.DEFAULT.pitch);
                        videoPlayer.setPlaybackParameters(playbackParameters);
                    }
                });
            }
        } else {
            //INCOMING FROM CatalogActivity.java
            SharedPreferences myPreferences = getSharedPreferences("app", 0);
            selectedMovie = new GsonBuilder().create().fromJson(myPreferences.getString("selectedMovieObject", "{}"), Movie.class);
            videoUri = selectedMovie.getMovieUrl();
            videoPlayerViewModel.setSelectedMovie(selectedMovie);
            communicationType = CommunicationType.RPM;
            communicationDevice = ApplicationSettings.SELECTED_COMMUNICATION_DEVICE;

            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.videoplayer_framelayout_statusbar, PraxFitStatusBarFragment.class, null)
                    .commit();
        }

        videoPlayerViewModel.getVolumeLevel().observe(this, volumeLevel -> {
            if (videoPlayer != null) {
                videoPlayer.setVolume(volumeLevel);
                backgroundSoundPlayer.setVolume(volumeLevel);
                //effectSoundPlayer.setVolume(volumeLevel);
            }
        });

        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);

        updateVideoPlayerScreen(66);

        setUp();

        //Pause screen init
        final Button backToOverview = findViewById(R.id.status_dialog_return_home_button);
        backToOverview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                releasePlayers();
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
                    if (videoPlayer != null) {
                        //progressbar.setProgress((int) ((exoPlayer.getCurrentPosition()*100)/exoPlayer.getDuration()));
                        videoPlayerViewModel.setRpmData(60);//new Random().nextInt(80));
                        videoPlayerViewModel.setMovieSpendDurationSeconds(videoPlayer.getCurrentPosition());
                        videoPlayerViewModel.setMovieTotalDurationSeconds(videoPlayer.getDuration());

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

    public void updateVideoPlayerScreen(int rpm) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //First update the measurements with the latest sensor data
                updateLastCadenceMeasurement(rpm);

                if (videoPlayer != null) {
                    videoPlayerViewModel.setMovieSpendDurationSeconds(videoPlayer.getCurrentPosition());
                    videoPlayerViewModel.setMovieTotalDurationSeconds(videoPlayer.getDuration());
                }

                /* Update the on-screen data based on CommunicationType */
                switch (communicationType) {
                    case RPM:
                        //Boolean to unlock video because sensor is connected
                        sensorConnected = rpm>0;
                        //Update RPM
                        videoPlayerViewModel.setRpmData(rpm);
                        break;
                    case ACTIVE:
                        //Boolean to unlock video because sensor is connected
                        sensorConnected = rpm>0;
                        //Update RPM
                        videoPlayerViewModel.setRpmData(rpm);
                        break;
                    case NONE:
                        sensorConnected = true;
                        break;
                    default:
                }
                Log.d(TAG, "RPM: "+rpm+" sensorConnected: "+sensorConnected);

                /* Pause mechanism  */
                //Only show pause screen while the video is not in loading state
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
                    /* Update the video player */
                    switch (communicationType) {
                        case RPM:
                            //Set the speed of the player based on our cadence rpm reading
                            PlaybackParameters playbackRpmParameters = new PlaybackParameters(RpmVectorLookupTable.getPlaybackspeed(rpm), PlaybackParameters.DEFAULT.pitch);
                            videoPlayer.setPlaybackParameters(playbackRpmParameters);

                            break;
                        case ACTIVE:

                            break;
                        case NONE:
                            // This clause will never be executed as there is no rpm data
                            break;
                        default:
                    }
                }
            }
        });
    }

    public void setDeadDeviceParams() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateVideoPlayerScreen(0);
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

        videoPlayerViewModel.setStatusbarVisible(false);
        Button backToOverview = findViewById(R.id.status_dialog_return_home_button);
        backToOverview.requestFocus();

        videoPlayer.setPlayWhenReady(false);
        videoPlayer.pause();
        videoPlayer.getPlaybackState();
        playerView.hideController();
        playerView.setUseController(false);
        toggleStatusScreen();
    }

    public void togglePauseScreen() {
        //Set new state of videoplayer
        routePaused = !routePaused;
        videoPlayerViewModel.setPlayerPaused(routePaused);

        final TextView pauseTitle = findViewById(R.id.status_dialog_title);
        pauseTitle.setText(getString(R.string.pause_screen_title));
        final TextView pauseMessage = findViewById(R.id.status_dialog_message);
        pauseMessage.setText(getString(R.string.pause_screen_message));
        final ImageButton finishFlag = findViewById(R.id.status_dialog_finished_image);
        finishFlag.setVisibility(View.GONE);

        if (routePaused) {
            videoPlayerViewModel.setStatusbarVisible(false);
            Button backToOverview = findViewById(R.id.status_dialog_return_home_button);
            backToOverview.requestFocus();
        } else {
            videoPlayerViewModel.setStatusbarVisible(true);
        }

        videoPlayer.setPlayWhenReady(!videoPlayer.getPlayWhenReady());
        videoPlayer.getPlaybackState();
        playerView.hideController();
        toggleStatusScreen();
    }

    public void showFinishScreen() {
        videoPlayerViewModel.setPlayerPaused(true);
        final TextView message = findViewById(R.id.status_dialog_title);
        message.setText(getString(R.string.finish_screen_title));
        final TextView pauseMessage = findViewById(R.id.status_dialog_message);
        pauseMessage.setText("");

        final ImageButton finishFlag = findViewById(R.id.status_dialog_finished_image);
        finishFlag.setBackground(getDrawable(R.drawable.finish_alpha));
        finishFlag.setVisibility(View.VISIBLE);

        FrameLayout statusbar = findViewById(R.id.videoplayer_framelayout_statusbar);
        statusbar.setVisibility(View.GONE);

        finishFlag.requestFocus();

        toggleStatusScreen();
        playerView.setUseController(false);
        playerView.hideController();
    }

    public void goToFrameNumber(int frameNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (videoPlayer.isCurrentWindowSeekable()) {
                    long positionSecond = 0;

                    videoPlayerViewModel.setStatusbarVisible(false);
                    videoPlayerViewModel.setPlayerPaused(true);
                    playerView.setVisibility(View.GONE);

                    TextView loadingMessage = findViewById(R.id.loading_message);
                    loadingMessage.setText(getString(R.string.loading_message));
                    loadingView.setVisibility(View.VISIBLE);
                    videoPlayer.pause();
                    if (backgroundSoundPlayer !=  null) {
                        backgroundSoundPlayer.pause();
                    }
//                    if (effectSoundPlayer != null) {
//                        effectSoundPlayer.pause();
//                    }
                    playerView.hideController();

                    int fps = selectedMovie.getRecordedFps();
                    if (frameNumber > fps) {
                        positionSecond = frameNumber / fps;
                        videoPlayer.seekTo(positionSecond*1000);
                    } else {
                        videoPlayer.seekTo(0);
                    }

                    waitUntilVideoIsReady(3);
//                    setFocusOnCurrentRoutePart(); //MOVE TO FRAGMENT (DISPLAY WHEN NECESSARY)
                }
            }
        });
    }

    public void setVolumeHigher() {
        if (videoPlayer != null) {
            float currentVolume = videoPlayer.getVolume();
            videoPlayer.setVolume(currentVolume+0.1f);
        }
    }
    public void setVolumeLower() {
        if (videoPlayer != null) {
            float currentVolume = videoPlayer.getVolume();
            videoPlayer.setVolume(currentVolume-0.1f);
        }
    }

    public void startResultScreen() {

        //Stop Ant+ service
        final Intent antplusService = new Intent(getApplicationContext(), AntPlusService.class);
        stopService(antplusService);

        // Build result screen
        final Intent resultScreen = new Intent(getApplicationContext(), ResultActivity.class);
        startActivity(resultScreen);
        //Release player and finish activity
        releasePlayers();
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
    protected void onStop() {
        super.onStop();
        releasePlayers();
        playerView.setPlayer(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSensorService();
        try {
            this.unregisterReceiver(cadenceSensorBroadcastReceiver);
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e(TAG, illegalArgumentException.getLocalizedMessage());
        }
        releasePlayers();
        playerView.setPlayer(null);
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
        loadingView.setVisibility(View.GONE);
        videoPlayerViewModel.setStatusbarVisible(true);
        videoPlayer.play();

        if (getCurrentBackgroundSoundByCurrentPostion() != null) {
            switchToNewBackgroundMedia(getCurrentBackgroundSoundByCurrentPostion().getSoundUrl());
        }

        setTimeLineEventVideoPlayer();
        playerView.hideController();
    }

    //TODO: implement later in fragment if needed
    private void setFocusOnCurrentRoutePart() {
        if(availableRoutePartsAdapter != null) {
            int currentPositionS = (int)(videoPlayer.getCurrentPosition() / 1000);
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
                if (videoPlayer != null) {
                    if (    (currentSecond >= minSecondsLoadingView) &&
                            isPraxtourMediaPlayerReady() &&
                            (sensorConnected || ApplicationSettings.DEVELOPER_MODE)
                    ) {
                        isLoading = false;
                        playVideo();
                    } else {
                        isLoading = true;
                        Log.d(TAG, "CurrentSecondWaiting: "+currentSecond);
                        Log.d(TAG, "Player State: "+ videoPlayer.getPlaybackState());
                        currentSecond++;
                        handler.postDelayed(this::run, 1000);
                    }
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private void setTimeLineEventVideoPlayer() {
        Handler timelineHandler = new Handler();
        Runnable runnableMovieDetails = new Runnable() {
            @Override
            public void run() {
                //Asssert of videoplayer remains as it is the main priority
                if (videoPlayer != null) {
                    checkBackgroundSoundMedia();
//                    checkEffectSoundMedia();
                    timelineHandler.postDelayed(this::run, 1000);
                }
            }
        };
        timelineHandler.postDelayed(runnableMovieDetails, 0);
    }

    private void checkBackgroundSoundMedia() {
        if (backgroundSoundList.size()>0) {
            long currentSecond = (videoPlayer.getCurrentPosition() / 1000L);
            for (final BackgroundSound backgroundSound : backgroundSoundList) {
                if ((backgroundSound.getFramenumber()/selectedMovie.getRecordedFps()) == currentSecond) {
                    switchToNewBackgroundMedia(backgroundSound.getSoundUrl());
                }
            }
        }
    }

    private void switchToNewBackgroundMedia(final String backgroundSoundurl) {
        if (backgroundSoundPlayer.getMediaItemCount()>0) {
            Uri soundItemUri = backgroundSoundPlayer.getCurrentMediaItem().playbackProperties.uri;
            String localFileName = soundItemUri.getPath().substring(soundItemUri.getPath().lastIndexOf('/'), soundItemUri.getPath().length());
            //If new item is the same then return
            if (backgroundSoundurl.contains(localFileName)) {
                //Check if already playing else start playing
                if (!backgroundSoundPlayer.isPlaying()){
                    backgroundSoundPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                    backgroundSoundPlayer.play();
                }
                return;
            }
            for (int bgIndex = 0; bgIndex < backgroundSoundPlayer.getMediaItemCount();bgIndex++) {
                if (backgroundSoundurl.contains(backgroundSoundPlayer.getMediaItemAt(bgIndex).playbackProperties.uri.getPath())) {
                    if (backgroundSoundPlayer.isPlaying()) {
                        backgroundSoundPlayer.pause();
                    }
                    backgroundSoundPlayer.moveMediaItem(bgIndex, 1);
                    backgroundSoundPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
                    backgroundSoundPlayer.next();
                    backgroundSoundPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                    backgroundSoundPlayer.play();
                }
            }
        }
    }

    /**
     * Check which background sound should be playing at the the current position
     * @return BackgroundSound
     */
    private BackgroundSound getCurrentBackgroundSoundByCurrentPostion() {
        BackgroundSound selectBackgroundSound = null;
        long currentSecond = (videoPlayer.getCurrentPosition() / 1000L);
        if (backgroundSoundList.size()>0) {
            for (BackgroundSound backgroundSound: backgroundSoundList) {
                int backgroundsoundTriggerSecond = backgroundSound.getFramenumber()/selectedMovie.getRecordedFps();

                if (currentSecond > backgroundsoundTriggerSecond) {
                    if (selectBackgroundSound != null) {
                        if (currentSecond > (selectBackgroundSound.getFramenumber()/selectedMovie.getRecordedFps())) {
                            selectBackgroundSound = backgroundSound;
                        }
                    } else
                    {
                        selectBackgroundSound = backgroundSound;
                    }
                }
            }
        }
        return selectBackgroundSound;
    }

//    private void checkEffectSoundMedia() {
//        if (effectSoundList.size()>0) {
//            long currentSecond = (videoPlayer.getCurrentPosition() / 1000L);
//            for (final EffectSound effectSound : effectSoundList) {
//                if ((effectSound.getFramenumber()/selectedMovie.getRecordedFps()) == currentSecond) {
//                    startEffectSound(effectSound.getSoundUrl());
//                }
//            }
//        }
//    }

//    private void startEffectSound(final String effectSoundUrl) {
//        if (effectSoundPlayer.getMediaItemCount()>0) {
//            for (int effectIndex = 0; effectIndex < effectSoundPlayer.getMediaItemCount();effectIndex++) {
//                Uri soundItemUri = effectSoundPlayer.getMediaItemAt(effectIndex).playbackProperties.uri;
//                String localFileName = soundItemUri.getPath().substring(soundItemUri.getPath().lastIndexOf('/'), soundItemUri.getPath().length());
//
//                if (effectSoundUrl.contains(localFileName)) {
//                    Log.d(TAG, "EFFECT SOUND FOUND URL :: "+effectSoundUrl);
//                    if (effectSoundPlayer.isPlaying()) {
//                        effectSoundPlayer.pause();
//                    }
//                    effectSoundPlayer.moveMediaItem(effectIndex, 1);
//                    effectSoundPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
//                    effectSoundPlayer.next();
//                    effectSoundPlayer.play();
//                }
//            }
//        }
//    }

    private boolean isPraxtourMediaPlayerReady() {
        boolean videoPlayerReady = false;
        boolean backgroundPlayerReady = false;
//        boolean effectSoundPlayerReady = false;

        if (videoPlayer.getMediaItemCount()>0) {
            videoPlayerReady = (videoPlayer.getPlaybackState() == Player.STATE_READY);
        }
        if (backgroundSoundPlayer.getMediaItemCount()>0) {
            backgroundPlayerReady = (backgroundSoundPlayer.getPlaybackState() == Player.STATE_READY);
        } else {
            backgroundPlayerReady = true;
        }
//        if (effectSoundPlayer.getMediaItemCount()>0) {
//            effectSoundPlayerReady = (effectSoundPlayer.getPlaybackState() == Player.STATE_READY);
//        } else {
//            effectSoundPlayerReady = true;
//        }

        return (videoPlayerReady || backgroundPlayerReady);// || effectSoundPlayerReady);
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
        //START INIT SENSORS
        startSensorService();

        //START INIT VIDEO
        initializeVideoPlayer();
        if (videoUri == null) {
            return;
        }
        //PREPARE SOURCE FOR PLAY
        prepareVideoMediaSource(Uri.parse(videoUri));

        //START INIT BACKGROUND SOUND
        initializeBackgroundSoundPlayer();
        prepareBackgroundSoundPlayer();

        //START INIT EFFECT SOUND
//        initializeEffectSoundPlayer();
//        prepareEffectSoundPlayer();

        //START DATA RECEIVERS
        if (communicationType != CommunicationType.NONE) {
            startSensorDataReceiver();
        }
    }

    private void startSensorService() {
        if (!ApplicationSettings.DEVELOPER_MODE) {
            switch (communicationDevice) {
                case ANT_PLUS:
                    //Start AntPlus service to connect with cadence sensor
                    Intent antplusService = new Intent(getApplicationContext(), AntPlusService.class);
                    startService(antplusService);
                    break;
                case BLE:
                    //Start BLE service to connect with cadence sensor
                    Intent bleService = new Intent(getApplicationContext(), BleService.class);
                    startService(bleService);
                default:
                    //NONE
            }
        }
    }

    private void startSensorDataReceiver() {
        //Register the cadence sensor data broadcast receiver
        cadenceSensorBroadcastReceiver = new CadenceSensorBroadcastReceiver();
        IntentFilter filter = new IntentFilter(ApplicationSettings.COMMUNICATION_INTENT_FILTER);
        this.registerReceiver(cadenceSensorBroadcastReceiver, filter);
    }

    private void initializeVideoPlayer() {
        if (videoPlayer == null) {
            // Create the player
            final MediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);

//            Trackselector to retrieve and start the best track in mp4
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(getApplicationContext());
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                        .setForceHighestSupportedBitrate(true)
                        .build()
            );

            DefaultLoadControl defaultLoadControl = new DefaultLoadControl.Builder()
                    .setPrioritizeTimeOverSizeThresholds(true)
//                    .setBufferDurationsMs(50000,50000, 20000,20000)
                    .build();

            videoPlayer = new SimpleExoPlayer.Builder(this)
                    .setLoadControl(defaultLoadControl)
                    //.setTrackSelector(trackSelector) Not using the trackselector
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build();

            // Set speed of the video (hence buffering /streaming speed )
            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
            videoPlayer.setPlaybackParameters(playbackParameters);

            //Set hardware sound to maximum
            videoPlayer.setDeviceVolume(videoPlayer.getDeviceInfo().maxVolume);

            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            //Set player on playerview
            playerView.setPlayer(videoPlayer);
        }
    }

    private void initializeBackgroundSoundPlayer() {
        if (backgroundSoundPlayer == null) {
            final MediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);
            DefaultLoadControl defaultLoadControl = new DefaultLoadControl.Builder().setPrioritizeTimeOverSizeThresholds(true).build();
            backgroundSoundPlayer = new SimpleExoPlayer.Builder(this).setLoadControl(defaultLoadControl).setMediaSourceFactory(mediaSourceFactory).build();

            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
            backgroundSoundPlayer.setPlaybackParameters(playbackParameters);

        }
    }

//    private void initializeEffectSoundPlayer() {
//        if (effectSoundPlayer == null) {
//            final MediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);
//            DefaultLoadControl defaultLoadControl = new DefaultLoadControl.Builder().setPrioritizeTimeOverSizeThresholds(true).build();
//            effectSoundPlayer = new SimpleExoPlayer.Builder(this).setLoadControl(defaultLoadControl).setMediaSourceFactory(mediaSourceFactory).build();
//
//            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
//            effectSoundPlayer.setPlaybackParameters(playbackParameters);
//        }
//    }

    private void prepareVideoMediaSource(Uri mUri) {
        final MediaItem mediaItem = MediaItem.fromUri(mUri);
        videoPlayer.setMediaItem(mediaItem);
        videoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        Log.d(TAG,"Player preparing!");
        videoPlayer.prepare();

        videoPlayer.addListener(new VideoPlayerEventListener());
        videoPlayer.addListener(new Player.EventListener()
        {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == ExoPlayer.STATE_READY) {
                    Log.d(TAG,"Player ready to start playing!");
                }
            }
        });
    }

    private void prepareBackgroundSoundPlayer() {
        if (movieId != 0) {
            videoPlayerViewModel.getBackgroundSounds(movieId).observe(this, backgroundSounds -> {
                backgroundSoundList = backgroundSounds;
                List<Uri> backgroundSoundUriList = new ArrayList<>();
                if (backgroundSounds.size()>0) {
                    for (BackgroundSound backgroundSound: backgroundSounds) {
                        backgroundSoundUriList.add(Uri.parse(backgroundSound.getSoundUrl()));
                    }
                }

                prepareBackgroundSoundMediaSources(backgroundSoundUriList);
            });
        }
    }

    private void prepareBackgroundSoundMediaSources(final List<Uri> backgroundSounds) {
        if (backgroundSounds.size()>0) {
            for (Uri uri: backgroundSounds) {
                if (isSoundOnDevice) {
                    uri = DownloadHelper.getLocalSound(getApplicationContext(), uri);
                }

                MediaItem mediaItem = MediaItem.fromUri(uri);
                backgroundSoundPlayer.addMediaItem(mediaItem);
            }
            backgroundSoundPlayer.prepare();
        }
    }

//    private void prepareEffectSoundPlayer() {
//        if (movieId != 0) {
//            videoPlayerViewModel.getEffectSounds(movieId).observe(this, effectSounds -> {
//                effectSoundList = effectSounds;
//                List<Uri> effectSoundUriList = new ArrayList<>();
//                if (effectSounds.size()>0) {
//                    for (EffectSound effectSound: effectSounds) {
//                        effectSoundUriList.add(Uri.parse(effectSound.getSoundUrl()));
//                    }
//                }
//
//                prepareEffectSoundMediaSources(effectSoundUriList);
//            });
//        }
//    }

//    private void prepareEffectSoundMediaSources(final List<Uri> effectSounds) {
//        if (effectSounds.size()>0) {
//            for (Uri uri: effectSounds) {
//                if (isSoundOnDevice) {
//                    uri = DownloadHelper.getLocalSound(getApplicationContext(), uri);
//                }
//
//                MediaItem mediaItem = MediaItem.fromUri(uri);
//                effectSoundPlayer.addMediaItem(mediaItem);
//            }
//            effectSoundPlayer.prepare();
//        }
//    }

    private void releasePlayers() {
        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }
        if (backgroundSoundPlayer != null) {
            backgroundSoundPlayer.release();
            backgroundSoundPlayer = null;
        }

//        if (effectSoundPlayer != null) {
//            effectSoundPlayer.release();
//            effectSoundPlayer = null;
//        }
    }

    private void pausePlayer() {
        if (videoPlayer != null) {
            videoPlayer.setPlayWhenReady(false);
            videoPlayer.getPlaybackState();
        }
    }

    private void resumePlayer() {
        if (videoPlayer != null) {
            videoPlayer.setPlayWhenReady(true);
            videoPlayer.getPlaybackState();
        }
    }

    private void stopSensorService() {
        switch(communicationDevice) {
            case ANT_PLUS:
                final Intent antplusService = new Intent(getApplicationContext(), AntPlusService.class);
                stopService(antplusService);
                break;
            case BLE:
                final Intent bleService = new Intent(getApplicationContext(), BleService.class);
                stopService(bleService);
                break;
            default:
        }
    }

}