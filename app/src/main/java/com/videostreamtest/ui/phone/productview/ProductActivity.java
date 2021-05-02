package com.videostreamtest.ui.phone.productview;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import com.videostreamtest.ui.phone.productview.fragments.TouchScreenFragment;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.workers.DownloadMovieServiceWorker;
import com.videostreamtest.workers.DownloadSoundServiceWorker;
import com.videostreamtest.workers.LocalMediaServerAvailableServiceWorker;

import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity {

    private ProductViewModel productViewModel;
    private Button signoutButton;
    private ImageView productLogo;

    private boolean refreshData = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        signoutButton = findViewById(R.id.product_logout_button);
        productLogo = findViewById(R.id.product_logo_view);

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
//                if (!(currentConfig.getProductCount()>0)) {
//                    Toast.makeText(this, "No active subscriptions.", Toast.LENGTH_LONG).show();
//                    productViewModel.signoutCurrentAccount(currentConfig);
//                    //Cancel all workers (in case of downloading)
//                    WorkManager
//                            .getInstance(getApplicationContext())
//                            .cancelAllWork();
//                    System.exit(0);
//                }
                if (refreshData) {
                    refreshData = false;
                    //TODO: WHEN PRODUCT COUNT IS 0 then logout
                    ConfigurationHelper.loadExternalData(this, currentConfig.getAccountToken());
                }

                Bundle arguments = getIntent().getExtras();
                arguments.putString("communication_device", currentConfig.getCommunicationDevice());

                getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container_view, TouchScreenFragment.class, arguments)
                        .commit();

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

        /**
         * TODO: pseudo steps
         *  1. get current configuration
         *  2. get current selected product
         *  3. Get screen type ( Touch or Plain )
         *  4. Is account for standalone or streaming
         *  4. Build up activity with fragments and/or nav_graphs
         */
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData = true;
        downloadSound();
        downloadLocalMovies();
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

                                OneTimeWorkRequest downloadSoundWorker = new OneTimeWorkRequest.Builder(DownloadSoundServiceWorker.class)
                                        .setConstraints(constraint)
                                        .setInputData(new Data.Builder().putString("apikey", currentConfig.getAccountToken()).build())
                                        .build();

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
