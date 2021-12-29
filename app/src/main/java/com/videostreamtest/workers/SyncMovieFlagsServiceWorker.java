package com.videostreamtest.workers;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncMovieFlagsServiceWorker extends Worker {
    private static final String TAG = SyncMovieFlagsServiceWorker.class.getSimpleName();

    public SyncMovieFlagsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");

        //Pre-define output
        Data output = new Data.Builder().build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);
        Call<List<MovieFlag>> call = praxCloud.getMovieFlags(apikey);
        List<MovieFlag> movieFlagList = new ArrayList<>();

        try {
            movieFlagList = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        if (movieFlagList!=null && movieFlagList.size()>0) {
            Log.d(TAG, String.format("Movie Flag relations counted: %d", movieFlagList.size()));
            for (final MovieFlag movieFlag: movieFlagList) {
                PraxtourDatabase.databaseWriterExecutor.execute(() ->{
                    PraxtourDatabase.getDatabase(getApplicationContext()).movieFlagDao().insert(movieFlag);
                });
            }
        }

        return Result.success();
    }
}
