package com.videostreamtest.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.RoutefilmDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

public class UpdateRegisteredMovieServiceWorker extends Worker {

    private final static String TAG = UpdatePackageServiceWorker.class.getSimpleName();

    public UpdateRegisteredMovieServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public interface PraxCloud {
        @GET("/api/route/movies")
        Call<List<Movie>> getRoutefilms(@Header("api-key") String accountToken);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");

        //API CALL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);
        Call<List<Movie>> call = praxCloud.getRoutefilms(apikey);
        List<Movie> externalRoutefilms = new ArrayList<>();

        //GET ROUTEFILMS
        try {
            externalRoutefilms = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        if (externalRoutefilms.size()>0) {
            //LOCAL DATABASE OBJECT
            final RoutefilmDao routefilmDao = PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao();
            final List<Routefilm> localRoutefilms = routefilmDao.getLocalRoutefilms(apikey);

            final List<Routefilm> markedForRemoval = new ArrayList<>();
            final List<Routefilm> markedForInsertion = new ArrayList<>();

            Log.d(TAG, "localRoutefilms counted: "+localRoutefilms.size());

            //If local database is filled then check if external movie is already present
            if (localRoutefilms.size() > 0) {
                //Check if external movie is already present or mark movie for insertion
                for (final Movie externalMovie: externalRoutefilms) {
                    if (!movieAlreadyPresentInLocalDatabase(externalMovie, localRoutefilms)) {
                        markedForInsertion.add(Routefilm.fromMovie(externalMovie, apikey));
                    }
                }
                //Check if local movie is not present in external list and mark for removal
                for (final Routefilm localFilm : localRoutefilms) {
                    if (isMovieMarkedForRemovalInLocalDatabase(localFilm, externalRoutefilms)) {
                        markedForRemoval.add(localFilm);
                    }
                }

                //Execute removals
                if (markedForRemoval.size()>0) {
                    Log.d(TAG, "localRoutefilms marked for removal: "+markedForRemoval.size());
                    for (final Routefilm routefilm:markedForRemoval) {
                        routefilmDao.delete(routefilm);
                    }
                }

                //Execute insertions
                if (markedForInsertion.size()>0) {
                    Log.d(TAG, "localRoutefilms marked for insertion: "+markedForInsertion.size());
                    for (final Routefilm routefilm:markedForInsertion) {
                        routefilmDao.insert(routefilm);
                    }
                }
            }
        }

        /*
        TODO:
         1. get linked movies from current logged in account with praxcloud
         2. crossreference the internet linked movies with local db linked movies
         3.
            a. If there are movies in local database but not in praxcloud => remove from local db
            b. If there are movies in external database but not in local db => add to local db (dont forget downloading when standalone, isLocalPlay)
            c. Notify recyclerview adapter of recent change if any, do nothing when nothing has changed
         */
        return Result.success();
    }

    private boolean movieAlreadyPresentInLocalDatabase(final Movie externalMovie, final List<Routefilm> localMovieList) {
        if (localMovieList.size()>0) {
            for (final Routefilm localFilm : localMovieList) {
                if (externalMovie.getId() == localFilm.getMovieId()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMovieMarkedForRemovalInLocalDatabase(final Routefilm localRoutefilm, final List<Movie> externalMovieList) {
        if (externalMovieList.size()>0) {
            for (final Movie externalMovie:externalMovieList) {
                if (localRoutefilm.getMovieId()==externalMovie.getId()) {
                    return false;
                }
            }
        }

        return true;
    }
}
