package com.videostreamtest.workers;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_MEDIA_URL;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.BuildConfig;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.download.callback.CallbackByteChannel;
import com.videostreamtest.workers.download.callback.ProgressCallBack;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class InstallPackageServiceWorker extends Worker implements ProgressCallBack {
    private final static String TAG = InstallPackageServiceWorker.class.getSimpleName();
    private static final String UPDATE_URL = PRAXCLOUD_MEDIA_URL+"/app";

    private File selectedVolume;
    private DatabaseRestService databaseRestService = new DatabaseRestService();

    public InstallPackageServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        final boolean updateAvailable = getInputData().getBoolean("update", false);
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

                if (ConfigurationHelper.getVersionNumberCode(getApplicationContext()) >= ConfigurationHelper.getLocalUpdatePackageInfo(getApplicationContext()).getLongVersionCode()) {
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

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INSTALL_PACKAGES) != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(getApplicationContext(), "No permission to install update!", Toast.LENGTH_LONG).show();
                        if (apikey != null && apikey != ""){
                            databaseRestService.writeLog(apikey, "No permission to install update!", "ERROR", "");
                        }
                    }

                    Intent autoUpdatePackage = new Intent(Intent.ACTION_VIEW);
                    startInstallation(contentUri);
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

    private void startInstallation(Uri apkUri) {
        try {
            PackageInstaller packageInstaller = getApplicationContext().getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            try (OutputStream out = session.openWrite("CUSTAPP", 0, -1);
                 InputStream in = getApplicationContext().getContentResolver().openInputStream(apkUri)) {
                byte[] buffer = new byte[65536];
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                }
                session.fsync(out);
            } catch (IOException e) {

            }

            Intent intent = new Intent(getApplicationContext(), InstallPackageServiceWorker.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    0, intent, PendingIntent.FLAG_IMMUTABLE);
            session.commit(pendingIntent.getIntentSender());
        } catch (IOException e) {
            Log.e(TAG, String.format("Installation failed: " + e.toString()));
        }
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
}
