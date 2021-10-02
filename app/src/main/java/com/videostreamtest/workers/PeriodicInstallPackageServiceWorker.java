package com.videostreamtest.workers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.BuildConfig;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
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

public class PeriodicInstallPackageServiceWorker extends Worker implements ProgressCallBack{
    private final static String TAG = PeriodicInstallPackageServiceWorker.class.getSimpleName();

    private static final String UPDATE_INFO_FILE = "/output-metadata.json";
    private static final String UPDATE_URL = "http://praxmedia.praxtour.com/app";

    private DatabaseRestService databaseRestService = new DatabaseRestService();

    private File selectedVolume;
    private int onlineVersion = -1;
    private String updateFileName = "";


    public PeriodicInstallPackageServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
//        final boolean updateAvailable = getInputData().getBoolean("update", false);
        final boolean updateAvailable = isUpdateAvailable();
        final String updateFileName = getInputData().getString("updatefilename");
        SharedPreferences myPreferences = getApplicationContext().getSharedPreferences("app",0);
        final String apikey = myPreferences.getString("apikey", "");

        if (updateAvailable) {
            //REMOVE ANY UPDATE FILE IF PRESENT
            if (new File(DownloadHelper.getLocalUpdateFileUri(getApplicationContext(), updateFileName).toString()).exists()) {
                new File(DownloadHelper.getLocalUpdateFileUri(getApplicationContext(), updateFileName).toString()).delete();
            }

            selectedVolume = DownloadHelper.selectStorageVolumeWithLargestFreeSpace(getApplicationContext());
            try {
                //download online file to local update folder
                download(UPDATE_URL + "/" + updateFileName, Long.MAX_VALUE);
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getLocalizedMessage());
                return Result.failure();
            }


            if (new File(DownloadHelper.getLocalUpdateFileUri(getApplicationContext(), updateFileName).toString()).exists()) {
                PackageInfo packageInfo = ConfigurationHelper.getLocalUpdatePackageInfo(getApplicationContext());
                Log.d(TAG, "EXTERNAL PACKAGE READ VERSION: " + packageInfo.versionName);
                try {
                    String versionName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
                    if (apikey != null && apikey != ""){
                        databaseRestService.writeLog(apikey, "UPDATE VERSION: "+versionName, "DEBUG", "");
                    }
                    Log.d(TAG, "CURRENT PACKAGE READ VERSION: " + versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }

                if (ConfigurationHelper.getVersionNumberCode(getApplicationContext()) >= ConfigurationHelper.getLocalUpdatePackageInfo(getApplicationContext()).versionCode) {
                    //DELETE LOCAL UPDATE
                    new File(DownloadHelper.getLocalUpdateFileUri(getApplicationContext(), updateFileName).toString()).delete();
                } else {
                    if (apikey != null && apikey != ""){
                        databaseRestService.writeLog(apikey, "UPDATE FOUND AND REQUESTING INSTALLATION", "DEBUG", "");
                    }

                    //REQUEST TO INSTALL UPDATE TO USER
                    Uri contentUri = FileProvider.getUriForFile(
                            getApplicationContext(),
                            BuildConfig.APPLICATION_ID + ".provider",
                            new File(DownloadHelper.getLocalUpdateFileUri(getApplicationContext(), updateFileName).toString()));

                    Intent autoUpdatePackage = new Intent(Intent.ACTION_VIEW);
                    autoUpdatePackage.setAction(Intent.ACTION_INSTALL_PACKAGE);
                    autoUpdatePackage.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                    autoUpdatePackage.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                    autoUpdatePackage.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    autoUpdatePackage.setDataAndType(
                            contentUri,
                            "application/vnd.android.package-archive");
                    getApplicationContext().startActivity(autoUpdatePackage);
                }
            }
        }

        Data outputData = new Data.Builder()
                .putBoolean("update", updateAvailable)
                .putString("updatefilename", updateFileName)
                .build();

        return Result.success(outputData);
    }

    private void download(final String inputPath, final long expectedSize) throws IOException {
        URL inputUrl = new URL(inputPath);

        ReadableByteChannel readableByteChannel = new CallbackByteChannel(Channels.newChannel(inputUrl.openStream()), expectedSize, this);
        String fileName = new File(inputUrl.getFile()).getName();

        if (selectedVolume.exists()) {
            Log.d(TAG, "Free space selectedVolume: "+selectedVolume.getFreeSpace());

            //Create main folder on external storage if not already there
            if (new File(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER).exists() &&
                    new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER).isDirectory()) {

                Log.d(TAG, "Folder "+ApplicationSettings.DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER+" exists");
            } else {
                new File(selectedVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER).mkdir();
            }
        } else {
            Log.e(TAG, "We're doomed");
            return;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER+"/"+fileName);
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }

    @Override
    public void callback(CallbackByteChannel rbc, double progress) {
        //MAYBE LATER
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
