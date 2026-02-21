package com.videostreamtest.workers.synchronisation;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.gson.GsonBuilder;
import com.videostreamtest.config.dao.ConfigurationDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.data.model.response.Configuration;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.workers.AbstractPraxtourWorker;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActiveConfigurationServiceWorker extends AbstractPraxtourWorker {
    private static final String TAG = ActiveConfigurationServiceWorker.class.getSimpleName();

    public ActiveConfigurationServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    protected Result doActualWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
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
        Data output = new Data.Builder().build();

        //ActiveProducts retrieval
        /**
         * Return active products count
         */

        Call<List<Product>> allAccountProducts = praxCloud.getActiveProducts(apikey);
        List<Product> activeProducts = new ArrayList<>();
        try {
            activeProducts = allAccountProducts.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }
        int activeProductsCount = 0;
        if (activeProducts != null) {
            activeProductsCount = activeProducts.size();
        }

        if (accountConfiguration!= null) {
            configurationDao.updateCurrentConfiguration(
                    accountConfiguration.isLocalPlay(),
                    accountConfiguration.isBootOnStart(),
                    accountConfiguration.getCommunicationDevice(),
                    accountConfiguration.isUpdatePraxCloud(),
                    accountConfiguration.getPraxCloudMediaServerUrl(),
                    accountConfiguration.getPraxCloudMediaServerLocalUrl(),
                    accountConfiguration.getAccountType());

            //Pre-define output
            output = new Data.Builder()
                    .putString("apikey", apikey)
                    .putInt("active-products-count", activeProductsCount)
                    .putBoolean("isStreamingAccount", !accountConfiguration.isLocalPlay())
                    .putString("configurationObject", new GsonBuilder().create().toJson(accountConfiguration, Configuration.class))
                    .build();
        } else {
            output = new Data.Builder()
                    .putString("apikey", apikey)
                    .putInt("active-products-count", activeProductsCount)
                    .build();
        }

        return Result.success(output);
    }
}
