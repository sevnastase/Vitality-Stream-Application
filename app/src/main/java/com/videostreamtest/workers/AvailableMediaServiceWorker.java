package com.videostreamtest.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.ProfileDao;
import com.videostreamtest.config.dao.RoutefilmDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.Profile;
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

public class AvailableMediaServiceWorker extends Worker {

    private static final String TAG = AvailableMediaServiceWorker.class.getSimpleName();

    public AvailableMediaServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public interface PraxCloud {
        @GET("/api/route/movies")
        Call<List<Movie>> getRoutefilms(@Header("api-key") String accountToken);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");
        //Pre-define output
        Data output = new Data.Builder().build();

        //API CALL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);
        Call<List<Movie>> call = praxCloud.getRoutefilms(apikey);
        List<Movie> routefilms = new ArrayList<>();
        try {
            routefilms = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }
        Log.d(TAG, "RouteFilms Count RetroFit :: "+routefilms.size());

        //PREPARE ARRAY OF INTEGERS WITH MOVIE ID'S
        ArrayList<Integer> availableMovieIds = new ArrayList<>();

        //DATABASE INJECTION
        final RoutefilmDao routefilmDao = PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao();
        if (routefilms.size() > 0) {
            for (final Movie routefilm: routefilms) {
                final com.videostreamtest.config.entity.Routefilm dbRoutefilm = new com.videostreamtest.config.entity.Routefilm();
                dbRoutefilm.setAccountToken(apikey);
                dbRoutefilm.setMovieId(routefilm.getId());
                dbRoutefilm.setMinimalSpeed(routefilm.getMinimalSpeed());
                dbRoutefilm.setMovieImagepath(routefilm.getMovieImagepath());
                dbRoutefilm.setMovieLength(routefilm.getMovieLength());
                dbRoutefilm.setMovieRouteinfoPath(routefilm.getMovieRouteinfoPath());
                dbRoutefilm.setMovieTitle(routefilm.getMovieTitle());
                dbRoutefilm.setMovieUrl(routefilm.getMovieUrl());
                dbRoutefilm.setRecordedFps(routefilm.getRecordedFps());
                dbRoutefilm.setRecordedSpeed(routefilm.getRecordedSpeed());
                dbRoutefilm.setMovieFileSize(routefilm.getMovieFileSize());
                dbRoutefilm.setMapFileSize(routefilm.getMapFileSize());
                dbRoutefilm.setSceneryFileSize(routefilm.getSceneryFileSize());

                availableMovieIds.add(routefilm.getId());

                long result = routefilmDao.insert(dbRoutefilm);
            }
            Log.d(TAG, "All routefilms been downloaded");
        }

        //Store outcome in the output data model
        int[] movieIdList = new int[availableMovieIds.size()];
        for (int movieIndex = 0; movieIndex < availableMovieIds.size(); movieIndex++) {
            movieIdList[movieIndex] = availableMovieIds.get(movieIndex).intValue();
        }
        //OUTPUT FOR NEXT WORKER IN CHAIN
        output = new Data.Builder()
                .putString("apikey", apikey)
                .putIntArray("movie-id-list", movieIdList)
                .build();

        //Return result with data output
        return ListenableWorker.Result.success(output);
    }
}
