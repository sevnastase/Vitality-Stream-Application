package com.videostreamtest.ui.phone.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

public class SettingsAntPlusBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = SettingsAntPlusBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        Task asyncTask = new Task(pendingResult, intent);
        asyncTask.execute();
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
            int rpmReceived = intent.getIntExtra("bc_service_lastvalue", 0);

            Log.d(TAG, "Action: " + intent.getAction() + "\n");
            Log.d(TAG, "Intent cadence received: "+rpmReceived+"\n");

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
