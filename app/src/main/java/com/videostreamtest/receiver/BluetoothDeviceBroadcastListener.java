package com.videostreamtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BluetoothDeviceBroadcastListener extends BroadcastReceiver {
    private static final String TAG = BluetoothDeviceBroadcastListener.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent.getAction());

        final PendingResult pendingResult = goAsync();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Task task = new Task(pendingResult, intent, mainHandler);
        Thread thread = new Thread(task);

        thread.start();
    }

    private static class Task implements Runnable {

        private final PendingResult pendingResult;
        private final Intent intent;
        private final Handler handler;

        private Task(PendingResult pendingResult, Intent intent, Handler handler) {
            this.pendingResult = pendingResult;
            this.intent = intent;
            this.handler = handler;
        }

        @Override
        public void run() {
            Log.d(TAG, intent.getAction());

            // Simulating some background task
            String result = intent.getAction();

            // Once the background task is done, update UI on the main thread
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Must call finish() so the BroadcastReceiver can be recycled.
                    pendingResult.finish();
                }
            });
        }
    }
}
