package com.videostreamtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.videostreamtest.constants.CadenceSensorConstants;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;

public class CadenceSensorBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = CadenceSensorBroadcastReceiver.class.getSimpleName();

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
            int rpmReceived = intent.getIntExtra(CadenceSensorConstants.BIKE_CADENCE_LAST_VALUE, 0);
            String serviceStatus = intent.getStringExtra(CadenceSensorConstants.BIKE_CADENCE_STATUS);

            if(serviceStatus == null) {
                serviceStatus = "Active";
            }
            Log.d(TAG, "ServiceStatus: "+serviceStatus);

            Log.d(TAG, "Action: " + intent.getAction() + "\n");
            Log.d(TAG, "Intent cadence received: "+rpmReceived+"\n");

//            VideoplayerActivity.getInstance().updateDeviceStatusField(serviceStatus);

            if (serviceStatus.toLowerCase().contains("dead")) {
                VideoplayerActivity.getInstance().setDeadDeviceParams();
            } else {
                VideoplayerActivity.getInstance().updateVideoPlayerScreen(rpmReceived);

                /* ONLY FOR VIDEO SPEED!
                 * When the rpm is above 0 ( there is activity) ) and
                 * when rpm is below minimum speed of 35
                 * set rpm on minimum speed of 35
                 */
                if (rpmReceived > 0 && rpmReceived < 40) {
                    rpmReceived = 40;
                }
                if (rpmReceived > 110) {
                    rpmReceived = 110;
                }
                VideoplayerActivity.getInstance().updateVideoPlayerParams(rpmReceived);
            }

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
