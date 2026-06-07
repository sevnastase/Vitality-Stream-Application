package com.videostreamtest.helpers;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.videostreamtest.service.wifi.WifiSpeedtest;
import com.videostreamtest.workers.AbstractPraxtourWorker;

public class WorkHelper {
    private static final String TAG = WorkHelper.class.getSimpleName();

    /**
     * @param worker worker to run
     * @param inputData input data for the worker
     * @param activity calling activity, which must stay active for the entire duration of the work
     * @param finishedCallback will be called once the work is finished
     * @param failureCallback will be called if the worker could not be started
     */
    public static void enqueueWork(
            Class<? extends AbstractPraxtourWorker> worker,
            Data inputData,
            AppCompatActivity activity,
            PraxCallbacks.OnWorkerFinishedCallback finishedCallback,
            PraxCallbacks.OnFailureCallback failureCallback
    ) {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        WifiSpeedtest.getPingTo(PRAXCLOUD_API_URL, new PraxCallbacks.WifiCallback() {
            @Override
            public void onSuccess(long value) {
                activity.runOnUiThread(() -> {
                    OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(worker)
                            .setConstraints(constraint)
                            .setInputData(inputData)
                            .addTag(worker.getSimpleName())
                            .build();

                    WorkManager wm = WorkManager.getInstance(activity);

                    wm.enqueue(oneTimeWorkRequest);
                    wm.getWorkInfoByIdLiveData(oneTimeWorkRequest.getId()).observe(activity, workInfo -> {
                        if (!workInfo.getState().isFinished()) return;
                        if (finishedCallback != null) finishedCallback.run(workInfo.getState(), workInfo.getOutputData());
                    });
                });
            }

            @Override
            public void onError(Exception e) {
                if (failureCallback != null) failureCallback.run();
            }
        });
    }
}
