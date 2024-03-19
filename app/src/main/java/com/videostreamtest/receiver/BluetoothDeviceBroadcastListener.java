package com.videostreamtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class BluetoothDeviceBroadcastListener extends BroadcastReceiver {
    private static final String TAG = BluetoothDeviceBroadcastListener.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent.getAction());
        final PendingResult pendingResult = goAsync();
        final Task asyncTask = new Task(pendingResult, intent);
        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class Task extends AsyncTask<String, Integer, String> {

        private final PendingResult pendingResult;
        private final Intent intent;

        private Task(PendingResult pendingResult, Intent intent) {
            this.pendingResult = pendingResult;
            this.intent = intent;
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, intent.getAction());

            return intent.getAction();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // Must call finish() so the BroadcastReceiver can be recycled.
            pendingResult.finish();
        }
    }
}
