package com.videostreamtest.workers;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.config.dao.ProfileDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.service.database.DatabaseRestService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

public class ProfileServiceWorker extends Worker {
    private static final String TAG = ProfileServiceWorker.class.getSimpleName();

    public ProfileServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public interface PraxCloud {
        @GET("/api/users/current/profiles")
        Call<List<Profile>> getAccountProfiles(@Header("api-key") String accountToken);
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
        Call<List<Profile>> call = praxCloud.getAccountProfiles(apikey);
        List<Profile> accountProfiles = new ArrayList<>();
        try {
            accountProfiles = call.execute().body();
            Log.d(TAG, "AccountProfiles Count RetroFit :: "+accountProfiles.size());
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }


        final ProfileDao profileDao = PraxtourDatabase.getDatabase(getApplicationContext()).profileDao();
            if (accountProfiles.size() > 0) {
                for (final Profile profile: accountProfiles) {
                    final com.videostreamtest.config.entity.Profile dbProfile = new com.videostreamtest.config.entity.Profile();
                    dbProfile.setAccountToken(apikey);
                    dbProfile.setProfileId(profile.getProfileId());
                    dbProfile.setProfileImgPath(profile.getProfileImgPath());
                    dbProfile.setProfileKey(profile.getProfileKey());
                    dbProfile.setProfileName(profile.getProfileName());
                    dbProfile.setBlocked(profile.getBlocked());

                    long result = profileDao.insert(dbProfile);
                }
                Log.d(TAG, "All've been done");
            }
        return Result.success();
    }
}
