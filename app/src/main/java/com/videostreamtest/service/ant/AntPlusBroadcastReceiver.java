package com.videostreamtest.service.ant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.RpmVectorLookupTable;

public class AntPlusBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = AntPlusBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        final Task asyncTask = new Task(pendingResult, intent);
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
            Log.d(TAG, "Intent playbackspeed set: "+ RpmVectorLookupTable.getPlaybackspeed(rpmReceived)+"\n");

            VideoplayerActivity.getInstance().updateVideoPlayerScreen(rpmReceived);
            VideoplayerActivity.getInstance().updateVideoPlayerParams(rpmReceived);

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
