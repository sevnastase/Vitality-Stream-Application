package com.videostreamtest.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.ConfigurationDao;
import com.videostreamtest.config.dao.ProductDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

public class ActiveProductsServiceWorker extends Worker {
    private static final String TAG = ActiveProductsServiceWorker.class.getSimpleName();

    public ActiveProductsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");
        //Pre-define output
        Data output = new Data.Builder().build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);
        Call<List<Product>> call = praxCloud.getActiveProducts(apikey);
        List<Product> activeProducts = new ArrayList<>();
        try {
            activeProducts = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }
        Log.d(TAG, "ActiveProducts Count RetroFit :: "+activeProducts.size());

        //Update local room database with the newest products based on cloud configuration
        final ProductDao productDao = PraxtourDatabase.getDatabase(getApplicationContext()).productDao();
        //Clear the products before adding the right dataset
        productDao.nukeTable();
        if (activeProducts.size() > 0) {
            for (final Product externalProduct : activeProducts) {
                final com.videostreamtest.config.entity.Product tmpProduct = new com.videostreamtest.config.entity.Product();
                tmpProduct.setUid(externalProduct.getId());
                tmpProduct.setAccountToken(apikey);
                tmpProduct.setDefaultSettingsId(externalProduct.getDefaultSettingsId());
                tmpProduct.setSupportStreaming(externalProduct.getSupportStreaming());
                tmpProduct.setProductName(externalProduct.getProductName());
                tmpProduct.setProductLogoButtonPath(externalProduct.getProductLogoButtonPath());
                tmpProduct.setBlocked(externalProduct.getBlocked());
                tmpProduct.setCommunicationType(externalProduct.getCommunicationType());

                productDao.insert(tmpProduct);
            }
        }

        //Update curent config with product count
        final ConfigurationDao configurationDao = PraxtourDatabase.getDatabase(getApplicationContext()).configurationDao();
        configurationDao.updateProductCountCurrentConfiguration(activeProducts.size());

        return ListenableWorker.Result.success();
    }
}
