package com.videostreamtest.workers;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.data.model.response.Configuration;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AccountServiceWorker extends Worker {
    private static final String TAG = AccountServiceWorker.class.getSimpleName();
    public AccountServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences myPreferences = getApplicationContext().getSharedPreferences("app",0);
        final String apikey = myPreferences.getString("apikey", "");

        if (apikey == null || apikey.isEmpty()) {
            return Result.failure();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

        Call<Configuration> accountConfigurationCall = praxCloud.getAccountConfiguration(apikey);
        Configuration accountConfiguration;
        try {
            accountConfiguration = accountConfigurationCall.execute().body();
        } catch (IOException ioException) {
            return Result.failure();
        }

        if (accountConfiguration == null) {
            return Result.failure();
        }

        String oldAccountType = myPreferences.getString("account-type", "");
        String newAccountType = accountConfiguration.getAccountType();
        if (newAccountType.equalsIgnoreCase(oldAccountType)) {
            Data output = new Data.Builder()
                    .putBoolean("accountTypeChanged", false)
                    .build();
            Result.success(output);
        } else {
            SharedPreferences.Editor editor = myPreferences.edit();
            editor.putString("account-type", newAccountType);
            Data output = new Data.Builder()
                    .putBoolean("accountTypeChanged", true)
                    .build();
            Result.success(output);
        }

        return Result.failure();
    }
}
