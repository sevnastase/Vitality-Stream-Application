package com.videostreamtest.ui.phone.profiles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Result;
import com.videostreamtest.workers.AddProfileServiceWorker;

public class AddProfileActivity extends AppCompatActivity {
    private final static String TAG = AddProfileActivity.class.getSimpleName();

    private ProfileViewModel profileViewModel;

    private ImageButton avatarMaleView;
    private ImageButton avatarFemaleView;
    private EditText profileName;

    private float alphaDefaultValue = 0.6f;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_profile);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
    }

    private boolean isFormValid() {
        if (avatarFemaleView.getAlpha() == 1.0f || avatarMaleView.getAlpha() == 1.0f) {
            if (profileName.getText() != null && !profileName.getText().toString().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void addProfileToAccount() {
        /*
        Add serviceworker onetime request
        response successful then proceed to ProfileActivity
         */
        SharedPreferences myPreferences = getApplication().getSharedPreferences("app",0);
        String apikey = myPreferences.getString("apiKey", "unauthorized");

        String imgpath = "";
        if (avatarMaleView.getAlpha() == 1.0f) {
            imgpath = "http://188.166.100.139:8080/api/dist/img/avatar5.png";
        }
        if (avatarFemaleView.getAlpha() == 1.0f) {
            imgpath = "http://188.166.100.139:8080/api/dist/img/avatar2.png";
        }

        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);
        networkData.putString("profilename", profileName.getText().toString());
        networkData.putString("imgpath", imgpath);

        OneTimeWorkRequest addProfileRequest = new OneTimeWorkRequest.Builder(AddProfileServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("addprofile")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(addProfileRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(addProfileRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("add-profile-result");

                        try {
                            final ObjectMapper objectMapper = new ObjectMapper();
                            Result responseResult = objectMapper.readValue(result, Result.class);
                            //Check if there are profiles else show add profile form
                            if (responseResult.getStatusCode() == 200) {
                                startActivity(new Intent(this, ProfilesActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this.getApplicationContext(), getString(R.string.profile_add_form_error),Toast.LENGTH_LONG).show();
                            }
                        } catch (JsonMappingException jsonMappingException) {
                            Log.e(TAG, jsonMappingException.getLocalizedMessage());
                        } catch (JsonProcessingException jsonProcessingException) {
                            Log.e(TAG, jsonProcessingException.getLocalizedMessage());
                        }
                    }
                });
    }

}
