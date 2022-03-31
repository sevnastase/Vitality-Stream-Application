package com.videostreamtest.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.EffectSound;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.SoundItem;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

public class SoundInformationServiceWorker extends Worker {
    private static final String TAG = SoundInformationServiceWorker.class.getSimpleName();

    private List<SoundItem> soundItems = new ArrayList<>();

    public SoundInformationServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

        //Get LIST OF linked MOVIES
        Call<List<Movie>> movieListCall = praxCloud.getRoutefilms(apikey);
        List<Movie> routefilms = new ArrayList<>();
        try {
            routefilms = movieListCall.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        //Get the active sound items available
        Call<List<SoundItem>> callSounds = praxCloud.getSounds(apikey);
        try {
            soundItems = callSounds.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        if (routefilms != null && routefilms.size()>0) {
            for (final Movie routefilm: routefilms) {
                //Update background sounds
                Call<List<BackgroundSound>> callBackgroundSounds = praxCloud.getBackgroundSounds(routefilm.getId().intValue(), apikey);
                List<BackgroundSound> backgroundSounds = new ArrayList<>();
                try {
                    backgroundSounds = callBackgroundSounds.execute().body();
                } catch (IOException ioException) {
                    Log.e(TAG, ioException.getLocalizedMessage());
                }

                registerBackgroundSounds(backgroundSounds, routefilm.getId().intValue());

                //Update effect sounds
                Call<List<EffectSound>> callEffectSounds = praxCloud.getEffectSounds(routefilm.getId().intValue(), apikey);
                List<EffectSound> effectSounds = new ArrayList<>();
                try {
                    effectSounds = callEffectSounds.execute().body();
                } catch (IOException ioException) {
                    Log.e(TAG, ioException.getLocalizedMessage());
                }

                registerEffectSounds(effectSounds, routefilm.getId().intValue());
            }
        }

        return Result.success();
    }

    private void registerBackgroundSounds(List<BackgroundSound> backgroundSounds, final Integer movieId){
        if (backgroundSounds.size()>0) {
            for (BackgroundSound backgroundSound : backgroundSounds) {
                if (backgroundSound.getSoundNumber() == null) {
                    backgroundSound.setSoundNumber(backgroundSound.getSoundId());
                }
                if (backgroundSound.getBgSoundId() == null) {
                    backgroundSound.setBgSoundId(backgroundSound.getFramenumber());
                }

                backgroundSound.setMovieId(movieId);
                backgroundSound.setSoundUrl(findSoundItem(backgroundSound.getSoundId()).getSoundUrl());
                PraxtourDatabase.getDatabase(getApplicationContext()).backgroundSoundDao().insert(backgroundSound);
            }
        }
    }

    private void registerEffectSounds(List<EffectSound> effectSounds, final Integer movieId){
        if (effectSounds.size()>0) {
            for (EffectSound effectSound: effectSounds) {
                if (effectSound.getSoundNumber() == null) {
                    effectSound.setSoundNumber(effectSound.getSoundId());
                }
                if (effectSound.getEfSoundId() == null) {
                    effectSound.setEfSoundId(effectSound.getFramenumber());
                }

                effectSound.setMovieId(movieId);
                effectSound.setSoundUrl(findSoundItem(effectSound.getSoundId()).getSoundUrl());
                PraxtourDatabase.getDatabase(getApplicationContext()).effectSoundDao().insert(effectSound);
            }
        }
    }

    private SoundItem findSoundItem(final Integer soundNumber) {
        if (soundItems.size()>0) {
            for (SoundItem soundItem: soundItems) {
                if (soundItem.getSoundNumber()==soundNumber) {
                    return soundItem;
                }
            }
        }
        return new SoundItem();
    }

}
