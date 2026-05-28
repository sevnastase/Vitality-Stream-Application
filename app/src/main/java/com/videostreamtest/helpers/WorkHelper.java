package com.videostreamtest.helpers;

import android.app.Activity;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.videostreamtest.workers.AbstractPraxtourWorker;
import com.videostreamtest.workers.synchronisation.ActiveProductsServiceWorker;

public class WorkHelper {
    /**
     * @param worker worker to run
     * @param inputData input data for the worker
     * @param activity calling activity, which must stay active for the entire duration of the work
     * @param callback callback interface that will be called once the work is finished
     */
    public static void enqueueWork(
            Class<? extends AbstractPraxtourWorker> worker,
            Data inputData,
            AppCompatActivity activity,
            PraxCallbacks.OnWorkerFinishedCallback callback
    ) {

        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(worker)
                .setConstraints(constraint)
                .setInputData(inputData)
                .addTag(worker.getSimpleName())
                .build();

        WorkManager wm = WorkManager.getInstance(activity);

        wm.enqueue(oneTimeWorkRequest);
        wm.getWorkInfoByIdLiveData(oneTimeWorkRequest.getId()).observe(activity, workInfo -> {
            if (!workInfo.getState().isFinished()) return;
            callback.run(workInfo.getState(), workInfo.getOutputData());
        });
    }
}
