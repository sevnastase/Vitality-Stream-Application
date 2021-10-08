package com.videostreamtest.ui.phone.videoplayer;

import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
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
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.service.ble.BleWrapper;
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

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

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

    private static final int MAX_PAUSE_TIME_SEC = 55;
    private VideoPlayerViewModel videoPlayerViewModel;

    private static VideoplayerActivity thisInstance;

    private CastContext castContext;

    private PlayerView playerView;
    private SimpleExoPlayer videoPlayer;
    private SimpleExoPlayer backgroundSoundPlayer;
//    private SimpleExoPlayer effectSoundPlayer;

    //VLC
    private VLCVideoLayout videoLayout;
    private LibVLC libVLC;
    private boolean vlcLoaded = false;
    private boolean isBestStreamLoaded = false;

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
    private List<BackgroundSound> backgroundSoundTriggers = new ArrayList<>();
    private List<MediaItem> backgroundMediaItems = new ArrayList<>();

    private List<EffectSound> effectSoundList = new ArrayList<>();

    private boolean isSoundOnDevice = false;
    private boolean isLocalPlay = false;

    private LinearLayout statusDialog;
    private RelativeLayout loadingView;

    private Button backToOverview;

    private int minSecondsLoadingView = 7;
    private boolean isLoading = true;
    private boolean sensorConnected = false;

    private int[] lastRpmMeasurements = new int[5];
    private int currentMeasurementIteration = 0;
    private int numberOfFalsePositives = 0;

    private MediaPlayer mediaPlayer;
    private MediaPlayer soundPlayer;

    private boolean routePaused = false;
    private int pauseTimer = 0;
    private boolean routeFinished = false;

    //BLE
    private BleWrapper bleWrapper;
    private boolean backToOverviewWaitForSensor = false;

    //VLC stuff
    private LibVLC createLibVLC() {
        final List<String> args = new ArrayList<>();
        args.add("-vvv");
        args.add("--sout-all");
        args.add("--aout=opensles");
//      args.add("--drop-late-frames");
        //LOCAL PLAY
        args.add("--file-caching=45000");
        args.add("--no-avcodec-hurry-up");//ATTEMPT TO SOLVE GREY SCREEN PROBLEM
        //STREAMING
        args.add("--network-caching=20000");

        LibVLC libVLC = new LibVLC(this, args);
        return libVLC;
    }

    private void setVideoFeatures() {
        mediaPlayer.setVideoTrackEnabled(true);

        int sw = getWindow().getDecorView().getWidth();
        int sh = getWindow().getDecorView().getHeight();

        // sanity check
        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        mediaPlayer.getVLCVout().setWindowSize(sw, sh);
        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                Log.d(TAG, "CURRENT TYPE : "+event.type);

                // IF NOT BUFFERING AND BEST VIDEO TRACK IS LOADED
                if (event.type != MediaPlayer.Event.Buffering && isBestStreamLoaded) {
                    vlcLoaded = true;
                }

                //IF END OF VIDEO IS REACHED
                if (event.type == MediaPlayer.Event.EndReached) {
                    mediaPlayer.release();
                    soundPlayer.pause();
                    soundPlayer.release();
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
//                                Log.d(TAG, "Name:" + trackDescription.name + " :: id:" + trackDescription.id);
                            }
                            if (id > 0 && mediaPlayer.getVideoTrack() != id) {
                                mediaPlayer.setVideoTrack(id);
                            }
                        }

                        //CHECK IF VIDEOTRACK IS QUALITY OF 720p OR HIGHER
                        if (mediaPlayer.getCurrentVideoTrack() != null) {
                            Log.d(TAG, "Height :: " + mediaPlayer.getCurrentVideoTrack().height);
                            if (mediaPlayer.getCurrentVideoTrack().height >= 720) {
                                isBestStreamLoaded = true;
                            }
                        }
                    }
                }
            }
        });
    }

    private void setMediaPlayer() {
        if (libVLC != null) {
            mediaPlayer = new MediaPlayer(libVLC);
            soundPlayer = new MediaPlayer(libVLC);
        } else {
            libVLC = createLibVLC();
            mediaPlayer = new MediaPlayer(libVLC);
            soundPlayer = new MediaPlayer(libVLC);
        }

        mediaPlayer.attachViews(videoLayout, null, false, false);

        //This loads the given videoUri to the media
        if (mediaPlayer.getMedia() == null) {
            if (isLocalPlay) {
                //VIDEO
                final Media media = new Media(libVLC, videoUri);
                //Streaming
//                media.setHWDecoderEnabled(true, false);
//                media.addOption(":clock-jitter=0");
//                media.addOption(":clock-synchro=0");
                //end
                mediaPlayer.setMedia(media);
                media.release();
            } else {
                //VIDEO
                final Media media = new Media(libVLC, Uri.parse(videoUri));
                mediaPlayer.setMedia(media);
                media.release();
            }
        }
        setVideoFeatures();

        mediaPlayer.setRate(1.0f);
        mediaPlayer.setAspectRatio("16:9");
        mediaPlayer.play();
        soundPlayer.play();
    }

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
        playerView = findViewById(R.id.playerView);
        playerView.setUseController(false);

        // VLC PLayer
        videoLayout = findViewById(R.id.vlc_player_view);

        statusDialog = findViewById(R.id.status_dialog_videoplayer);
        loadingView = findViewById(R.id.loading_view);

        isSoundOnDevice = DownloadHelper.isSoundPresent(getApplicationContext());

        libVLC = createLibVLC();

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

                //Pass movie details with a second based timer TODO:move to setTimeLineEvent method in this class
                Handler praxSpinHandler = new Handler();
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
                        if (mediaPlayer!= null) {
                            mediaPlayer.setRate(RpmVectorLookupTable.getPlayBackSpeedFromKmh(kmhData));
                        }
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
            if (mediaPlayer!=null) {
                final Float bigVolumeLevel = volumeLevel * 100;
                Log.d(TAG, "Volume Level: "+bigVolumeLevel.intValue());
                mediaPlayer.setVolume(bigVolumeLevel.intValue());
                soundPlayer.setVolume(bigVolumeLevel.intValue());
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
                if (bleWrapper != null) {
                    bleWrapper.disconnect();
                    backToOverviewWaitForSensor = true;

                    backToOverview.setClickable(false);
                    backToOverview.setText(R.string.video_player_wait_sensor_disconnect);
                    //HANDLED FURTHER in #setTimeLineEventVideoPlayer()
                }

            }
        });

        // START TEST CODE PAUSE SCREEN
//        Runnable showPauseScreen = new Runnable() {
//            public void run() {
//                updateVideoPlayerScreen(0);
//                updateVideoPlayerParams(0);
//                updateVideoPlayerScreen(0);
//                updateVideoPlayerParams(0);
//                updateVideoPlayerScreen(0);
//                updateVideoPlayerParams(0);
//                updateVideoPlayerScreen(0);
//                updateVideoPlayerParams(0);
//                updateVideoPlayerScreen(0);
//
//                updateVideoPlayerParams(0);
//                togglePauseScreen();
//
//                Runnable hidePauseScreen = new Runnable() {
//                    public void run() {
//                        updateVideoPlayerScreen(60);
//                        updateVideoPlayerScreen(60);
//                        updateVideoPlayerScreen(60);
//
//                        updateVideoPlayerParams(60);
//                        togglePauseScreen();
//                    }
//                };
//                new Handler(Looper.getMainLooper()).postDelayed( hidePauseScreen, 8000 );
//            }
//        };
//        new Handler(Looper.getMainLooper()).postDelayed( showPauseScreen, 18000 );
        // END TEST CODE
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        waitUntilVideoIsReady();
        setTimeLineEventVideoPlayer();

        if (ApplicationSettings.DEVELOPER_MODE) {
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (videoPlayer != null) {
                        //progressbar.setProgress((int) ((exoPlayer.getCurrentPosition()*100)/exoPlayer.getDuration()));
                        videoPlayerViewModel.setRpmData(60);//new Random().nextInt(80));

                        if (mediaPlayer!=null) {
                            Log.d(TAG, "TIME " + mediaPlayer.getTime());
                            videoPlayerViewModel.setMovieSpendDurationSeconds(mediaPlayer.getTime());
                            if (mediaPlayer.getMedia().getDuration() != -1) {
                                Log.d(TAG, "DURATION " + mediaPlayer.getMedia().getDuration());
                                videoPlayerViewModel.setMovieTotalDurationSeconds(mediaPlayer.getMedia().getDuration());
                            }
                        }

//                        videoPlayerViewModel.setMovieSpendDurationSeconds(videoPlayer.getCurrentPosition());
//                        videoPlayerViewModel.setMovieTotalDurationSeconds(videoPlayer.getDuration());

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
        if (routeFinished) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //First update the measurements with the latest sensor data
                updateLastCadenceMeasurement(rpm);

                if (videoPlayer != null && videoPlayer.isPlaying()) {
                    videoPlayerViewModel.setMovieSpendDurationSeconds(videoPlayer.getCurrentPosition());
                    videoPlayerViewModel.setMovieTotalDurationSeconds(videoPlayer.getDuration());
                }

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
                    if (getAverageCadenceMeasurements() == 0 && !routePaused && !(communicationType == CommunicationType.NONE)) {
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
                            //Set the speed of the player based on our cadence rpm reading
                            PlaybackParameters playbackRpmParameters = new PlaybackParameters(RpmVectorLookupTable.getPlaybackspeed(rpm), PlaybackParameters.DEFAULT.pitch);
                            if (videoPlayer != null) {
                                videoPlayer.setPlaybackParameters(playbackRpmParameters);
                            }
                            if (mediaPlayer!= null) {
                                mediaPlayer.setRate(RpmVectorLookupTable.getPlaybackspeed(rpm));
                            }
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
        //Videoplayer but doesnt used (OLD)
        videoPlayer.setPlayWhenReady(!videoPlayer.getPlayWhenReady());
        videoPlayer.getPlaybackState();

        // IF VLC EXISTS
        if (mediaPlayer!=null) {
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                soundPlayer.pause();
                backgroundSoundPlayer.setDeviceMuted(true);
            } else {
                this.pauseTimer = 0;
                backgroundSoundPlayer.setDeviceMuted(false);
                mediaPlayer.play();
                soundPlayer.play();
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
    }

    public void goToFrameNumber(int frameNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isSeekable()) {
                    long positionSecond = 0;

                    videoPlayerViewModel.setStatusbarVisible(false);
                    videoPlayerViewModel.setPlayerPaused(true);
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

                    waitUntilVideoIsReady(3);
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
        videoLayout.setVisibility(View.VISIBLE);
//        playerView.setVisibility(View.VISIBLE);
//        loadingView.setVisibility(View.GONE);
        videoPlayerViewModel.setStatusbarVisible(true);
//        videoPlayer.play();

        if (mediaPlayer ==null) {
            setMediaPlayer();
        } else {
            setVideoFeatures();
            mediaPlayer.play();
            soundPlayer.play();
        }

        if (getCurrentBackgroundSoundByCurrentPostion() != null) {
            switchToNewBackgroundMedia(getCurrentBackgroundSoundByCurrentPostion().getSoundUrl());
        } else {
            Log.d(TAG, "Empty currentBackGround at Start!");
        }

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
        this.pauseTimer = 0;
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            int currentSecond = 0;
            @Override
            public void run() {
                if (videoPlayer != null) {
                    if (mediaPlayer!=null) {
                        Log.d(TAG, "Seekable: "+mediaPlayer.isSeekable());
                        Log.d(TAG, "Time available: "+mediaPlayer.getTime());
                        Log.d(TAG, "Length available: "+mediaPlayer.getLength());
                    }
                    if (    (currentSecond >= minSecondsLoadingView) &&
                            isPraxtourMediaPlayerReady() &&
                            (sensorConnected || ApplicationSettings.DEVELOPER_MODE)
                    ) {
                        isLoading = false;
                        isBestStreamLoaded = false;
                        playVideo();
                    } else {
                        isLoading = true;

                        currentSecond++;
                        //Maximize waiting for video and/or sensor
                        if (currentSecond > 30) {
                            Toast.makeText(VideoplayerActivity.this, getString(R.string.videoplayer_sensor_wait_error_message), Toast.LENGTH_LONG).show();
                            VideoplayerActivity.this.finish();
                        } else {
                            handler.postDelayed(this::run, 1000);
                        }
                    }
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private void setTimeLineEventVideoPlayer() {
        Log.d(TAG, "TimeLineEventHandler started!");

        HandlerThread thread = new HandlerThread("TimeLineEventHandlerStart",
                Process.THREAD_PRIORITY_URGENT_DISPLAY);
        thread.start();

        Handler timelineHandler = new Handler(thread.getLooper());
        Runnable runnableMovieDetails = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer!=null) {
                    //Is media seekable
                    Log.d(TAG, "Seekable: "+mediaPlayer.isSeekable());
                    //Current position in ms
                    Log.d(TAG, "Time available: "+mediaPlayer.getTime());
                    //Total length of video in ms
                    Log.d(TAG, "Length available: "+mediaPlayer.getLength());
                }

                if (mediaPlayer != null && !routeFinished) {

                    if (mediaPlayer.getTime()/1000L < 2) {
                        mediaPlayer.setVolume(ApplicationSettings.DEFAULT_SOUND_VOLUME);
                        soundPlayer.setVolume(ApplicationSettings.DEFAULT_SOUND_VOLUME);
                    }

                    //check for current backgroundsound
                    BackgroundSound backgroundSound = getCurrentBackgroundSoundByCurrentPostion();
                    if ( backgroundSound != null) {
                        Log.d(TAG, "EventTimeLineHandler: "+backgroundSound.getSoundId() + " on second: "+mediaPlayer.getTime()/1000L);
                        Log.d(TAG, "BgSound with id: "+backgroundSound.getSoundId()+" state if playing: "+soundPlayer.isPlaying());
//                        switchToNewBackgroundMedia(getCurrentBackgroundSoundByCurrentPostion().getSoundUrl());
                    }

                    //set to new
                    checkBackgroundSoundMedia();

//                    checkEffectSoundMedia();
                    timelineHandler.postDelayed(this::run, 1000);
                }
                //Asssert of videoplayer remains as it is the main priority
                if (videoPlayer != null) {

                }

                //PAUSE TIMER
                if (routePaused) {
                    if (pauseTimer > MAX_PAUSE_TIME_SEC && !backToOverviewWaitForSensor) {
                        VideoplayerActivity.this.finish();
                    } else {
                        pauseTimer++;
                    }

                    Log.d(TAG, "backToOverviewSensorWait = "+backToOverviewWaitForSensor);

                    if (backToOverviewWaitForSensor) {
                        bleWrapper = null;
                        VideoplayerActivity.this.finish();
                    }


//                    if (pauseTimer % 10 == 0) {
//                        startSensorService();
//                    }
                }
            }
        };

        if (!routeFinished) {
            timelineHandler.postDelayed(runnableMovieDetails, 0);
        }
    }

    private void checkBackgroundSoundMedia() {
        if (backgroundSoundTriggers.size()>0) {
            long currentSecond = (mediaPlayer.getTime() / 1000L);
            for (final BackgroundSound backgroundSound : backgroundSoundTriggers) {
                if ((backgroundSound.getFramenumber()/selectedMovie.getRecordedFps()) == currentSecond) {
                    switchToNewBackgroundMedia(backgroundSound.getSoundUrl());
                }
            }
        }
    }

    private void switchToNewBackgroundMedia(final String backgroundSoundurl) {
        if (backgroundSoundTriggers.size()>0) {
            String localFileName = getSoundFileName(Uri.parse(backgroundSoundurl));

            if (mediaPlayer.getMedia() != null
                    && soundPlayer.getMedia() != null
            ) {
                Log.d(TAG, "AudioTrack uri: "+soundPlayer.getMedia().getUri().toString());
                Log.d(TAG, "AudioTrack uri should be: "+backgroundSoundurl);
                if (soundPlayer.getMedia().getUri().toString().contains(localFileName)) {
                    return;
                } else {
                    Uri localItem = DownloadHelper.getLocalSound(getApplicationContext(), Uri.parse(backgroundSoundurl));
                    soundPlayer.pause();
//                    Toast.makeText(this, localItem.toString() + " exists: "+new File(localItem.toString()).exists(), Toast.LENGTH_LONG).show();
                    Media localBgSoundMedia = new Media(libVLC, localItem.toString());
                    soundPlayer.setMedia(localBgSoundMedia);
                    soundPlayer.play();
                    localBgSoundMedia.release();
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
        long currentSecond = (mediaPlayer.getTime() / 1000L);
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
//        boolean effectSoundPlayerReady = false;

        videoPlayerReady = (mediaPlayer.isSeekable() && mediaPlayer.getLength() != -1);

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

        return (videoPlayerReady || backgroundPlayerReady || isBestStreamLoaded);// || effectSoundPlayerReady);
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

    private void setUp() {
        //START INIT SENSORS
        startSensorService();

        //START INIT VIDEO
        initializeVideoPlayer();
        initializeVlcVideoPlayer();
        if (videoUri == null) {
            return;
        }
        //PREPARE SOURCE FOR PLAY
        prepareVideoMediaSource(Uri.parse(videoUri));

        //START INIT BACKGROUND SOUND
        initializeBackgroundSoundPlayer();
        Log.d(TAG, "bg items loaded for preparation: "+backgroundSoundTriggers.size());
        prepareBackgroundSoundPlayer();

        //START INIT EFFECT SOUND
//        initializeEffectSoundPlayer();
//        prepareEffectSoundPlayer();

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
                    //Start BLE to connect with sensor device
                    bleWrapper = new BleWrapper();
                    bleWrapper.initBle(this);
                    bleWrapper.connectDefaultBleDevice();
                default:
                    //NONE
            }
        }
    }

    private void initializeVlcVideoPlayer() {
        if (mediaPlayer == null) {
            setMediaPlayer();
        }
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
                    .setPrioritizeTimeOverSizeThresholds(true)//default = false
//                    .setBufferDurationsMs(50000,50000, 20000,20000)
                    .build();

            videoPlayer = new SimpleExoPlayer.Builder(this)
//                    .setLoadControl(defaultLoadControl)
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
            backgroundSoundPlayer = new SimpleExoPlayer.Builder(this)
                    .setLoadControl(defaultLoadControl)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build();

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
                if (backgroundSounds != null && backgroundSounds.size() > 0 && backgroundSoundTriggers.size() == 0) {
                    //Split into 2 paths
                    // 1. backgroundTriggers
                    // 2. backgroundMediaItems
                    backgroundSoundTriggers = backgroundSounds;

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

                    //Old
//                    backgroundSoundList = backgroundSounds;
//                    Log.d(TAG, "method: bg items loaded for preparation: " + backgroundSoundList.size());
//                    List<Uri> backgroundSoundUriList = new ArrayList<>();
//                    if (backgroundSounds.size() > 0) {
//                        for (final BackgroundSound backgroundSound : backgroundSounds) {
//                            backgroundSoundUriList.add(Uri.parse(backgroundSound.getSoundUrl()));
//                        }
//                    }

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
            Media bgSound = new Media(libVLC, backgroundMediaItems.get(0).playbackProperties.uri.toString());
            soundPlayer.setMedia(bgSound);
            bgSound.release();
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
        if (mediaPlayer!=null) {
            mediaPlayer.release();
            mediaPlayer = null;
            soundPlayer.release();
            soundPlayer = null;
            libVLC.release();
            libVLC = null;
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
                final Intent antplusService = new Intent(getApplicationContext(), AntPlusService.class);
                stopService(antplusService);
                break;
            case BLE:
//                final Intent bleService = new Intent(getApplicationContext(), BleService.class);
//                stopService(bleService);
                if (bleWrapper != null) {
                    bleWrapper.disconnect();
                    bleWrapper = null;
                }
                break;
            default:
        }
    }

}