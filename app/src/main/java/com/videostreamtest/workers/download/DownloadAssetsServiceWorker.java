package com.videostreamtest.workers.download;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Asset;
import com.videostreamtest.config.entity.tracker.GeneralDownloadTracker;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Set;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DownloadAssetsServiceWorker extends AbstractPraxtourWorker implements ProgressCallBack {
    private final static String TAG = DownloadAssetsServiceWorker.class.getSimpleName();

    private File selectedVolume;
    private String apikey;
    final private GeneralDownloadTracker generalDownloadTracker = new GeneralDownloadTracker();

    public DownloadAssetsServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    protected Result doActualWork() {
        //GET INPUT DATA
        Data inputData = getInputData();
        apikey = getInputData().getString("apikey");

        //CHECK IF MOVIE IMAGES ARE ALREADY PRESENT
        if (DownloadHelper.assetsAlreadyPresent(getApplicationContext())) {
            return Result.success();
        }

        //SELECT LARGEST VOLUME AVAILABLE
        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());
        final Set<Asset> assets = getAssets();

        if (assets == null || assets.isEmpty()) return Result.failure();

        final long totalSizeBytes = assets.size();

        if (totalSizeBytes > selectedVolume.getFreeSpace()) {
            Log.e(TAG, "Not enough space to download assets");
            return Result.failure();
        }

        for (Asset asset : assets) {
            Log.d(TAG, "Starting download for " + asset.getAssetUrl() + "(ID " + asset.getId() + ")");
            download(asset.getAssetUrl(), asset.getAssetSizeBytes());generalDownloadTracker.setDownloadCurrentFile(new File(asset.getAssetUrl()).getName());
            generalDownloadTracker.setDownloadTypeCurrent(generalDownloadTracker.getDownloadTypeCurrent()+1);
            PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
            Log.d(TAG, "Asset " + asset.getId() + " downloaded");
        }

        generalDownloadTracker.setDownloadCurrentFile("done");
        PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);

        Data outputData = new Data.Builder()
                .putString("apikey", apikey)
                .build();
        return Result.success(outputData);
    }

    private Set<Asset> getAssets() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);
        Set<Asset> assets;

        try {
            assets = praxCloud.getAssets(apikey).execute().body();
            if (assets == null) return null;

            generalDownloadTracker.setId(2);
            generalDownloadTracker.setDownloadType("assets");
            generalDownloadTracker.setDownloadTypeTotal(assets.size());
            generalDownloadTracker.setDownloadTypeCurrent(0);
            generalDownloadTracker.setDownloadCurrentFile("");
            PraxtourDatabase.getDatabase(getApplicationContext()).generalDownloadTrackerDao().insert(generalDownloadTracker);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        return assets;
    }

    /**
     * Specify external URL, expected file size in bytes and movie id as the id will be used as foldername.
     */
    private void download(final String inputPath, final long expectedSize) {
        URL inputUrl;

        try {
            inputUrl = new URL(inputPath);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
            return;
        }

        try (InputStream inputStream = inputUrl.openStream()) {
            ReadableByteChannel readableByteChannel = new CallbackByteChannel(Channels.newChannel(inputStream), expectedSize, this);
            String fileName = new File(inputUrl.getFile()).getName();

            File assetDirectory = new File(selectedVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_ASSETS_STORAGE_FOLDER);
            if (selectedVolume.exists()) {
                //Create main folder on external storage
                if (assetDirectory.exists() && assetDirectory.isDirectory()) {
                    Log.d(TAG, "Folder " + ApplicationSettings.DEFAULT_LOCAL_ASSETS_STORAGE_FOLDER + " exists");
                    Log.d(TAG, "Checking " + selectedVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_ASSETS_STORAGE_FOLDER);
                } else {
                    assetDirectory.mkdir();
                }
            } else {
                Log.e(TAG, "We're doomed");
                inputStream.close();
                return;
            }

            FileOutputStream fileOutputStream = new FileOutputStream(assetDirectory + "/" + fileName);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            inputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void callback(CallbackByteChannel rbc, double progress) {

    }
}
