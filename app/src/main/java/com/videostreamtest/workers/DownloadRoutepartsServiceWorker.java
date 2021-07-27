package com.videostreamtest.workers;

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
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;

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
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

public class DownloadRoutepartsServiceWorker extends Worker implements ProgressCallBack {
    private static final String TAG = DownloadRoutepartsServiceWorker.class.getSimpleName();
    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String SOUND_FOLDER = "sound";

    private NotificationManager notificationManager;

    private File selectedVolume;
    private int[] movieIdList;

    public DownloadRoutepartsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public interface PraxCloud {
        @GET("/api/route/movieparts/{movieId}")
        Call<List<MoviePart>> getMoviepartsOfMovieId(@Path(value = "movieId", encoded = true) Integer movieId, @Header("api-key") String accountToken);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        //Get Input
        final String apikey = getInputData().getString("apikey");
        final int movieId = getInputData().getInt("movie-id",0);

        if (DownloadHelper.isMovieMediaPresent(getApplicationContext(), movieId)) {
            return Result.success();
        }

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
            return Result.failure();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

        if (movieId != 0) {
            Call<List<MoviePart>> call = praxCloud.getMoviepartsOfMovieId(movieId, apikey);
            List<MoviePart> routeparts = new ArrayList<>();
            try {
                routeparts = call.execute().body();
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
                return Result.failure();
            }
            Log.d(TAG, "'Routeparts object: "+routeparts);
            if (routeparts!= null) {
                Log.d(TAG, "'Routeparts found: "+routeparts.size());
            }
            try {
                if (routeparts != null && routeparts.size()>0) {
                    for (final MoviePart moviePart: routeparts) {
                        download(moviePart.getMoviepartImagepath(), Long.MAX_VALUE, String.valueOf(movieId));
                    }
                }
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
                return Result.failure();
            }
        }

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
    }

    /**
     * Check which volume has the largest free space and use that volumen for copying.
     * @return
     */
    private File selectStorageVolumeWithLargestFreeSpace() {
        File selectedVolume = null;
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
        long freeSpace = 0;
        for (File externalStorageVolume: externalStorageVolumes) {
            Log.d(DownloadRoutepartsServiceWorker.class.getSimpleName(), externalStorageVolume.getAbsolutePath() + " >> Free ::  "+externalStorageVolume.getFreeSpace());
            if (externalStorageVolume.getFreeSpace() > freeSpace) {
                freeSpace = externalStorageVolume.getFreeSpace();
                selectedVolume = externalStorageVolume;
            }
        }
        return selectedVolume;
    }

    /**
     * Check whether file can be copied, on other words check if there is enough free space.
     * @param externalVolume
     * @param fileSize
     * @return
     */
    private boolean canFileBeCopied(File externalVolume, long fileSize) {
        return (externalVolume.getFreeSpace() > fileSize);
    }

    /**
     * Insert download status for movie based on id
     * @param movieId
     * @param downloadProgress
     */
    private void insertDownloadStatus(int movieId, int downloadProgress) {
        StandAloneDownloadStatus standAloneDownloadStatus = new StandAloneDownloadStatus();
        standAloneDownloadStatus.setDownloadMovieId(movieId);
        standAloneDownloadStatus.setMovieId(movieId);
        standAloneDownloadStatus.setDownloadStatus(downloadProgress);

        PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().insert(standAloneDownloadStatus);
    }

    @Override
    public void callback(CallbackByteChannel rbc, double progress) {
//        System.out.println(rbc.getReadSoFar());
//        System.out.println(progress);
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
