package com.videostreamtest.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.DownloadStatusDao;
import com.videostreamtest.config.dao.RoutefilmDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.webinterface.PraxCloud;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

public class UpdateRegisteredMovieServiceWorker extends Worker {

    private final static String TAG = UpdatePackageServiceWorker.class.getSimpleName();
    private DatabaseRestService databaseRestService;
    private String accountToken;

    public UpdateRegisteredMovieServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        //Get Input
        final String apikey = getInputData().getString("apikey");
        databaseRestService = new DatabaseRestService();

        accountToken = AccountHelper.getAccountToken(getApplicationContext());

        //API CALL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);
        Call<List<Movie>> callExternalRoutefilms = praxCloud.getRoutefilms(accountToken);
        Call<List<MovieFlag>> callExternalMovieFlagLinks = praxCloud.getMovieFlags(accountToken);
        Call<List<Flag>> callExternalFlags = praxCloud.getFlags(accountToken);

        List<Movie> externalRoutefilms = new ArrayList<>();
        List<MovieFlag> externalMovieFlagLinksList = new ArrayList<>();
        List<Flag> externalFlags = new ArrayList<>();

        //GET ROUTEFILMS
        try {
            externalRoutefilms = callExternalRoutefilms.execute().body();
            externalMovieFlagLinksList = callExternalMovieFlagLinks.execute().body();
            externalFlags = callExternalFlags.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        //Routefilm objects sanitized with flagUrl
        List<Routefilm> externalRoutefilmListWithDetails = getRoutefilmWithDetails(externalRoutefilms, externalFlags, externalMovieFlagLinksList);

        if (externalRoutefilmListWithDetails.size()>0) {
            //LOCAL DATABASE OBJECT
            final RoutefilmDao routefilmDao = PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao();
            final DownloadStatusDao downloadStatusDao = PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao();
            final List<Routefilm> localRoutefilms = routefilmDao.getLocalRoutefilms(apikey);

            final List<Routefilm> markedForRemoval = new ArrayList<>();
            final List<Routefilm> markedForInsertion = new ArrayList<>();
            final List<Routefilm> markedForUpdate = new ArrayList<>();

            Log.d(TAG, "localRoutefilms counted: "+localRoutefilms.size());

            //If local database is filled then check if external movie is already present
            if (localRoutefilms.size() > 0) {
                //Check if external movie is already present or mark movie for insertion
                for (final Routefilm externalRoutefilm: externalRoutefilmListWithDetails) {
                    if (!movieAlreadyPresentInLocalDatabase(Movie.fromRoutefilm(externalRoutefilm), localRoutefilms)) {
                        markedForInsertion.add(externalRoutefilm);
                    } else {
                        if (hasUpdate(externalRoutefilm, localRoutefilms)) {
                            markedForUpdate.add(externalRoutefilm);
                        }
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
                        deletePhysicalMovie(Movie.fromRoutefilm(routefilm));
                        routefilmDao.delete(routefilm);
                        downloadStatusDao.deleteDownloadStatus(routefilm.getMovieId());
                    }
                }

                //Execute insertions
                if (markedForInsertion.size()>0) {
                    Log.d(TAG, "localRoutefilms marked for insertion: "+markedForInsertion.size());
                    for (final Routefilm routefilm:markedForInsertion) {
                        routefilmDao.insert(routefilm);
                        final StandAloneDownloadStatus standAloneDownloadStatus = new StandAloneDownloadStatus();
                        standAloneDownloadStatus.setMovieId(routefilm.getMovieId());
                        standAloneDownloadStatus.setDownloadMovieId(routefilm.getMovieId());
                        standAloneDownloadStatus.setDownloadStatus(-1);
                        downloadStatusDao.insert(standAloneDownloadStatus);
                    }
                }
                //MarkforUpdate, problem is the -1 downloadstatus for standalone which cause all movie to re-download on update
                if (markedForUpdate.size()>0) {
                    Log.d(TAG, "localRoutefilms marked for update: " + markedForUpdate.size());
                    for (final Routefilm routefilm : markedForUpdate) {
                        routefilmDao.insert(routefilm);
                    }
                }
            } else {
                for (final Routefilm routefilm:externalRoutefilmListWithDetails) {
                    routefilmDao.insert(routefilm);
                    final StandAloneDownloadStatus standAloneDownloadStatus = new StandAloneDownloadStatus();
                    standAloneDownloadStatus.setMovieId(routefilm.getMovieId().intValue());
                    standAloneDownloadStatus.setDownloadMovieId(routefilm.getMovieId().intValue());
                    standAloneDownloadStatus.setDownloadStatus(-1);
                    downloadStatusDao.insert(standAloneDownloadStatus);
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

    private boolean hasUpdate(final Routefilm routefilm, final List<Routefilm> localRoutefilmList) {
        if (localRoutefilmList!= null && localRoutefilmList.size()>0) {
            for (final Routefilm film: localRoutefilmList) {
                if (routefilm.getMovieId().intValue() == film.getMovieId().intValue()) {
                    if (routefilm.getMovieFlagUrl() != null && film.getMovieFlagUrl() != null &&
                            !routefilm.getMovieFlagUrl().equalsIgnoreCase(film.getMovieFlagUrl())) {
                        return true;
                    }
                    if (!routefilm.getMovieUrl().equalsIgnoreCase(film.getMovieUrl())) {
                        return true;
                    }
                    if (!routefilm.getMovieImagepath().equalsIgnoreCase(film.getMovieImagepath())) {
                        return true;
                    }
                    if (routefilm.getMovieRouteinfoPath().equalsIgnoreCase(film.getMovieRouteinfoPath())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<Routefilm> getRoutefilmWithDetails(final List<Movie> externalRoutefilmList,  final List<Flag> flagList, final List<MovieFlag> movieFlagList) {
        if (externalRoutefilmList!=null && externalRoutefilmList.size()>0) {
            List<Routefilm> routefilmList = new ArrayList<>();
            for (final Movie movie: externalRoutefilmList) {
                final Routefilm routefilm = Routefilm.fromMovie(movie, accountToken);
                final MovieFlag movieFlag = getRoutefilmFlagLink(movie.getId().intValue(), movieFlagList);
                if (movieFlag!= null) {
                    final Flag routefilmFlag = getFlag(movieFlag.getFlagId().intValue(), flagList);
                    routefilm.setMovieFlagUrl(routefilmFlag.getFlagUrl());
                }
                routefilmList.add(routefilm);
            }
            return routefilmList;
        }
        return new ArrayList<>();
    }

    private MovieFlag getRoutefilmFlagLink(final int movieId, final List<MovieFlag> movieFlagList) {
        if (movieFlagList!= null && movieFlagList.size()>0) {
            for (final MovieFlag movieFlag: movieFlagList) {
                if (movieId == movieFlag.getMovieId().intValue()) {
                    return movieFlag;
                }
            }
        }
        return null;
    }

    private Flag getFlag(final int flagId, final List<Flag> flagList) {
        if (flagList != null && flagList.size()>0) {
            for (final Flag flag: flagList) {
                if (flagId == flag.getId().intValue()) {
                    return flag;
                }
            }
        }
        return null;
    }

    private boolean deletePhysicalMovie(final Movie movie) {
        final File externalStorageVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());
        String existingPathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + movie.getId();
        File existingMovieLocation = new File(existingPathname);

        //Delete folder before flatten en renaming the other
        if (existingMovieLocation.exists()) {
            DownloadHelper.deleteMovieFolder(existingMovieLocation);
            databaseRestService.writeLog(accountToken, movie.getMovieTitle()+": Deleted succesfully!", "INFO", "");
            return true;
        } else {
            databaseRestService.writeLog(accountToken, movie.getMovieTitle()+": [ERROR] Movie not found on disk!", "ERROR", "");
            return true;
        }
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
                if (localRoutefilm.getMovieId().intValue()==externalMovie.getId().intValue()) {
                    return false;
                }
            }
        }

        return true;
    }
}
