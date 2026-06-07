package com.videostreamtest.workers.synchronisation;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.MovieFlagDao;
import com.videostreamtest.config.dao.MovieLocalInfoDao;
import com.videostreamtest.config.dao.RoutefilmDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.config.entity.MovieLocalInfo;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.AbstractPraxtourWorker;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncLocalDatabaseWorker extends AbstractPraxtourWorker {
    private static final String TAG = SyncLocalDatabaseWorker.class.getSimpleName();

    public SyncLocalDatabaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    protected Result doActualWork() {
        //Get Input
        Data inputData = getInputData();

        final String apikey = inputData.getString("apikey");
        final RoutefilmDao routefilmDao;
        final MovieLocalInfoDao movieLocalInfoDao;

        try {
            routefilmDao = PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao();
            movieLocalInfoDao = PraxtourDatabase.getDatabase(getApplicationContext()).movieLocalInfoDao();
        } catch (NullPointerException e) {
            return Result.failure();
        }

        final List<Routefilm> routefilms = routefilmDao.getLocalRoutefilms(apikey);
        final File largestStorageVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());

        for (Routefilm routefilm : routefilms) {
            Movie movie = Movie.fromRoutefilm(routefilm);
            MovieLocalInfo info = new MovieLocalInfo();

            info.setMovieId(movie.getId());
            info.setMovieSceneryPath(
                    largestStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER +
                            "/" + movie.getId() +
                            "/" + new File(movie.getMovieImagepath()).getName()
            );
            info.setMovieMapPath(
                    largestStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER +
                            "/" + movie.getId() +
                            "/" + new File(movie.getMovieRouteinfoPath()).getName()
            );
            if (movie.getMovieFlagUrl() != null && !movie.getMovieFlagUrl().isBlank()) {
                info.setMovieFlagPath(
                        largestStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER +
                                "/" + new File(movie.getMovieFlagUrl()).getName()
                );
            }

            movieLocalInfoDao.insert(info);
        }

        return Result.success();
    }
}
