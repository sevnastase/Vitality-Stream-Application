package com.videostreamtest.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.RoutefilmDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

public class SyncRoutefilmsServiceWorker extends Worker {

    private static final String TAG = SyncRoutefilmsServiceWorker.class.getSimpleName();

    private List<Routefilm> appRoutefilms = null;
    private List<Movie> cloudRoutefilms = null;

    public SyncRoutefilmsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
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
        cloudRoutefilms = new ArrayList<>();
        try {
            cloudRoutefilms = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }
        Log.d(TAG, "RouteFilms Count RetroFit :: "+cloudRoutefilms.size());

        //PREPARE ARRAY OF INTEGERS WITH MOVIE ID'S
        ArrayList<Integer> availableMovieIds = new ArrayList<>();

        //DATABASE INJECTION
        final RoutefilmDao routefilmDao = PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao();
        appRoutefilms = routefilmDao.getLocalRoutefilms(apikey);
        //CHECK: Fill in the first attempt, let the rest be synchronised by the UpdateRegisteredMovieServiceWorker
//        routefilmDao.nukeTable();
        if (cloudRoutefilms != null && cloudRoutefilms.size() > 0) {
            for (final Movie routefilm: cloudRoutefilms) {
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

                if (isRoutefilmInApp(dbRoutefilm)) {
                    updateRoutefilm(dbRoutefilm);
                }

                if (!isRoutefilmInApp(dbRoutefilm)) {
                    long result = routefilmDao.insert(dbRoutefilm);
                }
                if (!isRoutefilmInCloud(dbRoutefilm)) {
                    routefilmDao.delete(dbRoutefilm);
                }
            }
            Log.d(TAG, "All routefilms been synchronized");
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

    private boolean updateRoutefilm(final Routefilm routefilm) {
        if (appRoutefilms != null && appRoutefilms.size()>0) {
            for (final Routefilm appFilm : appRoutefilms) {
                if (routefilm.getMovieId().intValue() == appFilm.getMovieId().intValue()) {
                    if (routefilm.getMinimalSpeed() != appFilm.getMinimalSpeed() ||
                    routefilm.getMapFileSize() != appFilm.getMapFileSize() ||
                    routefilm.getMovieRouteinfoPath() != appFilm.getMovieRouteinfoPath() ||
                    routefilm.getMovieImagepath() != appFilm.getMovieImagepath() ||
                    routefilm.getMovieUrl() != appFilm.getMovieUrl() ||
                    routefilm.getMovieFileSize() != appFilm.getMovieFileSize() ||
                    routefilm.getMovieTitle() != appFilm.getMovieTitle() ||
                    routefilm.getMovieLength() != appFilm.getMovieLength() ||
                    routefilm.getRecordedFps() != appFilm.getRecordedFps() ||
                    routefilm.getRecordedSpeed() != appFilm.getRecordedSpeed() ||
                    routefilm.getSceneryFileSize() != appFilm.getSceneryFileSize()) {
                        final RoutefilmDao routefilmDao = PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao();
                        routefilmDao.insert(routefilm);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isRoutefilmInApp(final Routefilm routefilm) {
        if (appRoutefilms != null && appRoutefilms.size()>0) {
            for (final Routefilm appFilm: appRoutefilms) {
                if (routefilm.getMovieId().intValue() == appFilm.getMovieId().intValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRoutefilmInCloud(final Routefilm routefilm) {
        if (cloudRoutefilms != null && cloudRoutefilms.size()>0) {
            for (final Movie film: cloudRoutefilms) {
                if (routefilm.getMovieId().intValue() == film.getId().intValue()) {
                    return true;
                }
            }
        }
        return false;
    }
}
