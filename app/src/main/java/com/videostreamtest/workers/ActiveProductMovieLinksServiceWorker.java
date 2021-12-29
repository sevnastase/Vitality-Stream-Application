package com.videostreamtest.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.ProductDao;
import com.videostreamtest.config.dao.ProductMovieDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.ProductMovie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.data.model.response.ProductMovieRecord;
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

public class ActiveProductMovieLinksServiceWorker extends Worker {
    private static final String TAG = ActiveProductMovieLinksServiceWorker.class.getSimpleName();

    public ActiveProductMovieLinksServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
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
        Call<List<ProductMovieRecord>> call = praxCloud.getAllProductMovies(apikey);
        List<ProductMovieRecord> productMovieList = new ArrayList<>();
        try {
            productMovieList = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        final ProductMovieDao productMovieDao = PraxtourDatabase.getDatabase(getApplicationContext()).productMovieDao();
        if (productMovieList.size()>0) {
            for (ProductMovieRecord productMovieRecord: productMovieList) {
                ProductMovie productMovie = new ProductMovie();
                productMovie.setPmId(productMovieRecord.getRecordId());
                productMovie.setMovieId(productMovieRecord.getMovieId());
                productMovie.setProductId(productMovieRecord.getProductId());
                productMovieDao.insert(productMovie);
            }
        }

        return Result.success();
    }
}
