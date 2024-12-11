package com.videostreamtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

public class CadenceSensorBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = CadenceSensorBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        CadenceTask task = new CadenceTask(pendingResult, intent, mainHandler);
        Thread thread = new Thread(task);

        thread.start();
    }

    private static class CadenceTask extends TaskTemplate {
        private CadenceTask(PendingResult pendingResult, Intent intent, Handler handler) {
            super(pendingResult, intent, handler);
        }

        @Override
        public void run() {
            int rpmReceived = intent.getIntExtra(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, 0);

            Log.d(TAG, "Action: " + intent.getAction() + "\n");
            Log.d(TAG, "Intent cadence received: "+rpmReceived+"\n");

            if (VideoplayerActivity.getInstance() != null) {
                VideoplayerActivity.getInstance().updateVideoPlayerScreen(rpmReceived);
            }
            if (VideoplayerExoActivity.getInstance() != null) {
                VideoplayerExoActivity.getInstance().updateVideoPlayerScreen(rpmReceived);
            }

            /* ONLY FOR VIDEO SPEED!
             * When the rpm is above 0 ( there is activity) ) and
             * when rpm is below minimum speed
             * set rpm on static minimum speed
             */
            if (rpmReceived > 0 && rpmReceived < 50) {
                rpmReceived = 50;
            }
            if (rpmReceived > 100) {
                rpmReceived = 100;
            }
            if (VideoplayerActivity.getInstance() != null) {
                VideoplayerActivity.getInstance().updateVideoPlayerParams(rpmReceived);
            }
            if (VideoplayerExoActivity.getInstance() != null) {
                VideoplayerExoActivity.getInstance().updateVideoPlayerParams(rpmReceived);
            }

            String result = intent.getAction();

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
