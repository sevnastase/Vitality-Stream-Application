package com.videostreamtest.workers;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.GsonBuilder;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.appinfo.Metadata;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.utils.ApplicationSettings;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class UpdatePackageServiceWorker extends Worker {
    private static final String TAG = UpdatePackageServiceWorker.class.getSimpleName();

    private static final String UPDATE_INFO_FILE = "/output-metadata.json";
    private static final String UPDATE_URL = "https://praxmedia.praxtour.com/app";

    private File selectedVolume;
    private int onlineVersion = -1;
    private String updateFileName = "";

    public UpdatePackageServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        //Get Input
//        final String apikey = getInputData().getString("apikey");
//        final int[] movieIdList = getInputData().getIntArray("movie-id-list");
        boolean updateAvailable = isUpdateAvailable();

        // worker returns key / value:
        // update : true
        // filename : praxtour{VERSIONCODE}.apk
        Data outputData = new Data.Builder()
                .putBoolean("update", updateAvailable)
                .putString("updatefilename", updateFileName)
                .build();

        return Result.success(outputData);
    }

    private boolean isUpdateAvailable() {
        try {
            // Create a URL for the desired page
            URL url = new URL(UPDATE_URL+UPDATE_INFO_FILE);

            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                // str is one line of text; readLine() strips the newline character(s)
                Log.d(TAG, str);
                if (str.toLowerCase().contains("versioncode")) {
                    getOnlineVersion(str);
                }
                if (str.toLowerCase().contains("outputfile")) {
                    getUpdateFilename(str);
                }
            }
            in.close();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG,e.getLocalizedMessage());
        }

        int localVersion = ConfigurationHelper.getVersionNumberCode(getApplicationContext());

        return (localVersion < onlineVersion);
    }

    private void getOnlineVersion(final String versionCodeLine) {
        String versionCode[] = versionCodeLine.split(":");
        if (versionCode.length>0) {
            String extractedCode[] = versionCode[1].split(",");
            if (extractedCode.length>0) {
                onlineVersion = Integer.parseInt(extractedCode[0].trim());
            }
        }
    }

    private void getUpdateFilename(final String updateFileNameLine) {
        String updateFileNameLineChunks[] = updateFileNameLine.split(":");
        if (updateFileNameLineChunks.length>0) {
            String extractedCode[] = updateFileNameLineChunks[1].split(",");
            if (extractedCode.length>0) {
                updateFileName = extractedCode[0].trim().replace("\"","");
            }
        }
    }
}
