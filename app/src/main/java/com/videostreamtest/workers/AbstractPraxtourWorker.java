package com.videostreamtest.workers;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.service.wifi.WifiSpeedtest;

/**
 * This class shall define the basis for all {@link Worker}s of the app. Any {@link Worker}
 * shall extend this class. Common prep-work is defined in {@link this#doWork()} which, at the
 * end, calls the abstract {@link this#doActualWork()}, which extending classes must override.
 */
public abstract class AbstractPraxtourWorker extends Worker {
    private static final String TAG = AbstractPraxtourWorker.class.getSimpleName();

    public AbstractPraxtourWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * This method shall serve as the common prep-work area for all workers in the app.
     * If the prep fails, this method will return {@code Result.failure()} and
     * {@link this#doActualWork()} will not be called.
     * <p>
     * All workers shall override {@link this#doActualWork()}.
     */
    @NonNull
    @Override
    public Result doWork() {
        if (!WifiSpeedtest.getPingTo(PRAXCLOUD_API_URL)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Thread was interrupted: " + e);
            }
            return Result.retry();
        }

        return doActualWork();
    }

    /**
     * All workers shall override this method instead of doWork().
     */
    @NonNull
    protected abstract Result doActualWork();
}
