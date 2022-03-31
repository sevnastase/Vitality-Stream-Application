package com.videostreamtest.workers.download;

import static com.videostreamtest.utils.ApplicationSettings.NUMBER_OF_DOWNLOAD_RUNNERS;
import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_MEDIA_URL;
import static com.videostreamtest.utils.ApplicationSettings.THREAD_POOL_EXECUTOR;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
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
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.download.callback.ProgressCallBack;
import com.videostreamtest.workers.download.callback.CallbackByteChannel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

public class DownloadMovieServiceWorker extends Worker implements ProgressCallBack {
    private static final String TAG = DownloadMovieServiceWorker.class.getSimpleName();
    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String OUTPUT_FILE_NAME = "OUTPUT_FILE_NAME";
    private static final String CHECKSUM_DIGEST_MD5_FILENAME = "checksum_digest.md5";

    private DatabaseRestService databaseRestService = new DatabaseRestService();

    private NotificationManager notificationManager;
    private String downloadRunnerTag;
    private int downloadRunnerId = -1;

    private long totalDownloadSizeInBytes = 0;
    private long totalDownloadedSizeInBytes = 0;

    private File selectedVolume;
    private Movie routefilm;
    private String accountToken = "";
    private String localMediaServerUrl;
    private int movieId = -1;

    private int currentDownloadProgress = 0;

    public DownloadMovieServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        localMediaServerUrl = getInputData().getString("localMediaServer");
        movieId = getInputData().getInt("movie-id", -1);

        final String apikey = getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey", "");

        this.downloadRunnerTag = getTag(getTags().toArray(new String[0]));
        Log.d(TAG, "PARSING TAG: "+downloadRunnerTag);
        Log.d(TAG, "PARSING STRING: "+downloadRunnerTag.substring(downloadRunnerTag.length()-1));
        this.downloadRunnerId = Integer.parseInt(downloadRunnerTag.substring(downloadRunnerTag.length()-1));
        Log.d(TAG, "DownloadRunnerId: "+this.downloadRunnerId);

        //Execute write log to cloud
        databaseRestService.writeLog(apikey, "DownloadRunner "+getTags().toArray(new String[0])[0]+" Started","DEBUG", "");
        Log.d(TAG, "DownloadRunner "+getTags().toArray(new String[0])[0]+" Started");

        accountToken = apikey;

        if (accountToken==null||accountToken.isEmpty()){
            SharedPreferences myPreferences = getApplicationContext().getSharedPreferences("app",0);
            accountToken = myPreferences.getString("apikey", "unauthorized");
        }

        // Mark the Worker as important
        String progress = "Downloading";
        setForegroundAsync(createForegroundInfo(progress));

        THREAD_POOL_EXECUTOR.execute(() ->{
            if (movieId == -1) {
                startDownloadRunner();
            } else {
                startDownload(movieId);
            }
        });

        Log.d(getClass().getSimpleName(), "ActiveCount: "+THREAD_POOL_EXECUTOR.getActiveCount());
        Log.d(getClass().getSimpleName(), "QueueCount: "+THREAD_POOL_EXECUTOR.getQueue().size());

        Data outputData = new Data.Builder()
                .putString("apikey", apikey)
                .build();
        return Result.success(outputData);
    }

    private String getTag(final String[] tags) {
        if (tags!=null && tags.length>0) {
            for (final String tag:tags) {
                final String tagStripped = tag.replaceAll("[^\\d]", "");
                if (!tagStripped.isEmpty()) {
                    return tagStripped;
                }
            }
        }
        return "0";
    }

    private void startDownloadRunner() {
        List <StandAloneDownloadStatus> pendingDownloads = retrieveCurrentPendingDownloads();
        if (pendingDownloads!= null && pendingDownloads.size()>0){
            Log.d(TAG, "Number of pending downloads: "+pendingDownloads.size());
            new DatabaseRestService().writeLog(accountToken, "Number of pending downloads: "+pendingDownloads.size(), "DEBUG", "");
            this.routefilm = null;
            this.currentDownloadProgress = 0;
            final StandAloneDownloadStatus nextDownload = searchNextDownload(pendingDownloads);
            if (nextDownload != null) {
                startDownload(nextDownload.getMovieId());
                startDownloadRunner(); //RECURSION UNTIL PENDING DOWNLOADS = 0;
            }
        }
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
        FileOutputStream fileOutputStream = new FileOutputStream(selectedVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + movieIdFolder + "/" + fileName);
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
        if (downloadProgress > currentDownloadProgress || downloadProgress == 0) {
            if (downloadProgress>0) {
                Log.d(TAG, String.format("Movie %s is for %d percent ready.", routefilm.getMovieTitle(), downloadProgress));
            }
            final StandAloneDownloadStatus standAloneDownloadStatus = new StandAloneDownloadStatus();
            standAloneDownloadStatus.setDownloadMovieId(movieId);
            standAloneDownloadStatus.setMovieId(movieId);
            standAloneDownloadStatus.setDownloadStatus(downloadProgress);

//            Log.d(TAG, String.format("DownloadStatusInformation: movieId: %d, DownloadStatus: %d, MovieTitle: %s", standAloneDownloadStatus.getDownloadMovieId(), standAloneDownloadStatus.getDownloadStatus(), routefilm.getMovieTitle()));

//            PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().insert(standAloneDownloadStatus);

            PraxtourDatabase.databaseWriterExecutor.execute(() -> {
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
        if ( this.routefilm != null) {
            insertDownloadStatus(routefilm.getId(), roundedProgress);
        }
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

    private List<StandAloneDownloadStatus> retrieveCurrentPendingDownloads() {
        List<StandAloneDownloadStatus> pendingDownloads = PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().getPendingDownloadStatus();
        if (pendingDownloads != null && pendingDownloads.size()>0) {
            return pendingDownloads;
        }
        return new ArrayList<>();
    }

    private StandAloneDownloadStatus searchNextDownload(final List<StandAloneDownloadStatus> pendingDownloads) {
        if (pendingDownloads != null && pendingDownloads.size()>0) {
            for (final StandAloneDownloadStatus downloadStatus: pendingDownloads) {
                if (downloadStatus.getMovieId() % ApplicationSettings.NUMBER_OF_DOWNLOAD_RUNNERS == downloadRunnerId) {
                    return downloadStatus;
                }
            }
        }
        return null;
    }

    private void startDownload(final int movieId) {
        final Movie routefilm = getRoutefilm(movieId);
        if (routefilm == null) {
            return;
        }
        if (isDownloadable(routefilm)) {
            selectDownloadServer(routefilm);
            downloadMovie(routefilm);
        }
    }

    private Movie getRoutefilm(final int movieId) {
        final Movie routefilm = Movie.fromRoutefilm(PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao().getRoutefilm(movieId));
        if (routefilm != null) {
            this.routefilm = routefilm;
            return routefilm;
        }
        return null;
    }

    private boolean isDownloadable(final Movie routefilm) {
        if (isAlreadyDownloaded(routefilm)) {
            insertDownloadStatus(routefilm.getId(), 100);
            return false;
        }
        if (routefilm.getMovieFileSize() == -1) {
            Log.e(TAG, "No movie filesize available");
            databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":No movie filesize available", "ERROR", "");
            return false;
        }

        return true;
    }

    private boolean isAlreadyDownloaded(final Movie routefilm) {
        return DownloadHelper.isMoviePresent(getApplicationContext(), routefilm);
    }

    private void selectDownloadServer(final Movie routefilm) {
        if (routefilm.getMovieUrl().contains("http://praxmedia.praxtour.com/")) {
            if (DownloadHelper.isWebserverReachable("178.62.194.237")) {
                routefilm.setMovieUrl(routefilm.getMovieUrl().replace("http://praxmedia.praxtour.com/",PRAXCLOUD_MEDIA_URL+"/"));
            } else {
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":CloudServerNotResponding", "ERROR", "");
            }
        }

        if (localMediaServerUrl != null && DownloadHelper.isLocalMediaServerInSameNetwork(localMediaServerUrl)) {
            if (DownloadHelper.isWebserverReachable(localMediaServerUrl)) {
                routefilm.setMovieUrl(routefilm.getMovieUrl().replace("http://praxmedia.praxtour.com/","http://"+localMediaServerUrl+"/"));
            } else {
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":LocalServerNotResponding,"+localMediaServerUrl, "ERROR", "");
            }
        }
    }

    private void downloadMovie(final Movie routefilm) {
        totalDownloadSizeInBytes = routefilm.getMovieFileSize();
        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());

        if (selectedVolume.getTotalSpace()< ApplicationSettings.MINIMUM_DISK_SPACE_BYTES) {
            Log.e(TAG, "Disk not big enough for standalone subscription.");
            databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+": Disk not big enough for standalone subscription.", "ERROR", "");
//            return Result.failure();
            insertDownloadStatus(routefilm.getId(), -2);
        }

        if (canFileBeCopied(selectedVolume, totalDownloadSizeInBytes)) {
            try {
                insertDownloadStatus(routefilm.getId(), 0);
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+": Downloading started.", "INFO", "");
                Log.d(TAG, "Start downloading "+routefilm.getMovieTitle());
                //Movie
                download(routefilm.getMovieUrl(), routefilm.getMovieFileSize(), String.valueOf(routefilm.getId()));

                //Calculate checksum
                final String checksumDigest = DownloadHelper.calculateMD5(new File(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+routefilm.getId()+"/"+new File(routefilm.getMovieUrl()).getName()));
                final String externalChecksum = getExternalChecksumDigest(routefilm);

                //Compare the checksums
                if (checksumDigest != null && !checksumDigest.equals(externalChecksum)) {
                    new File(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+routefilm.getId()+"/"+new File(routefilm.getMovieUrl()).getName()).delete();
                    databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+": Data Integrity Error! File Corrupted!", "ERROR", "");
//                    return Result.retry();
                    insertDownloadStatus(routefilm.getId(), -1);
                    //TODO: keep track of attempts for avoiding infinite downloading loop e.g. when a disk is corrupt.
                }
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
                Log.e(TAG, "Error downloading");
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+": Error downloading.", "ERROR", "");
                databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+":"+ioException.getLocalizedMessage(), "ERROR", "");
//                return Result.failure();
                insertDownloadStatus(routefilm.getId(), -1);
            }
        } else {
            insertDownloadStatus(routefilm.getId(), -2);
            Log.e(TAG, "Cant copy file");
            databaseRestService.writeLog(accountToken, routefilm.getMovieTitle()+": Not enough diskspace.", "ERROR", "");
        }
    }
}
