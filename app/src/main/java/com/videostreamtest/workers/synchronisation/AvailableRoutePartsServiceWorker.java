package com.videostreamtest.workers.synchronisation;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.RoutepartDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

public class AvailableRoutePartsServiceWorker extends Worker {
    private static final String TAG = AvailableRoutePartsServiceWorker.class.getSimpleName();

    public AvailableRoutePartsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");
        final Integer movieId = getInputData().getInt("movieId", 0);
        final int[] movieIdList = getInputData().getIntArray("movie-id-list");

        //Pre-define output
        Data output = new Data.Builder().build();

        //API CALL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

        if (movieIdList != null && movieIdList.length >0 && movieId==0) {
            for (int mId : movieIdList) {
                Call<List<MoviePart>> call = praxCloud.getRoutepartsOfMovieId(mId, apikey);
                List<MoviePart> routeparts = new ArrayList<>();
                try {
                    routeparts = call.execute().body();
                } catch (IOException ioException) {
                    Log.e(TAG, ioException.getLocalizedMessage());
                }

                //DATABASE INJECTION
                final RoutepartDao routepartDao = PraxtourDatabase.getDatabase(getApplicationContext()).routepartDao();

                if (routeparts.size()>0) {
                    for (MoviePart part: routeparts) {
                        Routepart routepart = new Routepart();
                        routepart.setRoutepartId(part.getId());
                        routepart.setMovieId(part.getMovieId());
                        routepart.setMoviePartFrameNumber(part.getFrameNumber());
                        routepart.setMoviePartImagePath(part.getMoviepartImagepath());
                        routepart.setMoviePartName(part.getMoviepartName());
                        routepartDao.insert(routepart);
                    }
                }
            }
        } else {
            Call<List<MoviePart>> call = praxCloud.getRoutepartsOfMovieId(movieId, apikey);
            List<MoviePart> routeparts = new ArrayList<>();
            try {
                routeparts = call.execute().body();
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
            }

            //DATABASE INJECTION
            final RoutepartDao routepartDao = PraxtourDatabase.getDatabase(getApplicationContext()).routepartDao();
            if (routeparts.size() > 0) {
                for (MoviePart part : routeparts) {
                    Routepart routepart = new Routepart();
                    routepart.setRoutepartId(part.getId());
                    routepart.setMovieId(part.getMovieId());
                    routepart.setMoviePartFrameNumber(part.getFrameNumber());
                    routepart.setMoviePartImagePath(part.getMoviepartImagepath());
                    routepart.setMoviePartName(part.getMoviepartName());
                    routepartDao.insert(routepart);
                }
            }
        }

        //TODO: REMOVE OLD API CALL
        //Define which services you need
        final DatabaseRestService databaseRestService = new DatabaseRestService();
        //Execute some actions
        final String result = databaseRestService.getAvailableMovieParts(apikey, movieId);

        //Store outcome in the output data model
        output = new Data.Builder()
                .putString("apikey", apikey)
                .putIntArray("movie-id-list", getInputData().getIntArray("movie-id-list"))
                .putString("movieparts-list", result)
                .build();

        //Return result with data output
        return ListenableWorker.Result.success(output);
    }
}
