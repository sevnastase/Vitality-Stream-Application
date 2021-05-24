package com.videostreamtest.ui.phone.productview;

import android.app.ActionBar;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.productview.fragments.PlainScreenFragment;
import com.videostreamtest.ui.phone.productview.fragments.TouchScreenFragment;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.workers.DownloadMovieServiceWorker;
import com.videostreamtest.workers.DownloadRoutepartsServiceWorker;
import com.videostreamtest.workers.DownloadSoundServiceWorker;
import com.videostreamtest.workers.LocalMediaServerAvailableServiceWorker;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

public class ProductActivity extends AppCompatActivity {

    private ProductViewModel productViewModel;
    private Button signoutButton;
    private ImageView productLogo;
    private TextView appBuildNumber;

    private boolean refreshData = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        signoutButton = findViewById(R.id.product_logout_button);
        productLogo = findViewById(R.id.product_logo_view);
        appBuildNumber = findViewById(R.id.app_build_number);

        Product selectedProduct = new GsonBuilder().create().fromJson(getIntent().getExtras().getString("product_object", "{}"), Product.class);
        Log.d(ProductActivity.class.getSimpleName(), "Product ID Loaded: "+selectedProduct.getId());

        //Set product logo in view
        Picasso.get()
                .load(selectedProduct.getProductLogoButtonPath())
                .resize(300, 225)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .into(productLogo);

        productViewModel.getCurrentConfig().observe(this, currentConfig ->{
            if (currentConfig != null) {
                Log.d(getClass().getSimpleName(), "currentConfig pCount: "+currentConfig.getProductCount() + " Bundle pCount: 1");
                if (refreshData) {
                    refreshData = false;
                    ConfigurationHelper.loadExternalData(this, currentConfig.getAccountToken());

                    Data.Builder mediaDownloader = new Data.Builder();
                    mediaDownloader.putString("apikey",  currentConfig.getAccountToken());
                    mediaDownloader.putInt("movie-id", 1);
                    Constraints constraint = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();
                    OneTimeWorkRequest routepartsWorkRequest = new OneTimeWorkRequest.Builder(DownloadRoutepartsServiceWorker.class)
                            .setConstraints(constraint)
                            .setInputData(mediaDownloader.build())
                            .addTag("routeparts-routefilm-1")
                            .build();
                    WorkManager.getInstance(this)
                            .beginUniqueWork("download-movieparts-1", ExistingWorkPolicy.KEEP, routepartsWorkRequest)
                            .enqueue();
                }

                Bundle arguments = getIntent().getExtras();
                arguments.putString("communication_device", currentConfig.getCommunicationDevice());

                loadFragmentBasedOnScreenType(arguments);

                appBuildNumber.setText(ConfigurationHelper.getVersionNumber(getApplicationContext()));

                if (currentConfig.getProductCount() > 1) {
                    signoutButton.setText("Close");
                }
                signoutButton.setOnClickListener(view -> {
                    if (currentConfig.getProductCount() == 1) {
                        productViewModel.signoutCurrentAccount(currentConfig);
                        //Cancel all workers (in case of downloading)
                        WorkManager
                                .getInstance(getApplicationContext())
                                .cancelAllWork();
                    }
                    ProductActivity.this.finish();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData = true;
        downloadSound();
        downloadLocalMovies();
        downloadMovieRouteParts();
    }

    private void loadFragmentBasedOnScreenType(final Bundle arguments) {
        if (isTouchScreen()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view, TouchScreenFragment.class, arguments)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view, PlainScreenFragment.class, arguments)
                    .commit();
        }
    }

    private void downloadSound() {
        productViewModel.getCurrentConfig().observe(this, currentConfig -> {
            if (currentConfig != null) {
                if (currentConfig.isLocalPlay() && !DownloadHelper.isSoundPresent(getApplicationContext())) {
                    Constraints constraint = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();

                    OneTimeWorkRequest downloadSoundWorker = new OneTimeWorkRequest.Builder(DownloadSoundServiceWorker.class)
                            .setConstraints(constraint)
                            .setInputData(new Data.Builder().putString("apikey", currentConfig.getAccountToken()).build())
                            .build();

                    WorkManager.getInstance(this)
                            .beginUniqueWork("download-sound", ExistingWorkPolicy.KEEP, downloadSoundWorker)
                            .enqueue();
                }
            }
        });
    }

    private void downloadMovieRouteParts() {
        productViewModel.getCurrentConfig().observe(this, currentConfig -> {
            if (currentConfig != null) {
                productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(this, routefilms -> {
                    if (routefilms.size() > 0 && currentConfig.isLocalPlay()) {
                        for (Routefilm routefilm : routefilms) {
                            Data.Builder mediaDownloader = new Data.Builder();
                            mediaDownloader.putString("apikey",  currentConfig.getAccountToken());
                            mediaDownloader.putInt("movie-id", routefilm.getMovieId());
                            Constraints constraint = new Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build();
                            OneTimeWorkRequest routepartsWorkRequest = new OneTimeWorkRequest.Builder(DownloadRoutepartsServiceWorker.class)
                                    .setConstraints(constraint)
                                    .setInputData(mediaDownloader.build())
                                    .addTag("routeparts-routefilm-"+routefilm.getMovieId())
                                    .build();
                            WorkManager.getInstance(this)
                                    .beginUniqueWork("download-movieparts-"+routefilm.getMovieId(), ExistingWorkPolicy.KEEP, routepartsWorkRequest)
                                    .enqueue();
                        }
                    }
                });
            }
        });
    }

    private void downloadLocalMovies() {
        productViewModel.getCurrentConfig().observe(this, currentConfig -> {
            if (currentConfig != null) {
                productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(this, routefilms -> {
                    if (routefilms.size()>0 && currentConfig.isLocalPlay()) {
                        for (Routefilm routefilm : routefilms) {
                            if (!DownloadHelper.isMoviePresent(getApplicationContext(), Movie.fromRoutefilm(routefilm))) {
                                Constraints constraint = new Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build();

                                Data.Builder mediaDownloader = new Data.Builder();
                                mediaDownloader.putString("INPUT_ROUTEFILM_JSON_STRING", new GsonBuilder().create().toJson(Movie.fromRoutefilm(routefilm), Movie.class));
                                mediaDownloader.putString("localMediaServer", currentConfig.getPraxCloudMediaServerLocalUrl());

                                OneTimeWorkRequest availabilityWorker = new OneTimeWorkRequest.Builder(LocalMediaServerAvailableServiceWorker.class)
                                        .setConstraints(constraint)
                                        .setInputData(mediaDownloader.build())
                                        .addTag("local-" + routefilm.getMovieId())
                                        .build();

                                OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DownloadMovieServiceWorker.class)
                                        .setConstraints(constraint)
                                        .setInputData(mediaDownloader.build())
                                        .addTag("routefilm-" + routefilm.getMovieId())
                                        .build();

                                WorkManager.getInstance(this)
                                        .beginUniqueWork("download-movie-" + routefilm.getMovieId(), ExistingWorkPolicy.KEEP, availabilityWorker)
                                        .then(oneTimeWorkRequest)
                                        .enqueue();
                            }
                        }
                    }
                });
            }
        });
    }

    private boolean isTouchScreen() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }
}
