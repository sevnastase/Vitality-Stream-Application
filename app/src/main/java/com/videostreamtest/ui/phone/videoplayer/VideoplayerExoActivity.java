package com.videostreamtest.ui.phone.videoplayer;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
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
import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.enums.CommunicationType;
import com.videostreamtest.receiver.CadenceSensorBroadcastReceiver;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.helpers.ProductHelper;
import com.videostreamtest.ui.phone.helpers.SoundHelper;
import com.videostreamtest.ui.phone.result.ResultActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxFilmStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxFitStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.PraxSpinStatusBarFragment;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.RpmVectorLookupTable;
import com.videostreamtest.utils.VideoLanLib;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen videoplayer activity
 */
public class VideoplayerExoActivity extends AppCompatActivity {
    private static final String TAG = VideoplayerExoActivity.class.getSimpleName();

    private static final int MAX_PAUSE_TIME_SEC = 55;
    private VideoPlayerViewModel videoPlayerViewModel;

    private static VideoplayerExoActivity thisInstance;

    private CastContext castContext;

    private PlayerView bgSoundPlayerView;

    private ExoPlayer backgroundSoundPlayer;//TODO: Test if can be replaced by VLC MediaPlayer
    private CadenceSensorBroadcastReceiver cadenceSensorBroadcastReceiver;

    //EXOPlayer
    private PlayerView videoLayout;
    private ExoPlayer mediaPlayer;

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

    private int minSecondsLoadingView = 7;
    private boolean isLoading = true;
    private boolean sensorConnected = false;

    private int[] lastRpmMeasurements = new int[5];
    private int currentMeasurementIteration = 0;
    private int numberOfFalsePositives = 0;

    private boolean routePaused = false;
    private int pauseTimer = 0;
    private boolean routeFinished = false;

    //BLE
    private boolean backToOverviewWaitForSensor = false;

    //CHROMECAST
//    List<RendererDiscoverer> rendererDiscovererList = new ArrayList<>();
//    List<RendererItem> rendererItemList = new ArrayList<>();
    private BroadcastReceiver finishFilmReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisInstance = this;
        setContentView(R.layout.activity_exo_videoplayer);
        videoPlayerViewModel = new ViewModelProvider(this).get(VideoPlayerViewModel.class);

        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        //EXO SOUND player
        bgSoundPlayerView = findViewById(R.id.background_player_view);
        bgSoundPlayerView.setUseController(false);

        // EXO VIDEO PLayer
        videoLayout = findViewById(R.id.google_exoplayer_view);
        videoLayout.setUseController(false);

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
//                Handler praxFilmHandler = new Handler();
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
//                            praxFilmHandler.postDelayed(this::run, 1000);
//                        }
//                    }
//                };
//                praxFilmHandler.postDelayed(runnableMovieDetails, 0);

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
                            Log.d(TAG, "TIME >> "+mediaPlayer.getDuration());
                            Log.d(TAG, "CONTENT TIME >> "+mediaPlayer.getContentDuration());
                            Log.d(TAG, "CURRENT POSITION >> "+mediaPlayer.getCurrentPosition());
                            Log.d(TAG, "CONTENT POSITION >> "+mediaPlayer.getContentPosition());
                            videoPlayerViewModel.setMovieSpendDurationSeconds(mediaPlayer.getCurrentPosition());
                            if (mediaPlayer.getCurrentMediaItem()!= null && mediaPlayer.getCurrentPosition() != -1) {
                                Log.d(TAG, "DURATION "+mediaPlayer.getDuration());
                                videoPlayerViewModel.setMovieTotalDurationSeconds(mediaPlayer.getDuration());
                            }

                        }
                        if (!routeFinished) {
                            praxSpinHandler.postDelayed(this::run, 1000);
                        }
                    }
                };
                praxSpinHandler.postDelayed(runnableMovieDetails, 0);

                videoPlayerViewModel.getKmhData().observe(this, kmhData ->{
                    if (kmhData != null && mediaPlayer != null) {
                        // Set speed of the video (hence buffering /streaming speed )
                        if (mediaPlayer!= null) {
                            mediaPlayer.setPlaybackSpeed(RpmVectorLookupTable.getPlayBackSpeedFromKmh(kmhData));
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

        videoPlayerViewModel.getVolumeLevel().observe(this, volumeLevel -> {
            if (mediaPlayer!=null && volumeLevel != null) {
                final Float bgVolumeLevel = Float.valueOf(""+volumeLevel) / 100;
                mediaPlayer.setVolume(bgVolumeLevel);
                if (backgroundSoundTriggers!= null && backgroundSoundTriggers.size()>0) {
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

        finishFilmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (videoPlayerViewModel != null) {
                    videoPlayerViewModel.setVolumeLevel(0);
                }
                showFinishScreen();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(finishFilmReceiver, new IntentFilter("videoplayer_finish_film"));
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
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null) {
                        //progressbar.setProgress((int) ((exoPlayer.getCurrentPosition()*100)/exoPlayer.getDuration()));
                        videoPlayerViewModel.setRpmData(60);//new Random().nextInt(80));

                        if (mediaPlayer!=null) {
                            Log.d(TAG, "TIME " + mediaPlayer.getDuration());
                            videoPlayerViewModel.setMovieSpendDurationSeconds(mediaPlayer.getDuration());
                            if (mediaPlayer.getDuration() != -1) {
                                Log.d(TAG, "DURATION " + mediaPlayer.getDuration());
                                videoPlayerViewModel.setMovieTotalDurationSeconds(mediaPlayer.getDuration());
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

        int preferredDefaultVolume = getSharedPreferences("app", Context.MODE_PRIVATE).getInt("defaultVolume", 50);
        videoPlayerViewModel.setVolumeLevel(preferredDefaultVolume);

        // Workaround: if this is not here, the preferred sound will not take effect
        // the status bar (bottom) will show the correct volume, however the actual
        // volume is different FIXME
        new Handler().postDelayed(() -> {
            if (videoPlayerViewModel.getVolumeLevel().getValue() != null &&
                    videoPlayerViewModel.getVolumeLevel().getValue() <= 90) {
                videoPlayerViewModel.changeVolumeLevelBy(10);
                videoPlayerViewModel.changeVolumeLevelBy(-10);
            }
            if (videoPlayerViewModel.getVolumeLevel().getValue() != null &&
                    videoPlayerViewModel.getVolumeLevel().getValue() >= 10) {
                videoPlayerViewModel.changeVolumeLevelBy(-10);
                videoPlayerViewModel.changeVolumeLevelBy(10);
            }
        }, 200);
    }

    public static VideoplayerExoActivity getInstance() {
        return thisInstance;
    }

    //VLC stuff
    private void setVideoFeatures() {
        mediaPlayer.addListener(new Player.Listener(){
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_ENDED) {
                        mediaPlayer.release();
                        routeFinished = true;
                        showFinishScreen();
                    }
                }

                @Override
                public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {

                }
            });

//        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
//            @Override
//            public void onEvent(MediaPlayer.Event event) {
//                Log.d(TAG, "CURRENT TYPE : "+event.type);
//
//                // IF NOT BUFFERING AND BEST VIDEO TRACK IS LOADED
////                if (event.type != MediaPlayer.Event.Buffering && isBestStreamLoaded) {
//                if (event.type != MediaPlayer.Event.Buffering) {
//                    Log.d(TAG, "VLC Ready buffering");
//                }
//
//                //IF END OF VIDEO IS REACHED
//                if (event.type == MediaPlayer.Event.EndReached) {
//                    mediaPlayer.release();
//                    routeFinished = true;
//                    showFinishScreen();
//                }
//            }
//        });
    }

    private void setMediaPlayer() {
        mediaPlayer = new ExoPlayer.Builder(getApplicationContext())
                .build();
        mediaPlayer.setPlayWhenReady(true);
        videoLayout.setPlayer(mediaPlayer);

        //This loads the given videoUri to the media
        MediaItem mediaItem  = MediaItem.fromUri(videoUri);
        mediaPlayer.setMediaItem(mediaItem);
        mediaPlayer.prepare();

        videoLayout.setVisibility(View.INVISIBLE);
        setVideoFeatures();

        mediaPlayer.setPlaybackSpeed(1.0f);
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
                    Log.d(TAG, "TIME "+mediaPlayer.getDuration());
                    videoPlayerViewModel.setMovieSpendDurationSeconds(mediaPlayer.getCurrentPosition());
                    if (mediaPlayer.getDuration() != -1) {
                        Log.d(TAG, "DURATION "+mediaPlayer.getDuration());
                        videoPlayerViewModel.setMovieTotalDurationSeconds(mediaPlayer.getDuration());
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

    /**
     * This method is called as public method from bleService for updating the UI
     * @param rpm
     */
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
                                mediaPlayer.setPlaybackSpeed(RpmVectorLookupTable.getPlaybackspeed(rpm));
                            }
                            break;
                        case ACTIVE:
                            if (mediaPlayer!= null) {
                                mediaPlayer.setPlaybackSpeed(1.0f);
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

        bgSoundPlayerView.hideController();
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
        bgSoundPlayerView.setUseController(false);
        bgSoundPlayerView.hideController();
    }

    public void goToFrameNumber(int frameNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    long positionSecond = 0;

                    videoPlayerViewModel.setStatusbarVisible(false);
                    videoPlayerViewModel.setPlayerPaused(true);
                    bgSoundPlayerView.setVisibility(View.GONE);
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
                        mediaPlayer.seekTo(positionSecond*1000);
                    } else {
                        mediaPlayer.seekTo(0);
                    }

                    //Set number of false positives to 0 again as the player starts again
                    numberOfFalsePositives = 0;

                    waitUntilVideoIsReady(3);
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
        bgSoundPlayerView.setPlayer(null);
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
        bgSoundPlayerView.setPlayer(null);
    }

    private void toggleStatusScreen() {
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

        mediaPlayer.prepare();
        mediaPlayer.play();

        SharedPreferences sharedPreferences = getSharedPreferences("app", MODE_PRIVATE);
        String apikey = sharedPreferences.getString("apikey", "");

        if (getCurrentBackgroundSoundByCurrentPostion() != null) {
            switchToNewBackgroundMedia(getCurrentBackgroundSoundByCurrentPostion().getSoundUrl());
        } else {
            if (backgroundSoundTriggers != null && backgroundSoundTriggers.size()>0) {
                Log.d(TAG, "Empty currentBackGround at Start!");
                final String soundUrl = getFirstBackgroundSound().getSoundUrl();
                Log.d(TAG, "Starting first background sound at Start! " + soundUrl);
                switchToNewBackgroundMedia(soundUrl);
            }
        }

        bgSoundPlayerView.hideController();
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
        waitUntilVideoIsReady(this.minSecondsLoadingView);
    }

    private void waitUntilVideoIsReady(final int minSecondsLoadingView) {
        this.pauseTimer = 0;
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            int currentSecond = 0;
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    if (mediaPlayer!=null) {
                        try {
                            Log.d(TAG, "Mediaplayer is playing: "+mediaPlayer.isPlaying());
                            Log.d(TAG, "Seekable: " + mediaPlayer.isPlaying());
                            Log.d(TAG, "Time available: " + mediaPlayer.getDuration());
                            Log.d(TAG, "Length available: " + mediaPlayer.getContentDuration());
                        } catch (Exception exception) {
                            Log.e(TAG, exception.getLocalizedMessage());
                        }
                    }
                    if (    (currentSecond >= minSecondsLoadingView) &&
                            isPraxtourMediaPlayerReady() &&
                            (sensorConnected || ApplicationSettings.DEVELOPER_MODE)
                    ) {
                        isLoading = false;
                        playVideo();
                    } else {
                        isLoading = true;

                        currentSecond++;
                        //Maximize waiting for video and/or sensor
                        if (currentSecond > 30) {
                            Toast.makeText(VideoplayerExoActivity.this, getString(R.string.videoplayer_sensor_wait_error_message), Toast.LENGTH_LONG).show();
                            VideoplayerExoActivity.this.finish();
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

        Handler timelineHandler = new Handler(Looper.getMainLooper());
//        Handler timelineHandler = new Handler(thread.getLooper());
        Runnable runnableMovieDetails = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && !routeFinished) {
                    if (mediaPlayer.getDuration()/1000L < 2) {
                        float volumeLevel = ApplicationSettings.DEFAULT_SOUND_VOLUME / 100f;
                        Log.d(TAG, "VolumeLevel: "+volumeLevel);
                        mediaPlayer.setVolume(volumeLevel); //CONVERT TO float
                        mediaPlayer.setDeviceVolume(ApplicationSettings.DEFAULT_SOUND_VOLUME);
                    }

                    //check for current backgroundsound
                    BackgroundSound backgroundSound = getCurrentBackgroundSoundByCurrentPostion();
                    if ( backgroundSound != null) {
                        Log.d(TAG, "EventTimeLineHandler: "+backgroundSound.getSoundId().intValue() + " on second: "+mediaPlayer.getDuration()/1000L);
                        Log.d(TAG, "BgSound with id: "+backgroundSound.getSoundId().intValue()+" state if playing: "+backgroundSoundPlayer.isPlaying());
                        Log.d(TAG, "BgSound volume: "+backgroundSoundPlayer.getVolume());
                        Log.d(TAG, "BgSound dev volume: "+backgroundSoundPlayer.getDeviceVolume());
                        if (!backgroundSoundPlayer.isPlaying() && !routePaused && !routeFinished) {
                            backgroundSoundPlayer.play();
                        }
//                        switchToNewBackgroundMedia(getCurrentBackgroundSoundByCurrentPostion().getSoundUrl());
                    }

                    //set to new
                    checkBackgroundSoundMedia();
                }

                //PAUSE TIMER
                if (routePaused) {
                    if (pauseTimer > MAX_PAUSE_TIME_SEC || backToOverviewWaitForSensor) {
                        stopVideoplayer();
                    } else {
                        pauseTimer++;
                    }
                }

                if (routeFinished) {
                    Log.d(TAG, "backToOverviewSensorWait = "+backToOverviewWaitForSensor);
                    if (backToOverviewWaitForSensor) {
                        stopVideoplayer();
                    }
                }

                timelineHandler.postDelayed(this::run, 1000);
            }
        };

        if (!routeFinished) {
            timelineHandler.postDelayed(runnableMovieDetails, 0);
        }
    }

    private void stopVideoplayer() {
        if (backgroundSoundPlayer != null && backgroundSoundPlayer.isPlaying()) {
            backgroundSoundPlayer.stop();
        }
        VideoplayerExoActivity.this.finish();
    }

    private void checkBackgroundSoundMedia() {
        if (backgroundSoundTriggers.size()>0) {
            long currentSecond = (mediaPlayer.getDuration() / 1000L);
            for (final BackgroundSound backgroundSound : backgroundSoundTriggers) {
                if ((backgroundSound.getFramenumber()/selectedMovie.getRecordedFps()) == currentSecond) {
                    Log.d(TAG, "Framenumber to switch to soundId: "+backgroundSound.getFramenumber()+", "+backgroundSound.getSoundNumber()+" > "+backgroundSound.getSoundUrl());
                    switchToNewBackgroundMedia(backgroundSound.getSoundUrl());
                }
            }
        }
    }

    private void switchToNewBackgroundMedia(final String backgroundSoundurl) {
        if (backgroundSoundTriggers.size()>0) {
            String localFileName = getSoundFileName(Uri.parse(backgroundSoundurl));

            if (mediaPlayer.getCurrentMediaItem() != null
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
    private BackgroundSound getCurrentBackgroundSoundByCurrentPostion() {
        BackgroundSound selectBackgroundSound = null;
        long currentSecond = (mediaPlayer.getDuration() / 1000L);
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
        boolean videoPlayerReady;
        boolean backgroundPlayerReady;
        boolean soundReady = backgroundSoundTriggers.size()>0;

//        videoPlayerReady = (mediaPlayer.isSeekable() && mediaPlayer.getLength() != -1 && mediaPlayer.getVideoTrack()>2);
        videoPlayerReady = (mediaPlayer.isPlaying() && mediaPlayer.getContentDuration() != -1);
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

    private void setUp() {
        //START INIT SENSORS
        if (!selectedProduct.getCommunicationType().toLowerCase().equals("none")) {
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
        if (communicationType != CommunicationType.NONE) {
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

    private void initializeVlcVideoPlayer() {
        if (mediaPlayer == null) {
            setMediaPlayer();
        } else {
            Log.d(TAG, String.format("Mediaplayer already exists with aspect ratio: {} : {}", mediaPlayer.getVideoSize().width, mediaPlayer.getVideoSize().height ));
        }
    }

//    private void initializeVideoPlayer() {
//        if (videoPlayer == null) {
//            // Create the player
//            final MediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);
//
////            Trackselector to retrieve and start the best track in mp4
//            DefaultTrackSelector trackSelector = new DefaultTrackSelector(getApplicationContext());
//            trackSelector.setParameters(
//                trackSelector.buildUponParameters()
//                        .setForceHighestSupportedBitrate(true)
//                        .build()
//            );
//
//            DefaultLoadControl defaultLoadControl = new DefaultLoadControl.Builder()
//                    .setPrioritizeTimeOverSizeThresholds(true)//default = false
////                    .setBufferDurationsMs(50000,50000, 20000,20000)
//                    .build();
//
//            videoPlayer = new SimpleExoPlayer.Builder(this)
////                    .setLoadControl(defaultLoadControl)
//                    //.setTrackSelector(trackSelector) Not using the trackselector
//                    .setMediaSourceFactory(mediaSourceFactory)
//                    .build();
//
//            // Set speed of the video (hence buffering /streaming speed )
//            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
//            videoPlayer.setPlaybackParameters(playbackParameters);
//
//            //Set hardware sound to maximum
//            backgroundSoundPlayer.setDeviceVolume(backgroundSoundPlayer.getDeviceInfo().maxVolume);
//
//            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
//            //Set player on playerview
//            playerView.setPlayer(videoPlayer);
//        }
//    }

    private void initializeBackgroundSoundPlayer() {
        if (backgroundSoundPlayer == null) {
            backgroundSoundPlayer = new ExoPlayer.Builder(this).build();
            PlaybackParameters playbackParameters  = new PlaybackParameters(1.0f, PlaybackParameters.DEFAULT.pitch);
            backgroundSoundPlayer.setPlaybackParameters(playbackParameters);
            backgroundSoundPlayer.setDeviceVolume(backgroundSoundPlayer.getDeviceInfo().maxVolume);
            bgSoundPlayerView.setPlayer(backgroundSoundPlayer);
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
        if (mediaPlayer!=null) {
            mediaPlayer.setPlayWhenReady(false);
            mediaPlayer.getPlaybackState();
            mediaPlayer.pause();
        }
    }

    private void resumePlayer() {
        if (mediaPlayer!=null) {
            mediaPlayer.setPlayWhenReady(true);
            mediaPlayer.getPlaybackState();
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