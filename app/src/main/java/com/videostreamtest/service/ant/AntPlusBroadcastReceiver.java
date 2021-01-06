package com.videostreamtest.service.ant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.videostreamtest.utils.RpmVectorLookupTable;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;

public class AntPlusBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = AntPlusBroadcastReceiver.class.getSimpleName();

    private SimpleExoPlayer simpleExoPlayer = null;

    public AntPlusBroadcastReceiver() {
    }

    public AntPlusBroadcastReceiver(final SimpleExoPlayer simpleExoPlayer) {
        this.simpleExoPlayer = simpleExoPlayer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(simpleExoPlayer == null) return;

        final PendingResult pendingResult = goAsync();
        Task asyncTask = new Task(pendingResult, intent, simpleExoPlayer);
        asyncTask.execute();

    }

    private static class Task extends AsyncTask<String, Integer, String> {

        private final PendingResult pendingResult;
        private final Intent intent;
        private final SimpleExoPlayer simpleExoPlayer;

        private Task(PendingResult pendingResult, Intent intent, SimpleExoPlayer simpleExoPlayer) {
            this.pendingResult = pendingResult;
            this.intent = intent;
            this.simpleExoPlayer = simpleExoPlayer;
        }

        @Override
        protected String doInBackground(String... strings) {
            int rpmReceived = intent.getIntExtra("bc_service_lastvalue", 0);

            Log.d(TAG, "Action: " + intent.getAction() + "\n");
            Log.d(TAG, "Intent cadence received: "+rpmReceived+"\n");
            Log.d(TAG, "Intent playbackspeed set: "+ RpmVectorLookupTable.getPlaybackspeed(rpmReceived)+"\n");

            //Setting the speed of the player based on our cadence rpm reading
            PlaybackParameters playbackParameters  = new PlaybackParameters(RpmVectorLookupTable.getPlaybackspeed(rpmReceived), PlaybackParameters.DEFAULT.pitch);
            simpleExoPlayer.setPlaybackParameters(playbackParameters);

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
