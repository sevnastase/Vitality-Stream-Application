package com.videostreamtest.workers.download;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadDataIntegrityCheckServiceWorker extends Worker {
    private static final String TAG = DownloadDataIntegrityCheckServiceWorker.class.getSimpleName();
    private static final String CHECKSUM_DIGEST_MD5_FILENAME = "checksum_digest.md5";

    private File selectedVolume;
    private String accountToken = "";
    private Movie routefilm;
    private String localMediaServerUrl;
    private int movieId = -1;

    public DownloadDataIntegrityCheckServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        localMediaServerUrl = getInputData().getString("localMediaServer");
        movieId = getInputData().getInt("movie-id", -1);

        selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());
        accountToken= getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey", "");

        routefilm = getRoutefilm(movieId);

        //Calculate checksum
        final String checksumDigest = DownloadHelper.calculateMD5(new File(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+routefilm.getId()+"/"+new File(routefilm.getMovieUrl()).getName()));
        final String externalChecksum = getExternalChecksumDigest(routefilm);

        //Compare the checksums
        if (checksumDigest != null && !checksumDigest.equals(externalChecksum)) {
            new File(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+routefilm.getId()+"/"+new File(routefilm.getMovieUrl()).getName()).delete();
            new DatabaseRestService().writeLog(accountToken, routefilm.getMovieTitle()+": Data Integrity Error! File Corrupted!", "ERROR", "");
//                    return Result.retry();
            insertDownloadStatus(routefilm.getId(), -1);
            //TODO: keep track of attempts for avoiding infinite downloading loop e.g. when a disk is corrupt.
        }

        return Result.success();
    }

    /**
     * Insert download status for movie based on id
     * @param movieId
     * @param downloadProgress
     */
    private void insertDownloadStatus(int movieId, int downloadProgress) {
        final StandAloneDownloadStatus standAloneDownloadStatus = new StandAloneDownloadStatus();
        standAloneDownloadStatus.setDownloadMovieId(movieId);
        standAloneDownloadStatus.setMovieId(movieId);
        standAloneDownloadStatus.setDownloadStatus(downloadProgress);

        PraxtourDatabase.databaseWriterExecutor.execute(() -> {
            PraxtourDatabase.getDatabase(getApplicationContext()).downloadStatusDao().insert(standAloneDownloadStatus);
        });
    }

    private String getExternalChecksumDigest(final Movie movie) {
        String checksum = "";
        try {
            // Create a URL for the desired page
            URL url = new URL(getBaseUrl(movie.getMovieUrl())+CHECKSUM_DIGEST_MD5_FILENAME);
//            if (url.openConnection().) {}
            // Read all the text returned by the server
            InputStream inputStream = url.openStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader in = new BufferedReader(inputStreamReader);
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
            inputStream.close();
            inputStreamReader.close();
            in.close();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG,e.getLocalizedMessage());
        } catch (Exception exception) {
            Log.e(TAG, exception.getLocalizedMessage());
        }

        return checksum;
    }

    private String getBaseUrl(final String url) {
        return url.substring(0, url.lastIndexOf(File.separator)+1);
    }

    private Movie getRoutefilm(final int movieId) {
        final Movie routefilm = Movie.fromRoutefilm(PraxtourDatabase.getDatabase(getApplicationContext()).routefilmDao().getRoutefilm(movieId));
        if (routefilm != null) {
            return routefilm;
        }
        return null;
    }
}
