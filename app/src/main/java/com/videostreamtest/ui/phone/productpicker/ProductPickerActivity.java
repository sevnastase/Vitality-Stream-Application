package com.videostreamtest.ui.phone.productpicker;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.screensaver.ScreensaverActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.ActiveProductsServiceWorker;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

public class ProductPickerActivity extends AppCompatActivity {
    private final static String TAG = ProductPickerActivity.class.getSimpleName();

    private ProductPickerViewModel productPickerViewModel;
    private RecyclerView productOverview;
    private boolean refreshData = false;

    private Button signoutButton;

    private Handler screensaverhandler;
    private Looper screensaverLooper;
    private Runnable screensaverRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productpicker);

        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        productPickerViewModel = new ViewModelProvider(this).get(ProductPickerViewModel.class);

        initScreensaverHandler();
        startScreensaverHandler();

         /*
        Logout button
         */
        signoutButton = findViewById(R.id.productpicker_logout_button);
        signoutButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    final Drawable border = v.getContext().getDrawable(R.drawable.imagebutton_blue_border);
                    signoutButton.setBackground(border);
                } else {
                    signoutButton.setBackground(null);
                }
            }
        });

        /*
        Haal de producten op om vervolgens in de recyclerview weer te geven
         */

        //Koppel de recyclerView aan de layout xml
        productOverview = findViewById(R.id.recyclerview_products);
        productOverview.setHasFixedSize(true);

        productPickerViewModel.getCurrentConfig().observe(this, config -> {
            if (config != null) {
                if (refreshData) {
                    refreshData = false;
                    ConfigurationHelper.loadExternalData(this, config.getAccountToken());
                }
                // Add action onClick to signout button
                signoutButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences sp = getApplication().getSharedPreferences("app",0);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.clear();
                        editor.commit();
                        Log.d(TAG, "Finish productpicker");

                        config.setCurrent(false);
                        productPickerViewModel.updateConfiguration(config);

                        //Cancel all workers (in case of downloading)
                        WorkManager
                                .getInstance(getApplicationContext())
                                .cancelAllWork();

                        ProductPickerActivity.this.finish();
                    }
                });

                if(config.isLocalPlay()) {
                    signoutButton.setVisibility(View.GONE);
                }

                productPickerViewModel.getAccountProducts(config.getAccountToken(), !config.isLocalPlay()).observe(this, products ->{

                    List<Product> productList = new ArrayList<>();
                    if (products.size()>0) {
                        for (com.videostreamtest.config.entity.Product extProd : products) {
                            Product addProd = new Product();
                            addProd.setId(extProd.getUid());
                            addProd.setDefaultSettingsId(0);
                            addProd.setProductLogoButtonPath(extProd.getProductLogoButtonPath());
                            addProd.setSupportStreaming(extProd.getSupportStreaming());
                            addProd.setProductName(extProd.getProductName());
                            addProd.setBlocked(extProd.getBlocked());
                            addProd.setCommunicationType(extProd.getCommunicationType());
                            productList.add(addProd);
                        }
                    }

                    ProductPickerAdapter productPickerAdapter = new ProductPickerAdapter(productList.toArray(new Product[0]));
                    //set adapter to recyclerview
                    productOverview.setAdapter(productPickerAdapter);
                    //set recyclerview visible
                    productOverview.setVisibility(View.VISIBLE);

                    //For UI alignment in center with less then 5 products
                    int spanCount = 5;
                    if (products.size() < 5) {
                        spanCount = products.size();
                    }
                    //Grid Layout met een max 5 kolommen breedte
                    final GridLayoutManager gridLayoutManager = new GridLayoutManager(this,spanCount);
                    //Zet de layoutmanager erin
                    productOverview.setLayoutManager(gridLayoutManager);
                });
            }
        });

    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetScreensaverTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData = true;
        resetScreensaverTimer();
    }

    private void initScreensaverHandler() {
        screensaverRunnable = new Runnable() {
            @Override
            public void run() {
                //Start Screensaver service to show screensaver after predetermined user inactivity
                Intent screensaverActivity = new Intent(getApplicationContext(), ScreensaverActivity.class);
                startActivity(screensaverActivity);
                ApplicationSettings.setScreensaverActive(true);
            }
        };
    }

    private void startScreensaverHandler() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        screensaverLooper = thread.getLooper();
        screensaverhandler = new Handler(screensaverLooper);
        Log.d(TAG, "call postDelayed with delay of "+ApplicationSettings.SCREENSAVER_TRIGGER_SECONDS*1000+" ms");
        screensaverhandler.postDelayed(screensaverRunnable, ApplicationSettings.SCREENSAVER_TRIGGER_SECONDS*1000);
    }

    private void resetScreensaverTimer() {
        Log.d(TAG, "reset check if Screensaver state = "+ApplicationSettings.SCREENSAVER_ACTIVE);
        if (screensaverhandler != null && !ApplicationSettings.SCREENSAVER_ACTIVE) {
            screensaverhandler.removeCallbacksAndMessages(null);
            startScreensaverHandler();
        }
    }
}
