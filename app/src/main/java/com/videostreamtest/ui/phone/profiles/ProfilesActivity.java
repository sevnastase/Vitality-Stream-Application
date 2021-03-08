package com.videostreamtest.ui.phone.profiles;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.ProfileServiceWorker;

import java.util.HashMap;
import java.util.Iterator;

public class ProfilesActivity extends AppCompatActivity {
    private static final String TAG = ProfilesActivity.class.getSimpleName();

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_overview);

        Log.d(this.getClass().getSimpleName(), "Density: "+this.getResources().getDisplayMetrics());

        //Enumerate usb devices in console
        enumerateUsbDevices();

        //retrieve API key
        SharedPreferences myPreferences = getApplication().getSharedPreferences("app",0);
        String apiKey = myPreferences.getString("apiKey", "unauthorized");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equalsIgnoreCase("unauthorized")) {
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(loginActivity);
            finish();
        } else {
            switch (ApplicationSettings.SELECTED_COMMUNICATION_DEVICE) {
                case ANT_PLUS:
                    //Write log for stating antplus status and version
                    Log.d("ANTPLUS_SETTINGS", "Version number: "+ AntPluginPcc.getInstalledPluginsVersionNumber(getApplicationContext()));
                    if (AntPlusService.isAntPlusDevicePresent(getApplicationContext())) {
                        checkForSystemApprovalAntPlusService();
                    }
                    break;
                case BLE:
//                    startBleService();
                    break;
                default:
            }

            final Button signoutButton = findViewById(R.id.logout_button);
            signoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        SharedPreferences sp = getApplication().getSharedPreferences("app",0);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.clear();
                        editor.commit();
                        finish();
                }
            });

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

            /**
             * Link Recyclerview en laad profielen
             */

            //Koppel de recyclerView aan de layout xml
            recyclerView = findViewById(R.id.recyclerview_profiles);
            recyclerView.setHasFixedSize(true);
            //Maak lineaire layoutmanager en zet deze op horizontaal
            LinearLayoutManager layoutManager
                    = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            //Grid Layout
//            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4, GridLayoutManager.HORIZONTAL, false);
            //Zet de layoutmanager erin
            recyclerView.setLayoutManager(layoutManager);

            loadProfiles(apiKey);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        final Intent bleService = new Intent(getApplicationContext(), BleService.class);
        stopService(bleService);
    }

    @Override
    protected void onStop() {
        super.onStop();
        final Intent bleService = new Intent(getApplicationContext(), BleService.class);
        stopService(bleService);
    }

    private void enumerateUsbDevices() {
        final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            Log.d(TAG, "USB :: VENDOR_ID : "+ device.getVendorId() +" : PRODUCT_ID : "+device.getProductId()+" : PRODUCT_NAME : " + device.getProductName());
        }
    }

    private void checkForSystemApprovalAntPlusService() {
        Intent antPlusService = new Intent(getApplicationContext(), AntPlusService.class);
        startService(antPlusService);
        Runnable stopAntPlusService = () -> stopService(antPlusService);
        new Handler(Looper.getMainLooper()).postDelayed( stopAntPlusService, 2000 );
    }

    public void startBleService()
    {
        Intent bleService = new Intent(this, BleService.class);
        startService(bleService);
    }

    private void loadProfiles(final String apikey) {
        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);

        OneTimeWorkRequest profilesRequest = new OneTimeWorkRequest.Builder(ProfileServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("profiles")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(profilesRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(profilesRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("profiles");

                        try {
                            final ObjectMapper objectMapper = new ObjectMapper();
                            Profile profiles[] = objectMapper.readValue(result, Profile[].class);
                            //pass profiles to adapter
                            ProfileAdapter profileAdapter = new ProfileAdapter(profiles);
                            //set adapter to recyclerview
                            recyclerView.setAdapter(profileAdapter);
                            //set recyclerview visible
                            recyclerView.setVisibility(View.VISIBLE);
                        } catch (JsonMappingException e) {
                            e.printStackTrace();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
