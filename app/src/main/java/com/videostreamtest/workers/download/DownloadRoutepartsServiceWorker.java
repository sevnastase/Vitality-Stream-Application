package com.videostreamtest.workers.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.tracker.GeneralDownloadTracker;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.download.callback.ProgressCallBack;
import com.videostreamtest.workers.download.callback.CallbackByteChannel;
import com.videostreamtest.workers.webinterface.PraxCloud;

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

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

public class DownloadRoutepartsServiceWorker extends Worker implements ProgressCallBack {
    private static final String TAG = DownloadRoutepartsServiceWorker.class.getSimpleName();
    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String SOUND_FOLDER = "sound";

    private NotificationManager notificationManager;

    private GeneralDownloadTracker generalDownloadTracker = new GeneralDownloadTracker();
    private File selectedVolume;
    private String apikey = "";
    private PraxCloud praxCloud;

    public DownloadRoutepartsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        //Get Input
        apikey = getInputData().getString("apikey");

        // Mark the Worker as important
        String progress = "Download Routeparts";
        setForegroundAsync(createForegroundInfo(progress));
        Data outputData = new Data.Builder()
                .putString("progress-notification", "Downloading routeparts.")
                .build();
        setProgressAsync(outputData);

        //Select largest volume
        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());
        //CHECK WHETHER THE VOLUME IS BIG ENOUGH FOR IMAGES AND MOVIES
        if (selectedVolume.getTotalSpace()< ApplicationSettings.MINIMUM_DISK_SPACE_BYTES) {
            Log.e(TAG, "Disk not big enough for standalone.");
            new DatabaseRestService().writeLog(apikey, "[Routeparts download] Disk nog big enough for standalone.", "DEBUG", "");
            return Result.failure();
        }

        initPraxCloudInterface();
        List<Movie> accountMovies = getRoutefilms();
        int totalRouteparts = getAllRouteparts(accountMovies);

        generalDownloadTracker.setId(4);
        generalDownloadTracker.setDownloadType("movie-routepart-images");
        generalDownloadTracker.setDownloadTypeTotal(totalRouteparts);
        generalDownloadTracker.setDownloadTypeCurrent(0);
        generalDownloadTracker.setDownloadCurrentFile("");
        PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);

        downloadAllRouteparts(accountMovies);

        return Result.success();
    }

    /**
     * Specify external URL, expected file size in bytes and movie id as the id will be used as foldername.
     * @param inputPath
     * @param expectedSize
     * @throws IOException
     */
    private void download(final String inputPath, final long expectedSize, final String movieIdFolder) throws IOException {
        URL inputUrl = new URL(inputPath);

        ReadableByteChannel readableByteChannel = new CallbackByteChannel(Channels.newChannel(inputUrl.openStream()), expectedSize, this);
        String fileName = new File(inputUrl.getFile()).getName();

        if (selectedVolume.exists()) {

            //Create main folder on external storage if not already there
            if (new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER).exists() &&
                    new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER).isDirectory()) {
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

    private List<Movie> getRoutefilms() {
        Call<List<Movie>> call = praxCloud.getRoutefilms(apikey);
        List<Movie> accountMovies = new ArrayList<>();

        try {
            accountMovies = call.execute().body();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
        }
        if (accountMovies!=null) {
            Log.d(TAG, "RouteFilms Count RetroFit :: " + accountMovies.size());
        }
        return accountMovies;
    }

    private int getAllRouteparts(final List<Movie> accountMovies) {
        int numberOfRouteparts = 0;
        if (accountMovies != null && accountMovies.size()>0) {
            for (final Movie movie : accountMovies) {
                Call<List<MoviePart>> callParts = praxCloud.getRoutepartsOfMovieId(movie.getId(), apikey);
                final List<MoviePart> routeparts;
                try {
                    routeparts = callParts.execute().body();
                    numberOfRouteparts += routeparts.size();
                } catch (IOException ioException) {
                    Log.e(TAG, ioException.getLocalizedMessage());
                }
            }
        }
        return numberOfRouteparts;
    }

    private void downloadAllRouteparts(final List<Movie> accountMovies) {
        if (accountMovies != null && accountMovies.size()>0) {
            for (final Movie movie: accountMovies) {
                Call<List<MoviePart>> callParts = praxCloud.getRoutepartsOfMovieId(movie.getId(), apikey);
                List<MoviePart> routeparts = new ArrayList<>();
                try {
                    routeparts = callParts.execute().body();
                } catch (IOException ioException) {
                    Log.e(TAG, ioException.getLocalizedMessage());
                }

                try {
                    if (routeparts != null && routeparts.size()>0) {
                        for (final MoviePart moviePart: routeparts) {
                            String moviePartImageName = new File(moviePart.getMoviepartImagepath()).getName();
                            String pathname = selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movie.getId().intValue()+"/"+moviePartImageName;
                            if (!new File(pathname).exists()) {
                                generalDownloadTracker.setDownloadCurrentFile(new File(moviePart.getMoviepartImagepath()).getName());
                                generalDownloadTracker.setDownloadTypeCurrent(generalDownloadTracker.getDownloadTypeCurrent()+1);
                                PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);

                                download(moviePart.getMoviepartImagepath(), Long.MAX_VALUE, String.valueOf(movie.getId().intValue()));
                            }
                        }
                    }
                } catch (IOException ioException) {
                    Log.e(TAG, ioException.getLocalizedMessage());
                }
            }
        }
        generalDownloadTracker.setDownloadCurrentFile("done");
        PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
    }

    private ForegroundInfo createForegroundInfo(@NonNull String progress) {
        //Build a notification using bytesRead and contentLength
        Context context = getApplicationContext();
        String id = "download_media" ;//context.getString(R.string.notification_channel_id);
        String title = context.getString(R.string.app_name);//notification_title
        String cancel = "Cancel";//context.getString(R.string.cancel_download);

        //This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context).createCancelPendingIntent(getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        Notification notification = new NotificationCompat.Builder(context, id)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .addAction(R.drawable.exo_icon_stop, cancel, intent)
                .build();
        return new ForegroundInfo(99995, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        // Create a Notification channel
        NotificationChannel channel = new NotificationChannel("download_media", "download_media", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("download_media");

        assert notificationManager != null;
        notificationManager.createNotificationChannel(channel);
    }
}
