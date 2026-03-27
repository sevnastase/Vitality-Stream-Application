package com.videostreamtest.ui.phone.update;

import static com.videostreamtest.constants.PraxConstants.ApkUpdate.EVENT_INSTALL_COMPLETE;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_ACCOUNT_TOKEN;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_FROM_UPDATE_ACTIVITY;
import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.ApkDescription;
import com.videostreamtest.ui.phone.splash.SplashActivity;
import com.videostreamtest.workers.download.callback.CallbackByteChannel;
import com.videostreamtest.workers.download.callback.ProgressCallBack;
import com.videostreamtest.workers.webinterface.PraxCloud;

import java.io.File;
import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UpdateLauncherActivity extends AppCompatActivity {
    private final static String TAG = UpdateLauncherActivity.class.getSimpleName();
    
    private static final int MIN_WAIT_TIME_LOADING_MS = 1500;
    private String accountToken;
    ApkDescription launcherPackageInfo;

    private TextView titleTextView;
    private ProgressBar checkingForUpdatesLoadingWheel;
    private TextView descriptionTextView;
    private Button startInstallationButton;
    private Button permissionButton;
    private Button restartProcessButton;
    private LinearLayout installationInProgressLayout;
    private SeekBar downloadStatusProgressBar;
    private TextView appAccountInfoTextView;
    private final BroadcastReceiver installResultLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Greg installation complete");
            deleteApkFromCache();
            goToSplashActivity();
        }
    };
    /**
     * TLDR; A different text is displayed after the user rechecks for the {@code Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES}
     * permission, but they did not grant it.
     * <p>
     * At a point the user might be asked to grant {@code Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES}
     * permission. Since the permission is toggleable and there is no direct "result" that
     * we could register for, the user manually has to recheck the permission. When this happens,
     * and the user did NOT grant the permission (did not toggle), a different text is displayed
     * to emphasize it. For that, this boolean is used.
     *   */
    boolean manuallyRecheckedInstallPermission = false;

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    switch (result.getResultCode()) {
                        case Activity.RESULT_OK:
                            runOnUiThread(UpdateLauncherActivity.this::showInstallationStep);
                            break;
                        case Activity.RESULT_CANCELED:
                            // TODO show permission required page
                            break;
                        default:
                            // TODO error occurred during permission giving, contact service@praxtour.com
                            break;
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_launcher);

        titleTextView = findViewById(R.id.title_textview);
        checkingForUpdatesLoadingWheel = findViewById(R.id.checking_for_updates_loading_wheel);
        descriptionTextView = findViewById(R.id.description_textview);
        startInstallationButton = findViewById(R.id.start_installation_button);
        permissionButton = findViewById(R.id.permission_button);
        restartProcessButton = findViewById(R.id.restart_process_button);
        installationInProgressLayout = findViewById(R.id.installation_in_progress_layout);
        downloadStatusProgressBar = findViewById(R.id.download_status_progressbar);
        downloadStatusProgressBar.setMax(100);
        appAccountInfoTextView = findViewById(R.id.app_account_info_textview);

        appAccountInfoTextView.setText(String.format("Version %s", getAppVersion()));

        restartProcessButton.setOnClickListener(view -> restartUpdateProcess());
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onClick(View v) {
                // First press: request, change the button text
                requestInstallPackagesPermission();
                permissionButton.setText(getString(R.string.recheck_permission_button_text));

                permissionButton.setOnClickListener(view1 -> {
                    // Second+ press: different screen to emphasize request to user
                    manuallyRecheckedInstallPermission = true;
                    runOnUiThread(() -> {
                        if (appHasInstallPackagePermission()) {
                            showInstallationStep();
                        } else {
                            requestInstallPackagesPermission();
                        }
                    });
                });
            }
        });

        startInstallationButton.setOnClickListener(view -> {
            try {
                startInstallationButton.setVisibility(View.GONE);
                installationInProgressLayout.setVisibility(View.VISIBLE);
                boolean result = PraxPackageInstaller.installApk(this, launcherPackageInfo.getApkFilepath());
                if (!result) throw new IOException("Unknown error:(");
            } catch (IOException e) {
                runOnUiThread(() -> displayErrorEncountered(ErrorStep.INSTALL));
                Log.e(TAG, "Greg uh-oh " + e);
            }
        });

        accountToken = getIntent().getStringExtra(EXTRA_ACCOUNT_TOKEN);

        new Thread(() -> {
            try {
                Thread.sleep(MIN_WAIT_TIME_LOADING_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Unexpected interrupt", e);
            };
            launcherPackageInfo = fetchLauncherPackageInfo();
            if (launcherPackageInfo == null) {
                goToSplashActivity();
                return;
            }
            runOnUiThread(() -> checkingForUpdatesLoadingWheel.setVisibility(View.GONE));
            processPackage();
        }).start();
    }

    private void processPackage() {
        new Thread(() -> {
            if (!isUpdateAvailable()) {
                goToSplashActivity();
                return;
            }

            if (!isApkAvailableLocally()) {
                runOnUiThread(this::showDownloadStep);
                boolean downloaded = downloadApk();
                if (!downloaded) {
                    runOnUiThread(() -> displayErrorEncountered(ErrorStep.DOWNLOAD));
                    goToSplashActivity();
                    return;
                }
            }

            runOnUiThread(() -> {
                if (appHasInstallPackagePermission()) {
                    showInstallationStep();
                }
            });
        }).start();
    }

    private boolean isUpdateAvailable() {
        PackageInfo onDevicePackageInfo;
        try {
            onDevicePackageInfo = getPackageManager().getPackageInfo(launcherPackageInfo.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }

        String installedVersion = onDevicePackageInfo.versionName;
        String remoteVersion = launcherPackageInfo.getVersion();

        String[] installedVersionSplit = installedVersion.split("\\.");
        String[] remoteVersionSplit = remoteVersion.split("\\.");

        if (installedVersionSplit.length != remoteVersionSplit.length) {
            throw new RuntimeException("Version number patterns don't match!" +
                    "\n\tLocal: " + installedVersion +
                    "\n\tRemote: " + remoteVersion);
        }

        for (int i = 0; i < installedVersionSplit.length; i++) {
            int local = Integer.parseInt(installedVersionSplit[i]);
            int remote = Integer.parseInt(remoteVersionSplit[i]);

            if (remote > local) return true;
        }

        return false;
    }

    private boolean isApkAvailableLocally() {
        String savedFileName = launcherPackageInfo.getVersion() + ".apk";
        return new File(getCacheDir(), savedFileName).exists();
    }

    private boolean downloadApk() {
        File apkFile = ApkDownloader.download(this, launcherPackageInfo.getApkFilepath(), launcherPackageInfo.getFileSizeBytes(), new ProgressCallBack() {
            @Override
            public void callback(CallbackByteChannel rbc, double progress) {
                downloadStatusProgressBar.setProgress((int) progress);
            }
        });
        runOnUiThread(() -> downloadStatusProgressBar.setVisibility(View.GONE));

        if (apkFile == null) {
            return false;
        }
        Log.d(TAG, "Greg file is ok: " + apkFile.getAbsolutePath());

        return true;
    }

    private ApkDescription fetchLauncherPackageInfo() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PRAXCLOUD_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PraxCloud praxCloud = retrofit.create(PraxCloud.class);

        try {
            return praxCloud.getLauncherPackageInfo(accountToken).execute().body();
        } catch (IOException e) {
            throw new RuntimeException("Problem while getting package names");
        }
    }

    /**
     * @return true if permission is already granted or device is running an OS lower than Android 8,
     * false otherwise.
     */
    private boolean appHasInstallPackagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            showPermissionRequiredPage();
            return false;
        }

        return true;
    }

    private void showDownloadStep() {
        String appName = launcherPackageInfo.getAppName();

        titleTextView.setText(String.format("Downloading update for %s", appName));
        descriptionTextView.setText(String.format(getString(R.string.app_update_status_download_text), launcherPackageInfo.getVersion(), launcherPackageInfo.getAppName()));
        descriptionTextView.setVisibility(View.VISIBLE);
        permissionButton.setVisibility(View.GONE);
        downloadStatusProgressBar.setVisibility(View.VISIBLE);
    }

    private void showInstallationStep() {
        String appName = launcherPackageInfo.getAppName();

        titleTextView.setText(String.format("Install %s", appName));
        descriptionTextView.setText(getString(R.string.app_update_status_install_text));
        permissionButton.setVisibility(View.GONE);
        startInstallationButton.setVisibility(View.VISIBLE);
        startInstallationButton.requestFocus();
    }

    private void showPermissionRequiredPage() {
        String appName = launcherPackageInfo.getAppName();

        titleTextView.setText(getString(R.string.permission_required_title));
        if (manuallyRecheckedInstallPermission) {
            descriptionTextView.setText(String.format(getString(R.string.no_permission_after_interaction), appName, getPackageName()));
        } else {
            descriptionTextView.setText(String.format(getString(R.string.permission_required_description), getPackageName(), appName));
        }
        permissionButton.setText(getString(R.string.grant_permission_button_text));
        permissionButton.setVisibility(View.VISIBLE);
        permissionButton.requestFocus();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestInstallPackagesPermission() {
        Intent settingsIntent = new Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getPackageName())
        );
        activityResultLauncher.launch(settingsIntent);
    }

    private void goToSplashActivity() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra(EXTRA_FROM_UPDATE_ACTIVITY, true);
        intent.putExtra(EXTRA_ACCOUNT_TOKEN, accountToken);
        startActivity(intent);
        finish();
    }

    private void restartUpdateProcess() {
        startActivity(new Intent(UpdateLauncherActivity.this, UpdateLauncherActivity.class));
        finish();
    }

    enum ErrorStep {
        DOWNLOAD,
        INSTALL
    }

    private void displayErrorEncountered(ErrorStep step) {
        String titleText = "";
        String titleDescription = "";

        switch (step) {
            case DOWNLOAD:
                titleText = "Download error";
                titleDescription = "We encountered an error while downloading the update. Please try again. If the problem persists, contact us at service@praxtour.com";
                break;
            case INSTALL:
                titleText = "Error while installing APK";
                titleDescription = "We encountered an error while installing the update. Please try again. If the problem persists, contact us at service@praxtour.com";
        }

        titleTextView.setText(titleText);
        descriptionTextView.setText(titleDescription);
        restartProcessButton.setVisibility(View.VISIBLE);
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "ERROR";
        }
    }

    private void deleteApkFromCache() {
        String filename = launcherPackageInfo.getVersion() + ".apk";
        // inside a log just for debugging, but essentially deletes the apk to not fill up cache
        Log.d(TAG, "Greg del: " + new File(getCacheDir(), filename).delete());
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(installResultLocalReceiver, new IntentFilter(EVENT_INSTALL_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(installResultLocalReceiver);
    }
}
