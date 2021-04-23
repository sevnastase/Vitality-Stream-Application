package com.videostreamtest.ui.phone.helpers;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.videostreamtest.data.model.Movie;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadHelper {
    private static final String TAG = DownloadHelper.class.getSimpleName();

    private static DownloadHelper thisInstance;

    public static DownloadHelper getInstance() {
        if (thisInstance == null) {
            thisInstance = new DownloadHelper();
        }
        return thisInstance;
    }

    /**
     * Check if movie folder with content is located on any local storage device within provided context
     * @param context
     * @param movie
     * @return boolean
     */
    public static boolean isMoviePresent(final Context context, final Movie movie){
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movie.getId();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                long totalSizeOnDisk = 0;

                for (File file: possibleMovieLocation.listFiles()) {
                    totalSizeOnDisk += file.length();
                }

                long totalEstimatedSize = movie.getMapFileSize()+movie.getSceneryFileSize()+movie.getMovieFileSize();

                if (totalSizeOnDisk >= totalEstimatedSize) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adjust object url's to local storage paths
     * @param context
     * @param movie
     */
    public static void setLocalMedia(final Context context, final Movie movie) {
        String movieFileName = "";
        String sceneryFileName = "";
        String mapFilename = "";
        try {
            URL movieUrl = new URL(movie.getMovieUrl());
            movieFileName = new File(movieUrl.getFile()).getName();
            URL sceneryUrl = new URL(movie.getMovieImagepath());
            sceneryFileName = new File(sceneryUrl.getFile()).getName();
            URL mapUrl = new URL(movie.getMovieRouteinfoPath());
            mapFilename = new File(mapUrl.getFile()).getName();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return;
        }
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + movie.getId();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                movie.setMovieUrl(pathname+"/"+movieFileName);
                movie.setMovieImagepath(pathname+"/"+sceneryFileName);
                movie.setMovieRouteinfoPath(pathname+"/"+mapFilename);
            }
        }
    }

    /**
     * Select volume with the largest free space as recommended storage.
     * @param context
     * @return
     */
    public static File selectStorageVolumeWithLargestFreeSpace(Context context) {
        File selectedVolume = null;
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        long freeSpace = 0;
        for (File externalStorageVolume: externalStorageVolumes) {
            if (externalStorageVolume.getFreeSpace() > freeSpace) {
                freeSpace = externalStorageVolume.getFreeSpace();
                selectedVolume = externalStorageVolume;
            }
        }
        return selectedVolume;
    }

    /**
     * Returns if there is enough free space to download the entire media needed for the movie.
     * @param context
     * @param fileSize
     * @return boolean
     */
    public static boolean canFileBeCopied(Context context, long fileSize) {
        return (selectStorageVolumeWithLargestFreeSpace(context).getFreeSpace() > fileSize);
    }
}
