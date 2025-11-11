package com.videostreamtest.workers;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.RoutepartDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UpdateRoutePartsServiceWorker extends AbstractPraxtourWorker {
    private static final String TAG = UpdateRoutePartsServiceWorker.class.getSimpleName();

    private PraxCloud praxCloud;

    public UpdateRoutePartsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    protected Result doActualWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");

        //Pre-define output
        Data output = new Data.Builder().build();

        //API CALL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        praxCloud = retrofit.create(PraxCloud.class);

        //RETRIEVE LIST OF MOVIES
        Call<List<Movie>> movieListCall = praxCloud.getRoutefilms(apikey);
        List<Movie> routefilms = new ArrayList<>();
        try {
            routefilms = movieListCall.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        if (routefilms != null && routefilms.size()>0) {
            for (final Movie movie: routefilms) {
                insertRoutepart(movie.getId(), apikey);
            }
        }

        //Store outcome in the output data model
        output = new Data.Builder()
                .putString("apikey", apikey)
                .build();

        //Return result with data output
        return Result.success(output);
    }

    private void insertRoutepart( final Integer movieId, final String apikey) {
        Call<List<MoviePart>> call = praxCloud.getRoutepartsOfMovieId(movieId, apikey);
        List<MoviePart> routeparts = new ArrayList<>();
        try {
            routeparts = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        //DATABASE INJECTION
        final RoutepartDao routepartDao = PraxtourDatabase.getDatabase(getApplicationContext()).routepartDao();
        if (routeparts.size()>0) {
            routepartDao.deleteMoviepartsOfMovie(movieId);
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
}
