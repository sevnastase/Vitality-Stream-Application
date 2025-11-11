package com.videostreamtest.workers.download;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

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
import androidx.work.WorkerParameters;

import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.LocalMoviesDownloadTable;
import com.videostreamtest.config.entity.tracker.GeneralDownloadTracker;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.SoundItem;
import com.videostreamtest.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.AbstractPraxtourWorker;
import com.videostreamtest.workers.download.callback.CallbackByteChannel;
import com.videostreamtest.workers.download.callback.ProgressCallBack;
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

public class DownloadSoundServiceWorker extends AbstractPraxtourWorker implements ProgressCallBack {
    private static final String TAG = DownloadSoundServiceWorker.class.getSimpleName();
    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String SOUND_FOLDER = "sound";

    private NotificationManager notificationManager;

    private long totalDownloadSizeInBytes = 0;
    private long totalDownloadedSizeInBytes = 0;

    private File selectedVolume;
    private Movie routefilm;

    public DownloadSoundServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    protected Result doActualWork() {
        Data inputData = getInputData();
        //Get Input
        final String apikey = getInputData().getString("apikey");

        // Mark the Worker as important
        String progress = "Download Sound";
        setForegroundAsync(createForegroundInfo(progress));
        Data outputData = new Data.Builder()
                .putString("progress-notification", "Downloading sound.")
                .build();
        setProgressAsync(outputData);

        //Select largest volume
        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());
        List<SoundItem> soundItems = new ArrayList<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

        //Get the active sound items available
        Call<List<SoundItem>> callSounds = praxCloud.getSounds(apikey);
        try {
            soundItems = callSounds.execute().body();

            final GeneralDownloadTracker generalDownloadTracker = new GeneralDownloadTracker();
            generalDownloadTracker.setId(1);
            generalDownloadTracker.setDownloadType("sound");
            generalDownloadTracker.setDownloadTypeTotal(soundItems.size());
            generalDownloadTracker.setDownloadTypeCurrent(0);
            generalDownloadTracker.setDownloadCurrentFile("");

            PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
            if (soundItems.size()>0) {
                for (SoundItem soundItem : soundItems) {
                    generalDownloadTracker.setDownloadCurrentFile(new File(soundItem.getSoundUrl()).getName());
                    generalDownloadTracker.setDownloadTypeCurrent(generalDownloadTracker.getDownloadTypeCurrent()+1);
                    PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
                    download(soundItem.getSoundUrl(), Long.MAX_VALUE);
                }
            }
            generalDownloadTracker.setDownloadCurrentFile("done");
            PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
            return Result.failure();
        }
        return Result.success();
    }

    /**
     * Specify external URL, expected file size in bytes and movie id as the id will be used as foldername.
     * @param inputPath
     * @param expectedSize
     * @throws IOException
     */
    private void download(final String inputPath, final long expectedSize) throws IOException {
        URL inputUrl = new URL(inputPath);

        ReadableByteChannel readableByteChannel = new CallbackByteChannel(Channels.newChannel(inputUrl.openStream()), expectedSize, this);
        String fileName = new File(inputUrl.getFile()).getName();

        if (selectedVolume.exists()) {
            Log.d(DownloadSoundServiceWorker.class.getSimpleName(), "Free space selectedVolume: "+selectedVolume.getFreeSpace());

            //Create main folder on external storage if not already there
            if (new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER).exists() &&
                    new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER).isDirectory()) {

                Log.d(DownloadSoundServiceWorker.class.getSimpleName(), "Folder "+ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER+" exists");

            } else {
                new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER).mkdir();
            }
        } else {
            Log.e(DownloadSoundServiceWorker.class.getSimpleName(), "We're doomed");
            return;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER+"/"+fileName);
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();
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
            Log.d(DownloadSoundServiceWorker.class.getSimpleName(), externalStorageVolume.getAbsolutePath() + " >> Free ::  "+externalStorageVolume.getFreeSpace());
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
        LocalMoviesDownloadTable localMoviesDownloadTable = new LocalMoviesDownloadTable();
        localMoviesDownloadTable.setDownloadMovieId(movieId);
        localMoviesDownloadTable.setMovieId(movieId);
        localMoviesDownloadTable.setDownloadStatus(downloadProgress);

        PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().insert(localMoviesDownloadTable);
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
