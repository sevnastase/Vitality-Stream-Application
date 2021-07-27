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
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.request.MovieDownloadProgress;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

public class DownloadMovieServiceWorker extends Worker implements ProgressCallBack {
    private static final String TAG = DownloadMovieServiceWorker.class.getSimpleName();
    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String OUTPUT_FILE_NAME = "OUTPUT_FILE_NAME";

    private NotificationManager notificationManager;

    private long totalDownloadSizeInBytes = 0;
    private long totalDownloadedSizeInBytes = 0;

    private File selectedVolume;
    private Movie routefilm;

    private String accountToken = "";

    public interface PraxCloud {
        @POST("/api/route/downloadprogress")
        void updateDownloadProgress(@Body MovieDownloadProgress progress, @Header("api-key") String accountToken);
    }

    public DownloadMovieServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        String inputDataString = inputData.getString(INPUT_ROUTEFILM_JSON_STRING); // Movie object json
        String outputFile = inputData.getString(OUTPUT_FILE_NAME); // default folder
        final String apikey = getInputData().getString("apikey");
        accountToken = apikey;

        routefilm = new Gson().fromJson(inputDataString, Movie.class);

        if (DownloadHelper.isMoviePresent(getApplicationContext(), routefilm)) {
            return Result.success();
        }

        /*
        TODO: Requirements for downloading:
         - Movie object as string in json format
         -> every file has file size in bytes
         - path to store media content usually default folder /Praxtour/
         -> media path is further formulated as {movieId}/{content}
         */

        // Mark the Worker as important
        String progress = "Starting Download";
        setForegroundAsync(createForegroundInfo(progress));

        //Transform string json to object
        if (routefilm.getMovieFileSize() == -1) {
            Log.e(TAG, "No movie filesize available");
            return Result.failure();
        }

        totalDownloadSizeInBytes = routefilm.getMovieFileSize();
        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());

        if (selectedVolume.getTotalSpace()< ApplicationSettings.MINIMUM_DISK_SPACE_BYTES) {
            Log.e(TAG, "Disk not big enough for standalone subscription.");
            return Result.failure();
        }

        if (canFileBeCopied(selectedVolume, totalDownloadSizeInBytes)) {
            try {
                //Movie
                download(routefilm.getMovieUrl(), routefilm.getMovieFileSize(), String.valueOf(routefilm.getId()));
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
                Log.e(TAG, "Error downloading");
                return Result.failure();
            }
        } else {
            insertDownloadStatus(routefilm.getId(), -2);
            Log.e(TAG, "Cant copy file");
            return Result.failure();
        }
        Data outputData = new Data.Builder()
                .putString("apikey", apikey)
                .putInt("movie-id", routefilm.getId()).build();
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

            Log.d(TAG, "Free space selectedVolume: "+selectedVolume.getFreeSpace());

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

        //SEND UPDATE OF PROGRESS TO SERVER FOR OVERVIEW OF PROGRESS IN CRM
//        sendProgressToPraxCloud(String accountToken, int movieId, int roundedDownloadProgress);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

    }

    @Override
    public void callback(CallbackByteChannel rbc, double progress) {
        double bytesRead = Double.longBitsToDouble(rbc.getReadSoFar());
        double totalSize = Double.longBitsToDouble(totalDownloadSizeInBytes);

        double totalProgress = (bytesRead / totalSize) * 100;

        int roundedProgress = (int)Math.round(totalProgress);

        insertDownloadStatus(routefilm.getId(), roundedProgress);
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
