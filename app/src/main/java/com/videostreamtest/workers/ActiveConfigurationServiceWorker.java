package com.videostreamtest.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.GsonBuilder;
import com.videostreamtest.config.dao.ConfigurationDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.data.model.response.Configuration;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

public class ActiveConfigurationServiceWorker extends Worker {
    private static final String TAG = ActiveConfigurationServiceWorker.class.getSimpleName();

    public ActiveConfigurationServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public interface PraxCloud {
        @GET("/api/users/current/configuration")
        Call<Configuration> getAccountConfiguration(@Header("api-key") String accountToken);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);
        Call<Configuration> call = praxCloud.getAccountConfiguration(apikey);
        Configuration accountConfiguration = new Configuration();
        try {
            accountConfiguration = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
            return Result.failure();
        }

        final ConfigurationDao configurationDao = PraxtourDatabase.getDatabase(getApplicationContext()).configurationDao();
        configurationDao.updateCurrentConfiguration(
                accountConfiguration.isLocalPlay(),
                accountConfiguration.isBootOnStart(),
                accountConfiguration.getCommunicationDevice(),
                accountConfiguration.isUpdatePraxCloud(),
                accountConfiguration.getPraxCloudMediaServerUrl(),
                accountConfiguration.getPraxCloudMediaServerLocalUrl());

        //Pre-define output
        Data output = new Data.Builder()
                .putString("apikey", apikey)
                .putBoolean("isStreamingAccount", !accountConfiguration.isLocalPlay())
                .putString("configurationObject", new GsonBuilder().create().toJson(accountConfiguration, Configuration.class))
                .build();

        return Result.success(output);
    }
}
