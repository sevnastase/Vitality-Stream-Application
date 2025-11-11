package com.videostreamtest.workers.download;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.tracker.GeneralDownloadTracker;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.AbstractPraxtourWorker;
import com.videostreamtest.workers.download.callback.CallbackByteChannel;
import com.videostreamtest.workers.download.callback.ProgressCallBack;
import com.videostreamtest.workers.webinterface.PraxCloud;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DownloadAllMovieImagesServiceWorker extends AbstractPraxtourWorker implements ProgressCallBack {
    private final static String TAG = DownloadAllMovieImagesServiceWorker.class.getSimpleName();

    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String OUTPUT_FILE_NAME = "OUTPUT_FILE_NAME";

    private File selectedVolume;

    private PraxCloud praxCloud;
    private String apikey = "";
    private GeneralDownloadTracker generalDownloadTracker = new GeneralDownloadTracker();
    private DatabaseRestService databaseRestService = new DatabaseRestService();

    public DownloadAllMovieImagesServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    protected Result doActualWork() {
        //GET INPUT DATA
        apikey = getInputData().getString("apikey");

        //SELECT LARGEST VOLUME AVAILABLE
        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());
        //CHECK WHETHER THE VOLUME IS BIG ENOUGH FOR IMAGES AND MOVIES
        if (selectedVolume.getTotalSpace()< ApplicationSettings.MINIMUM_DISK_SPACE_BYTES) {
            Log.e(TAG, "Disk not big enough for standalone.");

            return Result.failure();
        }

        initPraxCloudInterface();
        List<Movie> allAvailableMovies = getAllRoutefilms();
        if (allAvailableMovies != null && allAvailableMovies.size()>0) {
            for (final Movie movie: allAvailableMovies) {
                downloadMovieImages(movie);
            }
        }

        generalDownloadTracker.setDownloadCurrentFile("done");
        PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);

        //BUILD OUTPUT DATA
        Data outputData = new Data.Builder()
                .putString("apikey", apikey).build();
        return Result.success(outputData);
    }

    /**
     * Specify external URL, expected file size in bytes and movie id as the id will be used as foldername.
     * @param inputPath
     * @param expectedSize
     * @param movieIdFolder
     * @throws IOException
     */
    private void download(final String inputPath, final long expectedSize, final String movieIdFolder) throws IOException {
        URL inputUrl = new URL(inputPath);

        ReadableByteChannel readableByteChannel = new CallbackByteChannel(Channels.newChannel(inputUrl.openStream()), expectedSize, this);
        String fileName = new File(inputUrl.getFile()).getName();

        if (selectedVolume.exists()) {
            Log.d(DownloadAllMovieImagesServiceWorker.class.getSimpleName(), "Free space selectedVolume: "+selectedVolume.getFreeSpace());

            //Create main folder on external storage
            if (new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER).exists() &&
                    new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER).isDirectory()) {

                Log.d(TAG, "Folder "+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+" exists");
                Log.d(TAG, "Checking "+selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder+" <<>>>>");

            } else {
                new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER).mkdir();
            }

            //Create movie folder named by movie ID
            if (new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder).exists() &&
                    new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder).isDirectory()) {
                Log.d(TAG, "movieID folder exists");
            } else {
                new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder).mkdir();
            }
        } else {
            Log.e(TAG, "We're doomed");
            return;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder+"/"+fileName);
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();
    }

    @Override
    public void callback(CallbackByteChannel rbc, double progress) {

    }

    private void initPraxCloudInterface() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        praxCloud = retrofit.create(PraxCloud.class);
    }

    private List<Movie> getAllRoutefilms() {
        Call<List<Movie>> movieListCall = praxCloud.getRoutefilms(apikey);
        List<Movie> routefilms = new ArrayList<>();
        try {
            routefilms = movieListCall.execute().body();

            generalDownloadTracker.setId(3);
            generalDownloadTracker.setDownloadType("movie-support-images");
            generalDownloadTracker.setDownloadTypeTotal(routefilms.size()*2);
            generalDownloadTracker.setDownloadTypeCurrent(0);
            generalDownloadTracker.setDownloadCurrentFile("");
            PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }
        return routefilms;
    }

    private void downloadMovieImages(final Movie routefilm) {
        //CHECK IF MOVIE IMAGES ARE ALREADY PRESENT
        if (DownloadHelper.isMovieImagesPresent(getApplicationContext(), routefilm)) {
            Log.d(getClass().getSimpleName(), "MovieImages already present.");
            return;
        }

        //Check if download can be performed
        long totalDownloadSizeInBytes = routefilm.getMapFileSize()+ routefilm.getSceneryFileSize();
        if (selectedVolume.getFreeSpace()>totalDownloadSizeInBytes) {
            try {
                //Scenery
                generalDownloadTracker.setDownloadCurrentFile(new File(routefilm.getMovieImagepath()).getName());
                generalDownloadTracker.setDownloadTypeCurrent(generalDownloadTracker.getDownloadTypeCurrent()+1);
                PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
                download(routefilm.getMovieImagepath(), Long.MAX_VALUE, String.valueOf(routefilm.getId()));
                //Map
                generalDownloadTracker.setDownloadCurrentFile(new File(routefilm.getMovieRouteinfoPath()).getName());
                generalDownloadTracker.setDownloadTypeCurrent(generalDownloadTracker.getDownloadTypeCurrent()+1);
                PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
                download(routefilm.getMovieRouteinfoPath(), Long.MAX_VALUE, String.valueOf(routefilm.getId()));
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
                Log.e(TAG, String.format("Error downloading for routefilm %s", routefilm.getMovieTitle()));
                databaseRestService.writeLog(apikey, String.format("Error downloading for routefilm %s", routefilm.getMovieTitle()), "ERROR", "");
            }
        } else {
            Log.e(TAG, String.format("Cant copy routefilm image files for movie: %s", routefilm.getMovieTitle()));
            databaseRestService.writeLog(apikey, String.format("Cant copy routefilm image files for movie: %s", routefilm.getMovieTitle()), "ERROR", "");
        }
    }
}
