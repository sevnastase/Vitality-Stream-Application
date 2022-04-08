package com.videostreamtest.ui.phone.profiles;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.ProfileServiceWorker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ProfilesActivity extends AppCompatActivity {
    private static final String TAG = ProfilesActivity.class.getSimpleName();
    private ProfileViewModel profileViewModel;

    private static ProfilesActivity thisInstance;

    private String apiKey;
    private Button signoutButton;
    private RecyclerView recyclerView;

    public static ProfilesActivity getInstance() {
        return thisInstance;
    }

    public void notifyDataSet() {
        if (recyclerView.getAdapter() != null) {
            loadProfiles(apiKey);
            recyclerView.getAdapter().notifyDataSetChanged();
        } else {
            Log.d(TAG, "No adapter found!");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisInstance = this;
        setContentView(R.layout.activity_profile_overview);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        signoutButton = findViewById(R.id.logout_button);

        profileViewModel.getCurrentConfig().observe(this, config -> {
            if (config != null) {
                Log.d(TAG, "ProfileActivity Loaded ");
                Log.d(TAG, "ProfileActivity >> "+config.getProductCount() + " Products Found");
                signoutButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Old
                        SharedPreferences sp = getApplication().getSharedPreferences("app",0);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.clear();
                        editor.commit();

                        //New
                        if (config.getProductCount() == 1) {
                            profileViewModel.signoutCurrentAccount();
                        }
                        ProfilesActivity.this.finish();
                    }
                });

                if (isAccountTokenValid(config.getAccountToken()) && config.isCurrent()) {
                    Log.d(TAG, "currentConfig CommunicationDevice = "+config.getCommunicationDevice());
                    //TODO: load device based on config.getCommunicationDevice()
                    loadCommunicationDeviceDrivers(ApplicationSettings.SELECTED_COMMUNICATION_DEVICE);

                    /**
                     * Link Recyclerview en laad profielen
                     */

                    //Koppel de recyclerView aan de layout xml
                    recyclerView = findViewById(R.id.recyclerview_profiles);
//                    recyclerView.setHasFixedSize(true);

                    profileViewModel.getAccountProfiles().observe(this, profiles -> {
                        Log.d(TAG, "ProfileActivity local profile count: "+profiles.size());
                        loadLocalAccountProfiles(profiles, config.getAccountToken());

                    });
                } else {
                    Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(loginActivity);
                    ProfilesActivity.this.finish();
                }
            }
        });

        Log.d(this.getClass().getSimpleName(), "Density: "+this.getResources().getDisplayMetrics());

        //Enumerate usb devices in console
        enumerateUsbDevices();

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
//        Intent antPlusService = new Intent(getApplicationContext(), AntPlusService.class);
//        startService(antPlusService);
//        Runnable stopAntPlusService = () -> stopService(antPlusService);
//        new Handler(Looper.getMainLooper()).postDelayed( stopAntPlusService, 2000 );
    }

    private void loadCommunicationDeviceDrivers(final CommunicationDevice communicationDevice) {
//        switch (communicationDevice) {
//            case ANT_PLUS:
//                //Write log for stating antplus status and version
//                Log.d("ANTPLUS_SETTINGS", "Version number: "+ AntPluginPcc.getInstalledPluginsVersionNumber(getApplicationContext()));
//                if (AntPlusService.isAntPlusDevicePresent(getApplicationContext())) {
//                    checkForSystemApprovalAntPlusService();
//                }
//                break;
//            default:
//        }
    }

    private boolean isAccountTokenValid(final String accountToken) {
        return (accountToken != null || !accountToken.isEmpty() || !accountToken.equalsIgnoreCase("unauthorized"));
    }

    //Load through room database
    private void loadProfiles(final String apikey) {
        Log.d(TAG, "Sharedpreferences ApiKey :: "+apikey);
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



                            //Check if there are profiles else show add profile form
                            if (profiles.length <1) {
                                startActivity(new Intent(this, AddProfileActivity.class));
                                finish();
                            }

                            //Add profile button
                            Profile addprofile = new Profile();
                            addprofile.setProfileName("Add profile");
                            addprofile.setProfileImgPath("http://188.166.100.139:8080/api/dist/img/buttons/add_profile_button.png");
                            addprofile.setProfileKey("new-profile");

                            profiles = Arrays.copyOf(profiles, profiles.length+1);
                            profiles[profiles.length -1] = addprofile;

//                            pass profiles to adapter
                            ProfileAdapter profileAdapter = new ProfileAdapter(profiles);
                            //set adapter to recyclerview
                            recyclerView.setAdapter(profileAdapter);
//                            set recyclerview visible
                            recyclerView.setVisibility(View.VISIBLE);

                            //For UI alignment in center with less then 5 products
                            int spanCount = 5;
                            if (profiles.length < 5) {
                                spanCount = profiles.length;
                            }
                            //Grid Layout met een max 5 kolommen breedte
                            final GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
                            recyclerView.setLayoutManager(gridLayoutManager);
                        } catch (JsonMappingException jsonMappingException) {
                            Log.e(TAG, jsonMappingException.getLocalizedMessage());
                        } catch (JsonProcessingException jsonProcessingException) {
                            Log.e(TAG, jsonProcessingException.getLocalizedMessage());
                        }
                    }
                });
    }

    private void loadLocalAccountProfiles(final List<com.videostreamtest.config.entity.Profile> accountProfiles, final String accountToken) {

        //Check if there are profiles else show add profile form
        if (accountProfiles.size() < 1) {
            startActivity(new Intent(this, AddProfileActivity.class));
            finish();
        }

        List<Profile> profiles = new ArrayList<>();
        //map Entity objects to response models without uid's
        for (com.videostreamtest.config.entity.Profile profile : accountProfiles) {
            if (!accountToken.equals(profile.getAccountToken())) {
                continue;
            }
            Profile newProfile = new Profile();
            newProfile.setProfileKey(profile.getProfileKey());
            newProfile.setProfileImgPath(profile.getProfileImgPath());
            newProfile.setProfileName(profile.getProfileName());
            newProfile.setBlocked(profile.getBlocked());
            newProfile.setProfileId(profile.getProfileId());
            profiles.add(newProfile);
        }

        //Add profile button
        Profile addprofile = new Profile();
        addprofile.setProfileName("Add profile");
        addprofile.setProfileImgPath("http://188.166.100.139:8080/api/dist/img/buttons/add_profile_button.png");
        addprofile.setProfileKey("new-profile");

        //profiles = Arrays.copyOf(profiles, profiles.size()+1);

        profiles.add(addprofile);

        Log.d(TAG, "LOCAL Profile count :: "+profiles.size());
        //pass profiles to adapter
        ProfileAdapter profileAdapter = new ProfileAdapter(profiles.toArray(new Profile[0]));
        //set adapter to recyclerview
        recyclerView.setAdapter(profileAdapter);
        //set recyclerview visible
        recyclerView.setVisibility(View.VISIBLE);

        //For UI alignment in center with less then 5 products
        int spanCount = 5;
        if (profiles.size() < 5) {
            spanCount = profiles.size();
        }
        //Grid Layout met een max 5 kolommen breedte
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
    }
}
