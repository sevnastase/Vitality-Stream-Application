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
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncFlagsServiceWorker extends Worker {
    private static final String TAG = SyncFlagsServiceWorker.class.getSimpleName();

    public SyncFlagsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
        Call<List<Flag>> call = praxCloud.getFlags(apikey);
        List<Flag> flagList = new ArrayList<>();

        try {
            flagList = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        if (flagList!=null && flagList.size()>0) {
            Log.d(TAG, String.format("Flags counted: %d", flagList.size()));
            for (final Flag flag: flagList) {
                Log.d(TAG, String.format("Flag item contents: %d, %s, %s, %d, %s", flag.getId(), flag.getFlagCountryIso(), flag.getFlagCountryName(), flag.getFlagFilesize(), flag.getFlagUrl()));
                PraxtourDatabase.databaseWriterExecutor.execute(() ->{
                    PraxtourDatabase.getDatabase(getApplicationContext()).flagDao().insert(flag);
                });
            }
        }

        return Result.success();
    }
}
