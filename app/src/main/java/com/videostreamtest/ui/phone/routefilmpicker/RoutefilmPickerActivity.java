package com.videostreamtest.ui.phone.routefilmpicker;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

import static com.videostreamtest.ui.phone.routefilmpicker.RoutefilmAdapter.ITEMS_PER_ROW;
import static com.videostreamtest.ui.phone.routefilmpicker.RoutefilmAdapter.MAX_VISIBLE_ROWS;
import static com.videostreamtest.ui.phone.routefilmpicker.RoutefilmAdapter.SINGLE_ITEM_HEIGHT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.RoutefilmRepository;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.helpers.ConfigurationHelper;
import com.videostreamtest.helpers.DownloadHelper;
import com.videostreamtest.helpers.ViewHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.workers.download.DownloadFlagsServiceWorker;
import com.videostreamtest.workers.download.DownloadMovieImagesServiceWorker;
import com.videostreamtest.workers.download.DownloadRoutepartsServiceWorker;
import com.videostreamtest.workers.download.DownloadSoundServiceWorker;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RoutefilmPickerActivity extends AppCompatActivity {
    private static final String TAG = RoutefilmPickerActivity.class.getSimpleName();

    /** The number of routefilms displayed per row in {@code this#routefilmsRecyclerView}. */
    final static int DEFAULT_SELECTED_POSITION = 0;
    private int counter = 0;

    // DATA
    private Product selectedProduct;
    private Routefilm[] availableRoutefilms;
    private TextView appAndAccountInfoTextView;
    private String apikey;
    private boolean chinesportAccount;

    // DATA FLOW
    private final RoutefilmRepository routefilmRepository = new RoutefilmRepository(getApplication());
    private final ConfigurationRepository configurationRepository = new ConfigurationRepository(getApplication());

    // VIEWS
    private ImageView productLogoView;
    private Button backToProductPickerButton;
    private RecyclerView routefilmsRecyclerView;
    private RoutefilmAdapter routefilmAdapter;
    private ConstraintLayout navigationPad;
    private ImageButton navigationUpArrow;
    private ImageButton navigationLeftArrow;
    private ImageButton navigationRightArrow;
    private ImageButton navigationDownArrow;
    private ImageView chinesportLogo;

    // SELECTED ROUTEFILM INFO
    private Routefilm selectedRoutefilm;
    private int selectedRoutefilmPosition;
    private TextView selectedRoutefilmTitleTextView;

    /** For displaying duration/distance. */
    private ConstraintLayout selectedRoutefilmInformationBoxLayout;
    private TextView selectedRoutefilmInfoTextView;
    private ImageView selectedRoutefilmMapImageView;
    private ImageView selectedRoutefilmCountryFlagImageView;
    private final Handler backToProductPickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable backToProductPickerRunnable = new Runnable() {
        final int BACK_TO_PRODUCT_PICKER_SECONDS = 60 * 5; // 5 minutes
        @Override
        public void run() {
            if (counter < BACK_TO_PRODUCT_PICKER_SECONDS) {
                counter++;
                backToProductPickerHandler.postDelayed(this, 1000);
            } else {
                Intent intent = new Intent(RoutefilmPickerActivity.this, ProductPickerActivity.class);
                startActivity(intent);
            }
        }
    };

    private final BroadcastReceiver mqttBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case "com.videostreamtest.ACTION_START_FILM":
                    if (routefilmAdapter != null) {
                        routefilmAdapter.startVideoPlayer(true);
                    }
                    break;
                case "com.videostreamtest.ACTION_ARROW":
                    String direction = intent.getStringExtra("direction");
                    if (direction == null) return;
                    navigate(direction);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routefilm_picker);
        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        selectedProduct = new GsonBuilder().create().fromJson(getIntent().getExtras().getString("product_object", "{}"), Product.class);
        apikey = AccountHelper.getAccountToken(this);
        if (apikey == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            Toast.makeText(this, "There was an error with your account", Toast.LENGTH_LONG).show();
            startActivity(loginIntent);
            return;
        }
        chinesportAccount = AccountHelper.isChinesportAccount(this);

        initViews();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstance) {
        super.onPostCreate(savedInstance);

        configurationRepository.getCurrentConfiguration().observe(this, currentConfig -> {
            if (currentConfig != null) {
                periodicSyncDownloadMovieRouteParts(currentConfig.getAccountToken());
            }
        });

    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        counter = 0;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (chinesportAccount) initMotolifeReceivers();
        backToProductPickerHandler.postDelayed(backToProductPickerRunnable, 1000);

        //TODO: switch to onetime only executions on startup phase or productpicker periodic updater.
        downloadFlags();
        downloadMovieSupportImages();
        downloadSound();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (chinesportAccount) LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttBroadcastReceiver);
        backToProductPickerHandler.removeCallbacksAndMessages(null);
    }

    private void initViews() {
        productLogoView = findViewById(R.id.product_logo_view);
        Picasso.get()
                .load(selectedProduct.getProductLogoButtonPath())
                .resize(300, 225)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .into(productLogoView);

        chinesportLogo = findViewById(R.id.chinesport_logo_imageview);
        if (chinesportAccount) {
            chinesportLogo.setVisibility(View.VISIBLE);
        } else {
            chinesportLogo.setVisibility(View.GONE);
        }

        backToProductPickerButton = findViewById(R.id.back_to_productpicker_button);
        backToProductPickerButton.setOnClickListener(view -> finish());

        appAndAccountInfoTextView = findViewById(R.id.app_account_info_textview);
        String appVersionNumber = ConfigurationHelper.getVersionNumber(this);
        String appAndAccountInfo = String.format("%s:%s", appVersionNumber, apikey);
        appAndAccountInfoTextView.setText(appAndAccountInfo);

        navigationPad = findViewById(R.id.navigation_pad);
        navigationUpArrow = findViewById(R.id.navigation_up_arrow);
        navigationLeftArrow = findViewById(R.id.navigation_left_arrow);
        navigationRightArrow = findViewById(R.id.navigation_right_arrow);
        navigationDownArrow = findViewById(R.id.navigation_down_arrow);

        if (ViewHelper.isTouchScreen(this)) {
            initNavigationArrow(navigationUpArrow, -1 * ITEMS_PER_ROW);
            initNavigationArrow(navigationLeftArrow, -1);
            initNavigationArrow(navigationRightArrow, 1);
            initNavigationArrow(navigationDownArrow, ITEMS_PER_ROW);
            navigationPad.setVisibility(View.VISIBLE);
        } else {
            navigationPad.setVisibility(View.GONE);
        }

        selectedRoutefilmInformationBoxLayout = findViewById(R.id.selected_routefilm_information_layout);
        selectedRoutefilmInformationBoxLayout.setVisibility(View.GONE);
        selectedRoutefilmTitleTextView = findViewById(R.id.selected_routefilm_title_textview);
        selectedRoutefilmInfoTextView = findViewById(R.id.selected_routefilm_info_textview);
        selectedRoutefilmCountryFlagImageView = findViewById(R.id.selected_routefilm_flag_imageview);
        if (!isProductDistanceBased(selectedProduct)) {
            selectedRoutefilmCountryFlagImageView.setVisibility(View.GONE);
        }
        selectedRoutefilmMapImageView = findViewById(R.id.selected_routefilm_map_imageview);

        loadProductMovies();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (ViewHelper.isTouchScreen(this)) return false;

        // NB: The OK/SELECT button has to be handled in RoutefilmAdapter. If defined here,
        // it will not work as expected.
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                int position = routefilmAdapter.getSelectedRoutefilmPosition();

                if (position % ITEMS_PER_ROW == 0) {
                    backToProductPickerButton.requestFocus();
                    selectedRoutefilmPosition = position;
                    routefilmAdapter.setSelectedRoutefilmPosition(RecyclerView.NO_POSITION);
                    routefilmAdapter.notifyItemChanged(position);
                } else {
                    jump(-1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (backToProductPickerButton.hasFocus()) {
                    routefilmsRecyclerView.requestFocus();
                    routefilmsRecyclerView.scrollToPosition(selectedRoutefilmPosition);
                    routefilmAdapter.setSelectedRoutefilmPosition(selectedRoutefilmPosition);
                    routefilmAdapter.notifyItemChanged(selectedRoutefilmPosition);
                } else {
                    jump(1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                jump(-4);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                jump(4);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initMotolifeReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.videostreamtest.ACTION_START_FILM");
        intentFilter.addAction("com.videostreamtest.ACTION_ARROW");

        LocalBroadcastManager.getInstance(this).registerReceiver(mqttBroadcastReceiver, intentFilter);
    }

    private void loadProductMovies() {
        if (!apikey.isEmpty()) {
            String accountType = AccountHelper.getAccountType(this);
            if (accountType == null) {
                return;
            }

            LiveData<List<Routefilm>> routefilmsLiveData;

            switch (accountType.toLowerCase()) {
                case "standalone":
                case "hybrid":
                case "motolife":
                    routefilmsLiveData = routefilmRepository.getAllStandaloneProductRoutefilms(apikey);
                    break;
                case "streaming":
                    routefilmsLiveData = routefilmRepository.getAllProductRoutefilms(apikey);
                    break;
                default:
                    Toast.makeText(this, "Could not load movies", Toast.LENGTH_LONG).show();
                    Log.d(TAG, String.format("Retrieved account type was %s, could not load movies", accountType));
                    return;
            }

            Observer<List<Routefilm>> oneTimeObserver = new Observer<>() {
                @Override
                public void onChanged(List<Routefilm> routefilms) {
                    if (routefilms != null) {
                        availableRoutefilms = new Routefilm[routefilms.size()];
                        availableRoutefilms = routefilms.toArray(availableRoutefilms);
                        preLoadImages(routefilms);
                        initRecyclerView();
                        routefilmsLiveData.removeObserver(this);
                    }
                }
            };
            routefilmsLiveData.observe(this, oneTimeObserver);
        }

//        new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);
//        new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);
//        new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);
    }

    private void initRecyclerView() {
        routefilmsRecyclerView = findViewById(R.id.routefilms_recyclerview);
        routefilmsRecyclerView.setLayoutManager(new GridLayoutManager(this, ITEMS_PER_ROW));
        ViewGroup.LayoutParams params = routefilmsRecyclerView.getLayoutParams();
        params.height = SINGLE_ITEM_HEIGHT * MAX_VISIBLE_ROWS;
        routefilmsRecyclerView.setLayoutParams(params);

        RoutefilmAdapter.SelectedRoutefilmListener listener = createListener();

        routefilmAdapter = new RoutefilmAdapter(availableRoutefilms, selectedProduct, listener, this);
        routefilmAdapter.sortByFavorites();
        routefilmsRecyclerView.setAdapter(routefilmAdapter);

        new Handler().postDelayed(() -> {
            routefilmsRecyclerView.scrollToPosition(DEFAULT_SELECTED_POSITION);
            routefilmAdapter.setSelectedRoutefilmPosition(DEFAULT_SELECTED_POSITION);
            if (availableRoutefilms != null && availableRoutefilms.length > 0) {
                selectedRoutefilmInformationBoxLayout.setVisibility(View.VISIBLE);
                routefilmsRecyclerView.requestFocus();
            }
        }, 200);

        routefilmsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // rebind visible items only
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    if (layoutManager instanceof GridLayoutManager) {
                        GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
                        int firstVisible = gridLayoutManager.findFirstVisibleItemPosition();
                        int lastVisible = gridLayoutManager.findLastVisibleItemPosition();
                        for (int i = firstVisible; i <= lastVisible; i++) {
                            routefilmAdapter.notifyItemChanged(i);
                        }
                    }
                }
            }
        });
    }

    /** Returns an interface implementation {@link RoutefilmAdapter.SelectedRoutefilmListener}
     * that listens to changes in selection and updates the information box
     * at the top of the screen. */
    private RoutefilmAdapter.SelectedRoutefilmListener createListener() {
        return routefilm -> {
            selectedRoutefilm = routefilm;
            Movie movie = Movie.fromRoutefilm(routefilm);
            // Flag
            if (movie.getMovieFlagUrl() != null && !movie.getMovieFlagUrl().isEmpty()) {
                selectedRoutefilmCountryFlagImageView.setVisibility(View.VISIBLE);
                if (DownloadHelper.isFlagsLocalPresent(getApplicationContext())) {
                    Picasso.get()
                            .load(new File(movie.getMovieFlagUrl()))
                            .placeholder(R.drawable.flag_placeholder)
                            .error(R.drawable.flag_placeholder)
                            .resize(150, 100)
                            .into(selectedRoutefilmCountryFlagImageView);
                } else {
                    Picasso.get()
                            .load(movie.getMovieFlagUrl())
                            .placeholder(R.drawable.flag_placeholder)
                            .error(R.drawable.flag_placeholder)
                            .resize(150, 100)
                            .into(selectedRoutefilmCountryFlagImageView);
                }
            } else {
                selectedRoutefilmCountryFlagImageView.setVisibility(View.GONE);
            }

            // Route map
            if (selectedProduct.getSupportStreaming()==0 && performStaticChecks(movie)) {
                Picasso.get()
                        .load(new File(movie.getMovieRouteinfoPath()))
                        .fit()
                        .placeholder(R.drawable.placeholder_map)
                        .error(R.drawable.placeholder_map)
                        .into(selectedRoutefilmMapImageView);
            } else {
                Picasso.get()
                        .load(movie.getMovieRouteinfoPath())
                        .fit()
                        .placeholder(R.drawable.placeholder_map)
                        .error(R.drawable.placeholder_map)
                        .into(selectedRoutefilmMapImageView);
            }

            // Title
            selectedRoutefilmTitleTextView.setText(routefilm.getMovieTitle());

            // Duration/Distance
            String info;
            if (isProductDistanceBased(selectedProduct)) {
                float meters = routefilm.getMovieLength();
                int km = (int) (meters / 1000f);
                int hectometers = (int) ((meters - (km * 1000f)) / 100f);
                info = String.format(getApplicationContext().getString(R.string.catalog_screen_distance), km, hectometers);
            } else {
                int duration = (routefilm.getMovieLength() / routefilm.getRecordedFps()) / 60;
                info = String.format("Duration: %d minutes", duration);
            }
            selectedRoutefilmInfoTextView.setText(info);
        };
    }

    private void initNavigationArrow(ImageButton arrowView, int jumpCount) {
        arrowView.setOnClickListener(view -> jump(jumpCount));
    }

    private void jump(int jumpCount) {
        if (!routefilmsRecyclerView.hasFocus()) return;

        final int prevPosition = routefilmAdapter.getSelectedRoutefilmPosition();
        final int nextPosition;
        if (jumpCount < 0) {
            nextPosition = Math.max(prevPosition + jumpCount, 0);
        } else {
            nextPosition = Math.min(prevPosition + jumpCount, routefilmAdapter.getItemCount() - 1);
        }

        if (prevPosition != nextPosition && routefilmAdapter != null) {
            routefilmAdapter.setSelectedRoutefilmPosition(nextPosition);
            routefilmAdapter.notifyItemChanged(prevPosition);
            routefilmAdapter.notifyItemChanged(nextPosition);
            routefilmsRecyclerView.getLayoutManager().scrollToPosition(nextPosition);
            selectedRoutefilmPosition = nextPosition;
        }
    }

    private void navigate(String direction) {
        switch (direction) {
            case "up":
                jump(-4);
                break;
            case "down":
                jump(4);
                break;
            case "right":
                jump(1);
                break;
            case "left":
                jump(-1);
                break;
        }
    }

    private boolean isProductDistanceBased(Product product) {
        String productName = product.getProductName().toLowerCase();
        return productName.contains("praxspin") || productName.contains("praxfit");
    }

    private boolean performStaticChecks(Movie movie) {
        Context appContext = getApplicationContext();
        // StandAlone
        if (selectedProduct.getSupportStreaming()==0) {
            boolean isMovieOnDevice = DownloadHelper.isMoviePresent(appContext, movie);
            boolean isSoundOnDevice = DownloadHelper.isSoundPresent(appContext);
            boolean isMovieSupportImagesOnDevice = DownloadHelper.isMovieImagesPresent(appContext, movie);
            return (isMovieOnDevice&&isSoundOnDevice&&isMovieSupportImagesOnDevice);
        }
        // Streaming
        else {
            return true;
        }
    }

    private void preLoadImages(List<Routefilm> routefilmList) {
        for (final Routefilm routefilm: routefilmList) {
            //Load local paths if standalone
            if (selectedProduct!= null && selectedProduct.getSupportStreaming()==0) {
                DownloadHelper.setLocalMedia(getApplicationContext(), routefilm);
            }
            //preload scenery
            if (routefilm.getMovieRouteinfoPath().startsWith("/")) {
                Picasso.get().load(new File(routefilm.getMovieRouteinfoPath())).fetch();
            } else {
                Picasso.get().load(routefilm.getMovieRouteinfoPath()).fetch();
            }
            //preload maps
            if (routefilm.getMovieImagepath().startsWith("/")) {
                Picasso.get().load(new File(routefilm.getMovieImagepath())).fetch();
            } else {
                Picasso.get().load(routefilm.getMovieImagepath()).fetch();
            }
            //preload flags
            if (routefilm.getMovieFlagUrl() != null && !routefilm.getMovieFlagUrl().isEmpty()) {
                if (routefilm.getMovieFlagUrl().startsWith("/")) {
                    Picasso.get().load(new File(routefilm.getMovieFlagUrl())).fetch();
                } else {
                    Picasso.get().load(routefilm.getMovieFlagUrl()).fetch();
                }
            }
        }
    }

    // TODO: duplicate... exact same methods exist in ProductPickerActivity. Unify somewhere.
    private void downloadSound() {
        if (!DownloadHelper.isSoundPresent(getApplicationContext())) {
            Constraints constraint = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest downloadSoundWorker = new OneTimeWorkRequest.Builder(DownloadSoundServiceWorker.class)
                    .setConstraints(constraint)
                    .setInputData(new Data.Builder().putString("apikey", AccountHelper.getAccountToken(getApplicationContext())).build())
                    .build();

            WorkManager.getInstance(this)
                    .beginUniqueWork("download-sound", ExistingWorkPolicy.KEEP, downloadSoundWorker)
                    .enqueue();
        }
    }

    private void downloadFlags() {
        if (AccountHelper.isLocalPlay(getApplicationContext())) {
            Constraints constraint = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest downloadFlagsWorker = new OneTimeWorkRequest.Builder(DownloadFlagsServiceWorker.class)
                    .setConstraints(constraint)
                    .setInputData(new Data.Builder().putString("apikey", AccountHelper.getAccountToken(getApplicationContext())).build())
                    .build();

            WorkManager.getInstance(this)
                    .beginUniqueWork("download-sound", ExistingWorkPolicy.KEEP, downloadFlagsWorker)
                    .enqueue();
        }
    }

    private void downloadMovieSupportImages() {
        routefilmRepository.getAllRoutefilms(AccountHelper.getAccountToken(this)).observe(this, routefilms -> {
            if (routefilms.size() > 0 && AccountHelper.isLocalPlay(getApplicationContext())) {
                for (Routefilm routefilm : routefilms) {
                    //SPECIFY INPUT
                    Data.Builder mediaDownloader = new Data.Builder();
                    mediaDownloader.putString("INPUT_ROUTEFILM_JSON_STRING", new GsonBuilder().create().toJson(Movie.fromRoutefilm(routefilm), Movie.class));
                    mediaDownloader.putString("localMediaServer", AccountHelper.getAccountMediaServerUrl(getApplicationContext()));

                    //COSNTRAINTS
                    Constraints constraint = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();
                    //WORKREQUEST
                    OneTimeWorkRequest downloadMovieSupportImagesWorkRequest = new OneTimeWorkRequest.Builder(DownloadMovieImagesServiceWorker.class)
                            .setConstraints(constraint)
                            .setInputData(mediaDownloader.build())
                            .addTag("support-images-routefilm-"+routefilm.getMovieId())
                            .build();
                    //START WORKING
                    WorkManager.getInstance(this)
                            .beginUniqueWork("download-support-images-"+routefilm.getMovieId(), ExistingWorkPolicy.KEEP, downloadMovieSupportImagesWorkRequest)
                            .enqueue();
                }
            }
        });
    }

    private void periodicSyncDownloadMovieRouteParts(final String apikey) {
        Data.Builder mediaDownloader = new Data.Builder();
        mediaDownloader.putString("apikey", apikey);

        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest routepartsDownloadRequest = new PeriodicWorkRequest.Builder(DownloadRoutepartsServiceWorker.class, 35, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(mediaDownloader.build())
                .addTag("download-movieparts")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-pms-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, routepartsDownloadRequest);

    }
}