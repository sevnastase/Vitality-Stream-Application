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

import com.google.gson.Gson;
import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class DownloadMovieServiceWorker extends Worker implements ProgressCallBack {
    private static final String TAG = DownloadMovieServiceWorker.class.getSimpleName();
    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String OUTPUT_FILE_NAME = "OUTPUT_FILE_NAME";

    private NotificationManager notificationManager;

    private long totalDownloadSizeInBytes = 0;
    private long totalDownloadedSizeInBytes = 0;

    private File selectedVolume;
    private Movie routefilm;

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
        if (routefilm.getMapFileSize() == -1 ||
            routefilm.getMovieFileSize() == -1 ||
            routefilm.getSceneryFileSize() == -1) {
            Log.e(TAG, "No size available");
            return Result.failure();
        }

        totalDownloadSizeInBytes = routefilm.getMapFileSize()+routefilm.getSceneryFileSize()+routefilm.getMovieFileSize();
        selectedVolume = selectStorageVolumeWithLargestFreeSpace();



        if (canFileBeCopied(selectedVolume, totalDownloadSizeInBytes)) {
            try {
                //Scenery
                download(routefilm.getMovieImagepath(), routefilm.getSceneryFileSize(), String.valueOf(routefilm.getId()));
                //Map
                download(routefilm.getMovieRouteinfoPath(), routefilm.getMapFileSize(), String.valueOf(routefilm.getId()));
                //Movie
                download(routefilm.getMovieUrl(), routefilm.getMovieFileSize(), String.valueOf(routefilm.getId()));
            } catch (IOException ioException) {
                Log.e(DownloadMovieServiceWorker.class.getSimpleName(), ioException.getLocalizedMessage());
                Log.e(TAG, "Error downloading");
                return Result.failure();
            }
        } else {
            insertDownloadStatus(routefilm.getId(), -2);
            Log.e(TAG, "Cant copy file");
            return Result.failure();
        }
        return Result.success();
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

            Log.d(DownloadMovieServiceWorker.class.getSimpleName(), "Free space selectedVolume: "+selectedVolume.getFreeSpace());

            //Create main folder on external storage
            if (new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER).exists() &&
                    new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER).isDirectory()) {

                Log.d(DownloadMovieServiceWorker.class.getSimpleName(), "Folder "+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+" exists");
                Log.d(DownloadMovieServiceWorker.class.getSimpleName(), "Checking "+selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder+" <<>>>>");

            } else {
                new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER).mkdir();
            }
            //Create movie folder named by movie ID
            if (new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder).exists() &&
                    new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder).isDirectory()) {
                Log.d(DownloadMovieServiceWorker.class.getSimpleName(), "movieID folder exists");
            } else {
                new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder).mkdir();
            }
        } else {
            Log.e(DownloadMovieServiceWorker.class.getSimpleName(), "We're doomed");
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
            Log.d(DownloadMovieServiceWorker.class.getSimpleName(), externalStorageVolume.getAbsolutePath() + " >> Free ::  "+externalStorageVolume.getFreeSpace());
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
        double bytesRead = Double.longBitsToDouble(rbc.getReadSoFar());
        double totalSize = Double.longBitsToDouble(totalDownloadSizeInBytes);

        double totalProgress = (bytesRead / totalSize) * 100;

        int roundedProgress = (int)Math.round(totalProgress);

        insertDownloadStatus(routefilm.getId(), roundedProgress);

        //Deprecated TODO: remove later
        Data outputData = new Data.Builder()
                .putString("progress-notification", "Progress: "+totalProgress+"%")
                .putDouble("progress", progress)
                .putInt("movie-id", routefilm.getId())
                .build();
        setProgressAsync(outputData);

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

//    class CallbackByteChannel implements ReadableByteChannel {
//        ProgressCallBack delegate;
//        long size;
//        ReadableByteChannel rbc;
//        long sizeRead;
//
//        CallbackByteChannel(ReadableByteChannel rbc, long expectedSize,
//                            ProgressCallBack delegate) {
//            this.delegate = delegate;
//            this.size = expectedSize;
//            this.rbc = rbc;
//        }
//        public void close() throws IOException {
//            rbc.close();
//        }
//        public long getReadSoFar() {
//            return sizeRead;
//        }
//
//        public boolean isOpen() {
//            return rbc.isOpen();
//        }
//
//        public int read(ByteBuffer bb) throws IOException {
//            int n;
//            double progress;
//            if ((n = rbc.read(bb)) > 0) {
//                sizeRead += n;
//                progress = size > 0 ? (double) sizeRead / (double) size
//                        * 100.0 : -1.0;
//                delegate.callback(this, progress);
//            }
//            return n;
//        }
//    }
}
