package com.videostreamtest.workers.download;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.download.callback.ProgressCallBack;
import com.videostreamtest.workers.download.callback.CallbackByteChannel;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class DownloadMovieImagesServiceWorker extends Worker implements ProgressCallBack {
    private final static String TAG = DownloadMovieImagesServiceWorker.class.getSimpleName();

    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";
    private static final String OUTPUT_FILE_NAME = "OUTPUT_FILE_NAME";

    private File selectedVolume;
    private Movie routefilm;

    public DownloadMovieImagesServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        //GET INPUT DATA
        Data inputData = getInputData();
        String inputDataString = inputData.getString(INPUT_ROUTEFILM_JSON_STRING); // Movie object json
        String outputFile = inputData.getString(OUTPUT_FILE_NAME); // default folder
        final String apikey = getInputData().getString("apikey");

        //Create movie object from json string
        routefilm = new Gson().fromJson(inputDataString, Movie.class);

        //CHECK IF MOVIE IMAGES ARE ALREADY PRESENT
        if (DownloadHelper.isMovieImagesPresent(getApplicationContext(), routefilm)) {
            return Result.success();
        }
        //CHECK IF IMAGE SIZE AVAILABLE
        if (routefilm.getMapFileSize() == -1 ||
                routefilm.getSceneryFileSize() == -1) {
            Log.e(TAG, "No size available");
            return Result.failure();
        }

        //SELECT LARGEST VOLUME AVAILABLE
        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());
        //CHECK WHETHER THE VOLUME IS BIG ENOUGH FOR IMAGES AND MOVIES
        if (selectedVolume.getTotalSpace()< ApplicationSettings.MINIMUM_DISK_SPACE_BYTES) {
            Log.e(TAG, "Disk not big enough for standalone.");
            return Result.failure();
        }

        //Check if download can be performed
        long totalDownloadSizeInBytes = routefilm.getMapFileSize()+ routefilm.getSceneryFileSize();
        if (selectedVolume.getFreeSpace()>totalDownloadSizeInBytes) {
            try {
                //Scenery
                download(routefilm.getMovieImagepath(), routefilm.getSceneryFileSize(), String.valueOf(routefilm.getId()));
                //Map
                download(routefilm.getMovieRouteinfoPath(), routefilm.getMapFileSize(), String.valueOf(routefilm.getId()));
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
                Log.e(TAG, "Error downloading");
                return Result.failure();
            }
        } else {
            Log.e(TAG, "Cant copy file");
            return Result.failure();
        }

        //BUILD OUTPUT DATA
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

        InputStream inputStream = inputUrl.openStream();

        ReadableByteChannel readableByteChannel = new CallbackByteChannel(Channels.newChannel(inputStream), expectedSize, this);
        String fileName = new File(inputUrl.getFile()).getName();

        if (selectedVolume.exists()) {

            Log.d(DownloadMovieImagesServiceWorker.class.getSimpleName(), "Free space selectedVolume: "+selectedVolume.getFreeSpace());

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
            inputStream.close();
            return;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieIdFolder+"/"+fileName);
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        inputStream.close();
        fileOutputStream.close();
    }

    @Override
    public void callback(CallbackByteChannel rbc, double progress) {

    }
}
