package com.videostreamtest.ui.phone.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.videostreamtest.enums.ProductType;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.profiles.ProfilesActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences appPreferences = getApplication().getSharedPreferences("app",0);
        final String apiKey = appPreferences.getString("apiKey" ,null);

        /*
        TODO: when API key is available, check is account is valid and still valid, else throw them to login.

         */

        if (apiKey != null) {
            /*
            TODO: Retrieve products of the customer if not already in sharedPreferences or room database
            - Based on the number of products a productpicker will appear or not
            - Based on one product the first page of the product will appear with switch statement
             */
            ProductType productType = ProductType.PRAXFIT_STREAM;
            switch(productType) {
                case PRAXFIT_STREAM:
                    startActivity(new Intent(SplashActivity.this, ProfilesActivity.class));
                    break;
                case PRAXFIT_LOCAL:
                    break;
                case PRAXFILM_LOCAL:
                    break;
                case PRAXFILM_STREAM:
                    break;
                case PRAXSPIN_LOCAL:
                    break;
                case PRAXSPIN_STREAM:
                    break;
                default:

            }
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        //Close this activity and release resources
        finish();
    }
}
