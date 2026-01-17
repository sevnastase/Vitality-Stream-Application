package com.videostreamtest.ui.phone.videoplayer;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

//import com.google.android.exoplayer2.DefaultLoadControl;
//import com.google.android.exoplayer2.ExoPlayer;
//import com.google.android.exoplayer2.MediaItem;
//import com.google.android.exoplayer2.PlaybackParameters;
//import com.google.android.exoplayer2.Player;
//import com.google.android.exoplayer2.SimpleExoPlayer;
//import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
//import com.google.android.exoplayer2.source.MediaSourceFactory;
//import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
//import com.google.android.exoplayer2.ui.PlayerView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.media3.exoplayer.ExoPlayer;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.enums.CommunicationType;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.receiver.CadenceSensorBroadcastReceiver;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.helpers.ConfigurationHelper;
import com.videostreamtest.helpers.DownloadHelper;
import com.videostreamtest.helpers.LogHelper;
import com.videostreamtest.helpers.ProductHelper;
import com.videostreamtest.helpers.SoundHelper;
import com.videostreamtest.ui.phone.result.ResultActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxFilmStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxFitStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxSpinStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxViewStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.RpmVectorLookupTable;
import com.videostreamtest.utils.VideoLanLib;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Full-screen videoplayer activity
 */
public class VideoplayerActivity extends AppCompatActivity {
    private static final String TAG = VideoplayerActivity.class.getSimpleName();

    private static final int MAX_PAUSE_TIME_SEC = 55;
    private static final int AUTO_BACK_TO_OVERVIEW_SECONDS = 10;
    private VideoPlayerViewModel videoPlayerViewModel;

    private static VideoplayerActivity thisInstance;

    private CastContext castContext;

    private PlayerView playerView;
    private ExoPlayer videoPlayer;
    private ExoPlayer backgroundSoundPlayer;//TODO: Test if can be replaced by VLC MediaPlayer
    private CadenceSensorBroadcastReceiver cadenceSensorBroadcastReceiver;

    //VLC
    private VLCVideoLayout videoLayout;
    private MediaPlayer mediaPlayer;

    private String videoUri;
    private int movieId = 0;
    private CommunicationType communicationType;
    private CommunicationDevice communicationDevice;

    private Movie selectedMovie;
    private Product selectedProduct;

    private List<BackgroundSound> backgroundSoundTriggers = new ArrayList<>();
    private List<MediaItem> backgroundMediaItems = new ArrayList<>();

    private boolean isSoundOnDevice = false;
    private boolean isLocalPlay = false;

    private LinearLayout statusDialog;
    private RelativeLayout loadingView;

    private Button backToOverview;

    /**
     * Chinesport: no BLE and always local play: can be quicker
     * <p></p>
     * Regular accounts: BLE and could be streaming: safer if we give it time
     */
    private final int MIN_LOADING_VIEW_SECONDS =
            AccountHelper.isChinesportAccount(PraxtourApplication.getAppContext()) ?
                    2 : 7;
    private boolean isLoading = true;
    private boolean sensorConnected = false;

    private int[] lastRpmMeasurements = new int[5];
    private int currentMeasurementIteration = 0;
    private int numberOfFalsePositives = 0;

    private boolean routePaused = false;
    private int pauseTimer = 0;
    private boolean routeFinished = false;
    private final Handler autoRunnerHandler = new Handler(Looper.getMainLooper());
    private final Handler praxHandler = new Handler(Looper.getMainLooper());
    private final Handler timelineHandler = new Handler(Looper.getMainLooper());
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    //BLE
    private boolean backToOverviewWaitForSensor = false;

    //MQTT
    private int rpmMqtt;
    private final BroadcastReceiver mqttBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case "com.videostreamtest.MQTT_DATA_UPDATE":
                    processIncomingData(intent);
                    break;
                case "videoplayer_finish_film":
                    if (videoPlayerViewModel != null) {
                        videoPlayerViewModel.setVolumeLevel(0);
                    }
                    showFinishScreen();
                    break;
                case "com.videostreamtest.ACTION_STOP_FILM":
                    stopVideoplayer();
                    break;
            }

        }
    };

    //CHROMECAST
//    List<RendererDiscoverer> rendererDiscovererList = new ArrayList<>();
//    List<RendererItem> rendererItemList = new ArrayList<>();

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

        //EXO player
        playerView = findViewById(R.id.google_exoplayer_view);
        playerView.setUseController(false);

        // VLC PLayer
        videoLayout = findViewById(R.id.vlc_player_view);

        statusDialog = findViewById(R.id.status_dialog_videoplayer);
        loadingView = findViewById(R.id.loading_view);

        isSoundOnDevice = DownloadHelper.isSoundPresent(getApplicationContext());

//        libVLC = createLibVLC();

        //Calculate rpm lookup table
        RpmVectorLookupTable.getPlaybackspeed(60);

        final Bundle arguments = getIntent().getExtras();
        if (arguments != null) {
            //Need selectedMovie to pass through viewmodel to statusbar fragments
            selectedMovie = new GsonBuilder().create().fromJson(arguments.getString("movieObject", "{}"), Movie.class);
            videoUri = selectedMovie.getMovieUrl();//NOT IMPORTANT ANYMORE AS WE"VE GOT THE MOVIE OBJECT
            movieId = selectedMovie.getId();
            communicationDevice = ConfigurationHelper.getCommunicationDevice(ProductHelper.getCommunicationType(arguments.getString("communication_device")));
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
                        .replace(R.id.videoplayer_framelayout_statusbar, PraxFilmStatusBarFragment.class, arguments)
                        .commit();

//                //Pass movie details with a second based timer TODO:move to setTimeLineEvent method in this class
//                Handler praxHandler = new Handler();
//                Runnable runnableMovieDetails = new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mediaPlayer != null && !routeFinished) {
//                            Log.d(TAG, "TIME "+mediaPlayer.getTime());
//                            videoPlayerViewModel.setMovieSpendDurationSeconds(mediaPlayer.getTime());
//                            if (mediaPlayer.getMedia()!= null && mediaPlayer.getMedia().getDuration() != -1) {
//                                Log.d(TAG, "DURATION "+mediaPlayer.getMedia().getDuration());
//                                videoPlayerViewModel.setMovieTotalDurationSeconds(mediaPlayer.getMedia().getDuration());
//                            }
//
//                        }
//                        if (!routeFinished) {
//                            praxHandler.postDelayed(this::run, 1000);
//                        }
//                    }
//                };
//                praxHandler.post(runnableMovieDetails);

            }
            if (selectedProduct.getProductName().contains("PraxView")) {
                this.communicationType = ProductHelper.getCommunicationType(selectedProduct.getCommunicationType());
                videoPlayerViewModel.setSelectedMovie(selectedMovie);

                getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.videoplayer_framelayout_statusbar, PraxViewStatusBarFragment.class, arguments)
                        .commit();

                videoPlayerViewModel.setMovieTotalDurationSeconds(selectedMovie.getMovieLength());
                //Pass movie details with a second based timer TODO:move to setTimeLineEvent method in this class
                Runnable runnableMovieDetails = new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && !routeFinished) {
                            Log.d(TAG, "TIME "+mediaPlayer.getTime());
                            videoPlayerViewModel.setMovieSpendDurationSeconds(mediaPlayer.getTime());
                            if (mediaPlayer.getMedia()!= null && mediaPlayer.getMedia().getDuration() != -1) {
                                Log.d(TAG, "DURATION "+mediaPlayer.getMedia().getDuration());
                                videoPlayerViewModel.setMovieTotalDurationSeconds(mediaPlayer.getMedia().getDuration());
                            }

                        }
                        if (!routeFinished) {
                            praxHandler.postDelayed(this, 1000);
                        }
                    }
                };
                praxHandler.post(runnableMovieDetails);
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

                //Pass movie details with a second based timer TODO:move to setTimeLineEvent method in this class

                Runnable runnableMovieDetails = new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && !routeFinished) {
                            Log.d(TAG, "TIME "+mediaPlayer.getTime());
                            videoPlayerViewModel.setMovieSpendDurationSeconds(mediaPlayer.getTime());
                            if (mediaPlayer.getMedia()!= null && mediaPlayer.getMedia().getDuration() != -1) {
                                Log.d(TAG, "DURATION "+mediaPlayer.getMedia().getDuration());
                                videoPlayerViewModel.setMovieTotalDurationSeconds(mediaPlayer.getMedia().getDuration());
                            }

                        }
                        if (!routeFinished) {
                            praxHandler.postDelayed(this, 1000);
                        }
                    }
                };
                praxHandler.post(runnableMovieDetails);

                videoPlayerViewModel.getKmhData().observe(this, kmhData ->{
                    if (kmhData != null && mediaPlayer != null) {
                        // Set speed of the video (hence buffering /streaming speed )
                        if (mediaPlayer!= null) {
                            mediaPlayer.setRate(RpmVectorLookupTable.getPlayBackSpeedFromKmh(kmhData));
                        }
                    }
                });
            }
        } else {
            //INCOMING FROM CatalogActivity
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

        if (!communicationType.equals(CommunicationType.RPM)) {
            View statusbarContainer = findViewById(R.id.videoplayer_framelayout_statusbar);
            statusbarContainer.setVisibility(View.INVISIBLE);
            praxHandler.postDelayed(() -> statusbarContainer.setVisibility(View.VISIBLE), MIN_LOADING_VIEW_SECONDS * 1000L);
        }

        if (AccountHelper.isChinesportAccount(this)) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.videostreamtest.MQTT_DATA_UPDATE");
            filter.addAction("videoplayer_finish_film");
            filter.addAction("com.videostreamtest.ACTION_STOP_FILM");
            LocalBroadcastManager.getInstance(this).registerReceiver(mqttBroadcastReceiver, filter);
        }

        videoPlayerViewModel.getVolumeLevel().observe(this, volumeLevel -> {
            Log.d(TAG, "volumeLevel: " + volumeLevel);
            if (mediaPlayer!=null) {
                mediaPlayer.setVolume(volumeLevel);
                if (backgroundSoundTriggers!= null && !backgroundSoundTriggers.isEmpty()) {
                    final float bgVolumeLevel = volumeLevel / 100f;
                    backgroundSoundPlayer.setVolume(bgVolumeLevel);
                }
            }
        });

        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);
        updateLastCadenceMeasurement(66);

        updateVideoPlayerScreen(0);

        setUp();

        //Pause screen init
        backToOverview = findViewById(R.id.status_dialog_return_home_button);
        backToOverview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                backToOverviewWaitForSensor = true;
                backToOverview.setClickable(false);
                //HANDLED FURTHER in #setTimeLineEventVideoPlayer()
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (videoUri == null) {
            return;
        }
        initializeVlcVideoPlayer();

        waitUntilVideoIsReady();
        setTimeLineEventVideoPlayer();
//        discoverChromecasts();

        if (ApplicationSettings.DEVELOPER_MODE) {
            Runnable controller = autoRunner();
            autoRunnerHandler.post(controller);
        }
    }

    @NonNull
    private Runnable autoRunner() {
        Runnable r1 = new Runnable() {
            @Override
            public void run() {
                sensorConnected = true;
                updateVideoPlayerParams(60);
                updateVideoPlayerScreen(60);
            }
        };
        Runnable r2 = new Runnable() {
            @Override
            public void run() {
                sensorConnected = true;
                updateVideoPlayerParams(42);
                updateVideoPlayerScreen(42);
            }
        };

        return new Runnable() {
            int secondsPassed = 0;
            @Override
            public void run() {
                if (secondsPassed > 20) secondsPassed = 0;
                if (secondsPassed < 10) {
                    autoRunnerHandler.post(r1);
                } else {
                    autoRunnerHandler.post(r2);
                }
                secondsPassed++;
                autoRunnerHandler.postDelayed(this, 1000);
            }
        };
    }

    public static VideoplayerActivity getInstance() {
        return thisInstance;
    }

    //VLC stuff

    private void setVideoFeatures() {
        mediaPlayer.setVideoTrackEnabled(true);
        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                Log.d(TAG, "CURRENT TYPE : "+event.type);

                // Refreshes the volume upon start. If not here, the volume in the beginning
                // can be bugged and set to an incorrect value.
                if (event.type == MediaPlayer.Event.Playing) {
                    new Handler().postDelayed(() -> {
                        videoPlayerViewModel.setVolumeLevel(videoPlayerViewModel.getVolumeLevel().getValue());
                    }, 150);
                }

                // IF NOT BUFFERING AND BEST VIDEO TRACK IS LOADED
//                if (event.type != MediaPlayer.Event.Buffering && isBestStreamLoaded) {
                if (event.type != MediaPlayer.Event.Buffering) {
                    Log.d(TAG, "VLC Ready buffering");
                }

                //IF END OF VIDEO IS REACHED
                if (event.type == MediaPlayer.Event.EndReached) {
                    mediaPlayer.release();
                    routeFinished = true;
                    showFinishScreen();
                }

                //WHILE ROUTE IS PLAYING
                if (!routeFinished) {
                    if (mediaPlayer != null && !selectedMovie.getMovieUrl().toLowerCase().contains("/mpd/")) {
                        //SELECT VIDEO TRACK
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

                        //CHECK IF VIDEOTRACK IS QUALITY OF 720p OR HIGHER
//                        if (mediaPlayer.getCurrentVideoTrack() != null) {
//                            Log.d(TAG, "Height :: " + mediaPlayer.getCurrentVideoTrack().height);
//                            if (mediaPlayer.getCurrentVideoTrack().height >= 720) {
//                                isBestStreamLoaded = true;
//                            }
//                        }
                    }
                }
            }
        });
    }

    public boolean isRouteFinished() {
        return this.routeFinished;
    }

    private void setMediaPlayer() {
        mediaPlayer = new MediaPlayer(VideoLanLib.getLibVLC(getApplicationContext()));
        mediaPlayer.attachViews(videoLayout, null, false, false);

        //This loads the given videoUri to the media
        if (isLocalPlay) {
            //VIDEO
            final Media media = new Media(VideoLanLib.getLibVLC(getApplicationContext()), videoUri);
            mediaPlayer.setMedia(media);
            media.release();
        } else {
            //VIDEO
            final Media media = new Media(VideoLanLib.getLibVLC(getApplicationContext()), Uri.parse(videoUri));
            mediaPlayer.setMedia(media);
            media.release();
        }
        videoLayout.setVisibility(View.INVISIBLE);
        setVideoFeatures();

        mediaPlayer.setRate(1.0f);
        mediaPlayer.play();
    }

    public void updateVideoPlayerScreen(int rpm) {
        if (routeFinished) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //First update the measurements with the latest sensor data
                updateLastCadenceMeasurement(rpm);

                if (mediaPlayer!=null) {
                    Log.d(TAG, "TIME "+mediaPlayer.getTime());
                    videoPlayerViewModel.setMovieSpendDurationSeconds(mediaPlayer.getTime());
                    if (mediaPlayer.getMedia().getDuration() != -1) {
                        Log.d(TAG, "DURATION "+mediaPlayer.getMedia().getDuration());
                        videoPlayerViewModel.setMovieTotalDurationSeconds(mediaPlayer.getMedia().getDuration());
                    }
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
                    if (rpm == 0 && !routePaused && !(communicationType == CommunicationType.NONE)) {
                        togglePauseScreen();
                    } else {
                        //If the route is paused and the average measurement is higher then 0 then unpause en remove pause screen
                        if (routePaused && rpm > 0) {
                            togglePauseScreen();
                        }
                    }
                }
            }
        });
    }

    public void updateVideoPlayerParams(int rpm) {
        if (routeFinished) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //If the route is not paused then pass params to the videoplayer
                if(!routePaused) {
                    /* Update the video player */
                    switch (communicationType) {
                        case RPM:
                            if (mediaPlayer!= null) {
                                mediaPlayer.setRate(RpmVectorLookupTable.getPlaybackspeed(rpm));
                            }
                            break;
                        case ACTIVE:
                            if (mediaPlayer!= null) {
                                mediaPlayer.setRate(1.0f);
                            }
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

    public void togglePauseScreen() {
        //Set new state of videoplayer
        routePaused = !routePaused;
        videoPlayerViewModel.setPlayerPaused(routePaused);

        //LAYOUT CHANGES
        final TextView pauseTitle = findViewById(R.id.status_dialog_title);
        pauseTitle.setText(getString(R.string.pause_screen_title));
        final TextView pauseMessage = findViewById(R.id.status_dialog_message);
        pauseMessage.setText(getString(R.string.pause_screen_message));
        final ImageButton finishFlag = findViewById(R.id.status_dialog_finished_image);
        finishFlag.setVisibility(View.GONE);

        if (routePaused) {
            videoPlayerViewModel.setStatusbarVisible(false);
            backToOverview.requestFocus();
        } else {
            videoPlayerViewModel.setStatusbarVisible(true);
            numberOfFalsePositives = 0;
            this.pauseTimer = 0;
        }

        // IF VLC EXISTS
        if (mediaPlayer!=null) {
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                backgroundSoundPlayer.setDeviceMuted(true);
            } else {
                this.pauseTimer = 0;
                backgroundSoundPlayer.setDeviceMuted(false);
                mediaPlayer.play();
            }
        }

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

        timelineHandler.post(new Runnable() {
            int counter = AUTO_BACK_TO_OVERVIEW_SECONDS;

            @Override
            @SuppressLint("DefaultLocale")
            public void run() {
                if (counter > 0) {
                    String newText = String.format("%s (%d)", getString(R.string.pause_screen_button_text), counter);
                    backToOverview.setText(newText);
                    counter--;
                    timelineHandler.postDelayed(this, 1000);
                } else {
                    runOnUiThread(VideoplayerActivity.this::stopVideoplayer);
                }
            }
        });
    }

    public void goToFrameNumber(int frameNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isSeekable()) {
                    long positionSecond = 0;

                    videoPlayerViewModel.setStatusbarVisible(false);
//                    videoPlayerViewModel.setPlayerPaused(true);
                    playerView.setVisibility(View.GONE);
                    videoLayout.setVisibility(View.GONE);

                    //Loading message
                    TextView loadingMessage = findViewById(R.id.loading_message);
                    loadingMessage.setText(getString(R.string.loading_message));
                    loadingView.setVisibility(View.VISIBLE);

                    //Exoplayer needs to pause before seeking
//                    videoPlayer.pause();

                    //Sounds
                    if (backgroundSoundPlayer !=  null) {
                        backgroundSoundPlayer.pause();
                    }

                    int fps = selectedMovie.getRecordedFps();
                    if (frameNumber > fps) {
                        positionSecond = frameNumber / fps;
//                        mediaPlayer.setPosition(positionSecond*1000);
                        mediaPlayer.setTime(positionSecond*1000);
                    } else {
                        mediaPlayer.setTime(0);
                    }

                    //Set number of false positives to 0 again as the player starts again
                    numberOfFalsePositives = 0;

                    waitUntilVideoIsReady();
                }
            }
        });
    }

    public void startResultScreen() {
        // Build result screen
        final Intent resultScreen = new Intent(getApplicationContext(), ResultActivity.class);
        startActivity(resultScreen);
        //Release player and finish activity
        releasePlayers();
        finish();
    }

    public boolean isActive() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        } else {
            return false;
        }
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
//        pausePlayer();
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
        praxHandler.removeCallbacksAndMessages(null);
        timelineHandler.removeCallbacksAndMessages(null);
        autoRunnerHandler.removeCallbacksAndMessages(null);
        thisInstance = null; // invalidate instance
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
        if (AccountHelper.isChinesportAccount(this)) {
            // Handled in AbstractPraxStatusBarFragment
            return;
        }
        if(statusDialog.getVisibility() == View.GONE) {
            statusDialog.setVisibility(View.VISIBLE);
        } else {
            statusDialog.setVisibility(View.GONE);
        }
    }

//    private boolean discoverChromecasts() {
//        SharedPreferences sharedPreferences = getSharedPreferences("app", MODE_PRIVATE);
//        String apikey = sharedPreferences.getString("apikey", "");
//
//        Log.d(TAG, "CREATE DISCOVERER");
//        for (RendererDiscoverer.Description discoverer : RendererDiscoverer.list(libVLC)) {
//            Log.d(TAG, "DISCOVERER FOUND: " + discoverer.name);
//            RendererDiscoverer scanner = new RendererDiscoverer(libVLC, discoverer.name);
//            scanner.setEventListener(new RendererDiscoverer.EventListener() {
//                @Override
//                public void onEvent(RendererDiscoverer.Event event) {
//                    if (event.type == RendererDiscoverer.Event.ItemAdded) {
//                        Log.d(TAG, "Item found! { " + event.getItem().displayName + "}");
////                        LogHelper.WriteLogRule(getApplicationContext(), apikey, "Renderer found: " + event.getItem().displayName, "DEBUG", "");
//                        rendererItemList.add(event.getItem());
//                    }
//                    if (event.type == RendererDiscoverer.Event.ItemDeleted) {
//                        Log.d(TAG, "Item removed! { " + event.getItem().displayName + "}");
////                        LogHelper.WriteLogRule(getApplicationContext(), apikey, "Renderer removed: " + event.getItem().displayName, "DEBUG", "");
//                        rendererItemList.remove(event.getItem());
//                    }
//                }
//            });
//            rendererDiscovererList.add(scanner);
//        }
//
//        Handler handler = new Handler();
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                for (RendererDiscoverer scanner: rendererDiscovererList) {
//                    scanner.start();
//                }
//            }
//        };
//        handler.postDelayed(runnable, 0);
//        return false;
//    }
//
//    private RendererItem getSpecificRendererItem() {
//        if (rendererItemList.size()>0) {
//            for (final RendererItem item: rendererItemList) {
//                if (item.displayName.equalsIgnoreCase("display")) {
//                    return item;
//                }
//            }
//        }
//        return null;
//    }

    private void playVideo() {
        videoLayout.setVisibility(View.VISIBLE);
        videoPlayerViewModel.setStatusbarVisible(true);
        videoPlayerViewModel.setPlayerPaused(false);
        videoPlayerViewModel.setResetChronometer(true);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int sw = displayMetrics.widthPixels;
        int sh = displayMetrics.heightPixels;

        //FIX FOR IIYAMA ProLite T2452MTS MONITOR
        if (sh == 1008) {
            sh = 1080;
        }

        // sanity check
        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size");
            LogHelper.WriteLogRule(getApplicationContext(), getSharedPreferences("app", MODE_PRIVATE).getString("apikey",""), String.format("[VIDEO] Invalid surface size:(wxh) %d x %d . Movie title: %s .",sw, sh, selectedMovie.getMovieTitle()), "ERROR", "");
            return;
        }

        mediaPlayer.getVLCVout().setWindowSize(sw, sh);
        mediaPlayer.setAspectRatio("16:9");
        mediaPlayer.play();

        SharedPreferences sharedPreferences = getSharedPreferences("app", MODE_PRIVATE);
        String apikey = sharedPreferences.getString("apikey", "");

        long currentSecond = mediaPlayer.getTime() / 1000L;
        if (getCurrentBackgroundSoundByCurrentPosition(currentSecond) != null) {
            runOnUiThread(() -> switchToNewBackgroundMedia(getCurrentBackgroundSoundByCurrentPosition(currentSecond).getSoundUrl()));
        } else {
            if (backgroundSoundTriggers != null && backgroundSoundTriggers.size()>0) {
                Log.d(TAG, "Empty currentBackGround at Start!");
                final String soundUrl = getFirstBackgroundSound().getSoundUrl();
                Log.d(TAG, "Starting first background sound at Start! " + soundUrl);
                runOnUiThread(() -> switchToNewBackgroundMedia(soundUrl));
            }
        }

        playerView.hideController();
    }

    private BackgroundSound getFirstBackgroundSound() {
        BackgroundSound selectedBackgroundSound = null;
        if (backgroundSoundTriggers != null && backgroundSoundTriggers.size()>0) {
            for (final BackgroundSound backgroundSound: backgroundSoundTriggers) {
                if (selectedBackgroundSound == null) {
                    selectedBackgroundSound = backgroundSound;
                }
                if (selectedBackgroundSound!= null && backgroundSound.getFramenumber().intValue() < selectedBackgroundSound.getFramenumber().intValue()) {
                    selectedBackgroundSound = backgroundSound;
                }
            }
        }
        return selectedBackgroundSound;
    }

    private void waitUntilVideoIsReady() {
        this.pauseTimer = 0;

        Runnable runnable = new Runnable() {
            int currentSecond = 0;
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    try {
                        Log.d(TAG, "Mediaplayer is playing: "+mediaPlayer.isPlaying());
                        Log.d(TAG, "Seekable: " + mediaPlayer.isSeekable());
                        Log.d(TAG, "Time available: " + mediaPlayer.getTime());
                        Log.d(TAG, "Length available: " + mediaPlayer.getLength());
                    } catch (Exception exception) {
                        Log.e(TAG, exception.getLocalizedMessage());
                    }

                    if (mediaPlayerShouldBeStarted(currentSecond)) {
                        isLoading = false;
                        playVideo();
                    } else {
                        isLoading = true;

                        currentSecond++;
                        //Maximize waiting for video and/or sensor
                        if (currentSecond > 30) {
                            Toast.makeText(VideoplayerActivity.this, getString(R.string.videoplayer_sensor_wait_error_message), Toast.LENGTH_LONG).show();
                            VideoplayerActivity.this.finish();
                        } else {
                            praxHandler.postDelayed(this, 1000);
                        }
                    }
                }
            }
        };
        praxHandler.post(runnable);
    }

    private boolean mediaPlayerShouldBeStarted(int currentSecond) {
        return currentSecond >= MIN_LOADING_VIEW_SECONDS &&
                isPraxtourMediaPlayerReady() &&
                (sensorConnected || ApplicationSettings.DEVELOPER_MODE || AccountHelper.isChinesportAccount(this));
    }

    private void setTimeLineEventVideoPlayer() {
        Log.d(TAG, "TimeLineEventHandler started!");

        Runnable runnableMovieDetails = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && !routeFinished) {
                    final long currentSecond = mediaPlayer.getTime() / 1000L;

                    if (currentSecond < 2) {
                        mediaPlayer.setVolume(ApplicationSettings.DEFAULT_SOUND_VOLUME);
                    }

                    //check for current backgroundsound
                    backgroundExecutor.execute(() -> {
                        BackgroundSound backgroundSound = getCurrentBackgroundSoundByCurrentPosition(currentSecond);
                        if (backgroundSound != null) {
                            runOnUiThread(() -> {
                                try {
                                    Log.d(TAG, "EventTimeLineHandler: " + backgroundSound.getSoundId().intValue() + " on second: " + currentSecond);
                                    Log.d(TAG, "BgSound with id: " + backgroundSound.getSoundId().intValue() + " state if playing: " + backgroundSoundPlayer.isPlaying());
                                    Log.d(TAG, "BgSound volume: " + backgroundSoundPlayer.getVolume());
                                    Log.d(TAG, "BgSound dev volume: " + backgroundSoundPlayer.getDeviceVolume());
                                    if (!backgroundSoundPlayer.isPlaying() && !routePaused && !routeFinished) {
                                        backgroundSoundPlayer.play();
                                    }
                                } catch (NullPointerException e) {
                                    Log.d(TAG, e.toString());
                                }
                            });
                        }

                        //set to new
                        checkBackgroundSoundMedia(currentSecond);

                        //PAUSE TIMER
                        if (routePaused) {
                            if (pauseTimer > MAX_PAUSE_TIME_SEC || backToOverviewWaitForSensor) {
                                runOnUiThread(() -> stopVideoplayer());
                            } else {
                                pauseTimer++;
                            }
                        }
                    });
                }

                if (routeFinished) {
                    Log.d(TAG, "backToOverviewSensorWait = "+backToOverviewWaitForSensor);
                    if (backToOverviewWaitForSensor) {
                        runOnUiThread(() -> stopVideoplayer());
                    }
                }

                timelineHandler.postDelayed(this, 1000);
            }
        };

        if (!routeFinished) {
            timelineHandler.post(runnableMovieDetails);
        }
    }

    private void stopVideoplayer() {
        if (backgroundSoundPlayer != null && backgroundSoundPlayer.isPlaying()) {
            backgroundSoundPlayer.stop();
        }
        VideoplayerActivity.this.finish();
    }

    private void checkBackgroundSoundMedia(long currentSecond) {
        if (backgroundSoundTriggers.size()>0) {
            for (final BackgroundSound backgroundSound : backgroundSoundTriggers) {
                if ((backgroundSound.getFramenumber()/selectedMovie.getRecordedFps()) == currentSecond) {
                    Log.d(TAG, "Framenumber to switch to soundId: "+backgroundSound.getFramenumber()+", "+backgroundSound.getSoundNumber()+" > "+backgroundSound.getSoundUrl());
                    runOnUiThread(() -> switchToNewBackgroundMedia(backgroundSound.getSoundUrl()));
                }
            }
        }
    }

    private void switchToNewBackgroundMedia(final String backgroundSoundurl) {
        if (backgroundSoundTriggers.size()>0) {
            String localFileName = getSoundFileName(Uri.parse(backgroundSoundurl));

            if (mediaPlayer.getMedia() != null
                    && (backgroundSoundPlayer != null)
            ) {
                Log.d(TAG, "AudioTrack uri bgsPlayer: "+backgroundSoundPlayer.getCurrentMediaItem().playbackProperties.uri.toString());
                Log.d(TAG, "AudioTrack uri should be: "+backgroundSoundurl);
                Log.d(TAG, "AudioTrack localFileName: "+localFileName);
                if (backgroundSoundPlayer.getCurrentMediaItem().playbackProperties.uri.toString().contains(localFileName)) {
                    if (!backgroundSoundPlayer.isPlaying()) {
                        backgroundSoundPlayer.play();
                    }
                    return;
                } else {
                    Uri localItem = DownloadHelper.getLocalSound(getApplicationContext(), Uri.parse(backgroundSoundurl));
                    backgroundSoundPlayer.pause();
                    backgroundSoundPlayer.setMediaItem(MediaItem.fromUri(localItem.toString()));
                    backgroundSoundPlayer.play();
                }
            }
        }
    }

    /**
     * Check which background sound should be playing at the the current position
     * @return BackgroundSound
     */
    private BackgroundSound getCurrentBackgroundSoundByCurrentPosition(long currentSecond) {
        BackgroundSound selectBackgroundSound = null;

        if (backgroundSoundTriggers.size()>0) {
            for (BackgroundSound backgroundSound: backgroundSoundTriggers) {
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

    private String getSoundFileName(final Uri soundItemUri) {
        return soundItemUri.getPath().substring(soundItemUri.getPath().lastIndexOf('/'), soundItemUri.getPath().length());
    }

    private boolean isPraxtourMediaPlayerReady() {
        boolean videoPlayerReady = false;
        boolean backgroundPlayerReady = false;
        boolean soundReady = backgroundSoundTriggers.size()>0;

//        videoPlayerReady = (mediaPlayer.isSeekable() && mediaPlayer.getLength() != -1 && mediaPlayer.getVideoTrack()>2);
        videoPlayerReady = (mediaPlayer.isSeekable() && mediaPlayer.getLength() != -1);
        if (videoPlayerReady) {
            mediaPlayer.pause();
        }

        if (backgroundSoundPlayer.getMediaItemCount()>0) {
            backgroundPlayerReady = (backgroundSoundPlayer.getPlaybackState() == Player.STATE_READY);
        } else {
            backgroundPlayerReady = true;
        }
        Log.d(TAG, "videoPlayerReady:"+videoPlayerReady+", backgroundPlayerReady: "+backgroundPlayerReady+", soundReady: "+soundReady);
        return (videoPlayerReady && (backgroundPlayerReady || soundReady));// || effectSoundPlayerReady || isBestStreamLoaded);
    }

    private void updateLastCadenceMeasurement(final int rpm){
        if (rpm == 0) {
            numberOfFalsePositives++;
            if (numberOfFalsePositives > 3) {
                updateMeasurementList(rpm);
            }
        } else {
            updateMeasurementList(rpm);
        }
    }

    private void updateMeasurementList(final int rpm) {
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

    private void processIncomingData(Intent intent) {
        ArrayList<String> motoLifeData = intent.getStringArrayListExtra("motoLifeData");
        if (motoLifeData == null) {
            Log.d(TAG, "motoLifeData was null, skipping");
            return;
        }

        try {
            rpmMqtt = Integer.parseInt(motoLifeData.get(0));
        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, e.toString());
        }

        Log.d(TAG, "rpmMqtt: " + rpmMqtt);
        runOnUiThread(() -> {
            updateVideoPlayerParams(rpmMqtt);
            updateVideoPlayerScreen(rpmMqtt);
        });
    }

    private void setUp() {
        //START INIT SENSORS
        if (!selectedProduct.getCommunicationType().toLowerCase().equals("none") && !AccountHelper.isChinesportAccount(this)) {
            startSensorService();
        }

        //START INIT VIDEO
//        initializeVideoPlayer();
//        if (videoUri == null) {
//            return;
//        }
//        initializeVlcVideoPlayer();
        //PREPARE SOURCE FOR PLAY
//        prepareVideoMediaSource(Uri.parse(videoUri));

        //START INIT BACKGROUND SOUND
        initializeBackgroundSoundPlayer();
        Log.d(TAG, "bg items loaded for preparation: "+backgroundSoundTriggers.size());
        prepareBackgroundSoundPlayer();

        //START DATA RECEIVERS
        if (communicationType != CommunicationType.NONE && !AccountHelper.isChinesportAccount(this)) {
            startSensorDataReceiver();
        }

    }

    private void startSensorService() {
        if (!ApplicationSettings.DEVELOPER_MODE) {
            switch (communicationDevice) {
                case ANT_PLUS:
                    //DEPRECATED:: Start AntPlus service to connect with cadence sensor

                case BLE:
                    //Start BLE to connect with sensor device
                    final Intent bleService = new Intent(getApplicationContext(), BleService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(bleService);
                    } else {
                        startService(bleService);
                    }
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

    private void initializeVlcVideoPlayer() {
        if (mediaPlayer == null) {
            setMediaPlayer();
        } else {
            Log.d(TAG, "Mediaplayer already exists with aspect ratio: "+mediaPlayer.getAspectRatio());
        }
    }

    private void initializeVideoPlayer() {
        if (videoPlayer == null) {
            // Create the player
//            final MediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);

//            Trackselector to retrieve and start the best track in mp4
//            DefaultTrackSelector trackSelector = new DefaultTrackSelector(getApplicationContext());
//            trackSelector.setParameters(
//                trackSelector.buildUponParameters()
//                        .setForceHighestSupportedBitrate(true)
//                        .build()
//            );

//            DefaultLoadControl defaultLoadControl = new DefaultLoadControl.Builder()
//                    .setPrioritizeTimeOverSizeThresholds(true)//default = false
//                    .setBufferDurationsMs(50000,50000, 20000,20000)
//                    .build();

            videoPlayer = new ExoPlayer.Builder(this)
//                    .setLoadControl(defaultLoadControl)
                    //.setTrackSelector(trackSelector) Not using the trackselector
//                    .setMediaSourceFactory(mediaSourceFactory)
                    .build();

            // Set speed of the video (hence buffering /streaming speed )
            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
            videoPlayer.setPlaybackParameters(playbackParameters);

            //Set hardware sound to maximum
            backgroundSoundPlayer.setDeviceVolume(backgroundSoundPlayer.getDeviceInfo().maxVolume);

            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            //Set player on playerview
            playerView.setPlayer(videoPlayer);
        }
    }

    private void initializeBackgroundSoundPlayer() {
        if (backgroundSoundPlayer == null) {
            backgroundSoundPlayer = new ExoPlayer.Builder(this).build();
            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
            backgroundSoundPlayer.setPlaybackParameters(playbackParameters);
            backgroundSoundPlayer.setDeviceVolume(backgroundSoundPlayer.getDeviceInfo().maxVolume);
            playerView.setPlayer(backgroundSoundPlayer);
        }
    }

//    private void prepareVideoMediaSource(Uri mUri) {
//        final MediaItem mediaItem = MediaItem.fromUri(mUri);
//        videoPlayer.setMediaItem(mediaItem);
//        videoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
//        Log.d(TAG,"Player preparing!");
//        videoPlayer.prepare();
//
//        videoPlayer.addListener(new VideoPlayerEventListener());
//        videoPlayer.addListener(new Player.EventListener()
//        {
//            @Override
//            public void onPlaybackStateChanged(int state) {
//                if (state == ExoPlayer.STATE_READY) {
//                    Log.d(TAG,"Player ready to start playing!");
//                }
//            }
//        });
//    }

    private void prepareBackgroundSoundPlayer() {
        if (movieId != 0) {
            videoPlayerViewModel.getBackgroundSounds(movieId).observe(this, backgroundSounds -> {
                if (backgroundSounds != null && backgroundSounds.size() > 0 && backgroundSoundTriggers.size() == 0) {
                    //Split into 2 paths
                    // 1. backgroundTriggers
                    // 2. backgroundMediaItems
                    backgroundSoundTriggers = backgroundSounds;
                    Log.d(TAG, "bg items loaded for preparation in observer: "+backgroundSoundTriggers.size());

                    isSoundOnDevice = DownloadHelper.isSoundPresent(getApplicationContext());

                    if (backgroundSounds.size() > 0) {
                        for (final BackgroundSound backgroundSound : backgroundSounds) {
                            if (!isBackgroundSoundAlreadyExistingInList(backgroundSound)) {
                                Uri uri = Uri.parse(backgroundSound.getSoundUrl());
                                if (isSoundOnDevice) {
                                    uri = DownloadHelper.getLocalSound(getApplicationContext(), uri);
                                }

                                MediaItem mediaItem = MediaItem.fromUri(uri);
                                backgroundMediaItems.add(mediaItem);
                            }
                        }
                    }
                    prepareBackgroundSoundMediaSources();
                }
            });
        }
    }

    private boolean isBackgroundSoundAlreadyExistingInList(BackgroundSound backgroundSound) {
        MediaItem mediaItem =  null;
        Uri uri = Uri.parse(backgroundSound.getSoundUrl());
        if (isSoundOnDevice) {
            uri = DownloadHelper.getLocalSound(getApplicationContext(), uri);
        }
        mediaItem = MediaItem.fromUri(uri);

        for (final MediaItem bgSound: backgroundMediaItems) {
            if (    getSoundFileName(bgSound.playbackProperties.uri).toLowerCase()
                    .equals(
                        getSoundFileName(Uri.parse(backgroundSound.getSoundUrl().toLowerCase()))
                    )
            ) {
               return true;
            }
        }
        return false;
    }

    private void prepareBackgroundSoundMediaSources() {
        if (backgroundMediaItems.size()>0) {
            for (MediaItem bgMediaItem: backgroundMediaItems) {
                backgroundSoundPlayer.addMediaItem(bgMediaItem);
            }
            backgroundSoundPlayer.prepare();

            Log.d(TAG, "Add (AUDIO) to SOUND VLC Player: "+backgroundMediaItems.get(0).playbackProperties.uri.toString());
//            mediaPlayer.addSlave(IMedia.Slave.Type.Audio, backgroundMediaItems.get(0).playbackProperties.uri, false );

        }
//        if (backgroundSounds.size()>0) {
//            for (Uri uri: backgroundSounds) {
//                if (isSoundOnDevice) {
//                    uri = DownloadHelper.getLocalSound(getApplicationContext(), uri);
//                }
//
//                MediaItem mediaItem = MediaItem.fromUri(uri);
//                backgroundSoundPlayer.addMediaItem(mediaItem);
//            }
//            Log.d(TAG, "preparemethod: bg items loaded for preparation: "+backgroundSoundList.size());
//            backgroundSoundPlayer.prepare();
//        }
    }

    private void releasePlayers() {
        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }
        if (mediaPlayer!=null) {
            mediaPlayer.release();
            mediaPlayer = null;
//            libVLC.release();
//            libVLC = null;
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
        if (mediaPlayer!=null) {
            mediaPlayer.pause();
        }
    }

    private void resumePlayer() {
        if (videoPlayer != null) {
            videoPlayer.setPlayWhenReady(true);
            videoPlayer.getPlaybackState();
        }
        if (mediaPlayer!=null) {
            mediaPlayer.play();
        }
    }

    private void stopSensorService() {
        switch(communicationDevice) {
            case ANT_PLUS:
                Log.d(TAG, "DEPRECATED");
                break;
            case BLE:
//                final Intent bleService = new Intent(getApplicationContext(), BleService.class);
//                stopService(bleService);
//                if (bleWrapper != null) {
//                    bleWrapper.disconnect();
//                    bleWrapper = null;
//                }
                break;
            default:
        }
    }

    private boolean hasSound() {
        boolean backgroundPlayer = backgroundSoundPlayer != null;
        boolean backgroundItemLoaded = true;
        boolean videoVolume = true;
        if (backgroundSoundTriggers.size()>0) {
            backgroundPlayer = backgroundSoundPlayer != null && backgroundSoundPlayer.isPlaying();
            backgroundItemLoaded = backgroundSoundPlayer.getCurrentMediaItem() != null && !backgroundSoundPlayer.getCurrentMediaItem().playbackProperties.uri.toString().isEmpty();
            videoVolume = true;
        }
        boolean backgroundSoundPlayerVolume = backgroundSoundPlayer.getVolume()>0;
        boolean backgroundSoundDeviceMuted = backgroundSoundPlayer.isDeviceMuted();

        boolean video = mediaPlayer!=null && mediaPlayer.isPlaying();

        boolean systemSoundMode = SoundHelper.hasSystemSound(getApplicationContext());

        return backgroundPlayer && backgroundSoundPlayerVolume
                && !backgroundSoundDeviceMuted && backgroundItemLoaded
                && video && videoVolume
                && systemSoundMode;
    }

}