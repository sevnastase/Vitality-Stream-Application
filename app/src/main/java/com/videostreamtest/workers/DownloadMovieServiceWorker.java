package com.videostreamtest.workers;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
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
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_URL;

public class DownloadMovieServiceWorker extends Worker implements ProgressCallBack {
    private static final String TAG = DownloadMovieServiceWorker.class.getSimpleName();
    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String OUTPUT_FILE_NAME = "OUTPUT_FILE_NAME";
    private static final String CHECKSUM_DIGEST_MD5_FILENAME = "checksum_digest.md5";

    private NotificationManager notificationManager;

    private DatabaseRestService databaseRestService;

    private long totalDownloadSizeInBytes = 0;
    private long totalDownloadedSizeInBytes = 0;

    private File selectedVolume;
    private Movie routefilm;
    private String accountToken = "";

    private int currentDownloadProgress = 0;

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
        final String localMediaServerUrl = getInputData().getString("localMediaServer");

        accountToken = apikey;
        routefilm = new Gson().fromJson(inputDataString, Movie.class);

        //Define which services you need
        databaseRestService = new DatabaseRestService();
        //Execute write log to cloud
        databaseRestService.writeLog(apikey, routefilm.getMovieTitle()+":DownloadMovieServiceWorker Started","DEBUG", "");

        if (accountToken==null||accountToken.isEmpty()){
            SharedPreferences myPreferences = getApplicationContext().getSharedPreferences("app",0);
            accountToken = myPreferences.getString("apikey", "unauthorized");
        }

        if (DownloadHelper.isMoviePresent(getApplicationContext(), routefilm)) {
            return Result.success();
        }

        if (routefilm.getMovieUrl().contains("http://praxmedia.praxtour.com/")) {
            if (DownloadHelper.isWebserverReachable("178.62.194.237")) {
                routefilm.setMovieUrl(routefilm.getMovieUrl().replace("http://praxmedia.praxtour.com/","http://178.62.194.237/"));
            } else {
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":CloudServerNotResponding", "ERROR", "");
            }
        }

        if (DownloadHelper.isLocalMediaServerInSameNetwork(localMediaServerUrl)) {
            if (DownloadHelper.isWebserverReachable(localMediaServerUrl)) {
                routefilm.setMovieUrl(routefilm.getMovieUrl().replace("http://praxmedia.praxtour.com/","http://"+localMediaServerUrl+"/"));
            } else {
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":LocalServerNotResponding,"+localMediaServerUrl, "ERROR", "");
            }
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
            databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":No movie filesize available", "ERROR", "");
            return Result.failure();
        }

        totalDownloadSizeInBytes = routefilm.getMovieFileSize();
        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());

        if (selectedVolume.getTotalSpace()< ApplicationSettings.MINIMUM_DISK_SPACE_BYTES) {
            Log.e(TAG, "Disk not big enough for standalone subscription.");
            databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":Disk not big enough for standalone subscription.", "ERROR", "");
            return Result.failure();
        }

        if (canFileBeCopied(selectedVolume, totalDownloadSizeInBytes)) {
            try {
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":Downloading started.", "INFO", "");
                //Movie
                download(routefilm.getMovieUrl(), routefilm.getMovieFileSize(), String.valueOf(routefilm.getId()));

                final String checksumDigest = DownloadHelper.calculateMD5(new File(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+routefilm.getId()+"/"+new File(routefilm.getMovieUrl()).getName()));
                final String externalChecksum = getExternalChecksumDigest(routefilm);

                //Compare the checksums
                if (!checksumDigest.equals(externalChecksum)) {
                    new File(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+routefilm.getId()+"/"+new File(routefilm.getMovieUrl()).getName()).delete();
                    databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+": Data Integrity Error! File Corrupted!", "ERROR", "");
                    return Result.retry();
                    //TODO: keep track of attempts for avoiding infinite downloading loop e.g. when a disk is corrupt.
                }
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
                Log.e(TAG, "Error downloading");
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":Error downloading.", "ERROR", "");
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":"+ioException.getLocalizedMessage(), "ERROR", "");

                return Result.failure();
            }
        } else {
            insertDownloadStatus(routefilm.getId(), -2);
            Log.e(TAG, "Cant copy file");
            databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":Not enough diskspace.", "ERROR", "");
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
                Log.d(TAG, "movieID folder exists ("+movieIdFolder+")");
            } else {
                new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder).mkdir();
            }
        } else {
            Log.e(TAG, "We're doomed");
            databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":Volume doesnt exist.", "ERROR", "");
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
        if (downloadProgress > currentDownloadProgress) {
            StandAloneDownloadStatus standAloneDownloadStatus = new StandAloneDownloadStatus();
            standAloneDownloadStatus.setDownloadMovieId(movieId);
            standAloneDownloadStatus.setMovieId(movieId);
            standAloneDownloadStatus.setDownloadStatus(downloadProgress);

            PraxtourDatabase.databaseWriterExecutor.execute(()->{
                PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().insert(standAloneDownloadStatus);
            });
            currentDownloadProgress = downloadProgress;
        }
    }

    private String getExternalChecksumDigest(final Movie movie) {
        String checksum = "";
        try {
            // Create a URL for the desired page
            URL url = new URL(getBaseUrl(movie.getMovieUrl())+CHECKSUM_DIGEST_MD5_FILENAME);

            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            int linenumber = 0;
            while ((str = in.readLine()) != null) {
                // str is one line of text; readLine() strips the newline character(s)
                Log.d(TAG, str);
                if (linenumber ==0) {
                    checksum = str;
                }
                linenumber++;
            }
            in.close();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG,e.getLocalizedMessage());
        }

        return checksum;
    }

    private String getBaseUrl(final String url) {
        return url.substring(0, url.lastIndexOf(File.separator)+1);
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
