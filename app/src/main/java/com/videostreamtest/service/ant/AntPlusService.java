package com.videostreamtest.service.ant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.EnumSet;

public class AntPlusService extends Service {
    private static final String TAG = AntPlusService.class.getSimpleName();

    private static final int ONGOING_NOTIFICATION_ID = 99999;
    private static final String CHANNEL_DEFAULT_IMPORTANCE = "antplus_ble_channel";

    //ANT+ sensors
    //Bike Cadence
    private AntPlusBikeCadencePcc bcPcc = null;
    private PccReleaseHandle<AntPlusBikeCadencePcc> bcReleaseHandle = null;
    //Bike Speed
    private AntPlusBikeSpeedDistancePcc bsdPcc = null;
    private PccReleaseHandle<AntPlusBikeSpeedDistancePcc> bsdReleaseHandle = null;
    //Heart rate sensor
    private AntPlusHeartRatePcc hrPcc = null;
    private PccReleaseHandle<AntPlusHeartRatePcc> hrReleaseHandle = null;

    // 700x23c circumference in meter
    private static final BigDecimal circumference = new BigDecimal("2.095");
    // m/s to km/h ratio
    private static final BigDecimal msToKmSRatio = new BigDecimal("3.6");

    private int lastCadence = 0;

    @Override
    public void onCreate() {
        Log.d(TAG, "Ant Plus Service started");
        super.onCreate();
        //Start cadence service TODO: FOLLOWUP BY divide in services/design patterns
//        Intent cadenceService = new Intent(getApplicationContext(), AntDeviceChangeReceiver.class);
//        cadenceService.putExtra("antSensorType", AntSensorType.CyclingCadence);
//        getApplicationContext().startService(cadenceService);

        initAntPlus();
    }

    @Override
    public void onDestroy() {
        if( bcReleaseHandle != null ) {
            bcReleaseHandle.close();
        }
        stopSelf();
        super.onDestroy();
    }

    //Must implement by the Service parent class which is extended
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isAntPlusDevicePresent(Context context) {
        return (AntPluginPcc.getInstalledPluginsVersionNumber(context) > 0);
    }

    private AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikeCadencePcc> mBCResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikeCadencePcc>() {
        @Override
        public void onResultReceived(AntPlusBikeCadencePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
            if( resultCode == RequestAccessResult.SUCCESS) {
                bcPcc = result;
                Log.d(TAG, result.getDeviceName() + ": "+ initialDeviceState);
                subscribeToEvents();
            } else if (resultCode == RequestAccessResult.USER_CANCELLED) {
                Log.d(TAG, "BikeCadence Closed: " + resultCode);
            } else {
                Log.d(TAG, "BikeCadence state changed: " + initialDeviceState + ", resultCode: "+ resultCode);
            }

            //send broadcast
            Intent i = new Intent("com.fitstream.ANTDATA");
            i.putExtra("bc_service_status", initialDeviceState.toString() + "\n("+resultCode+")");
            sendBroadcast(i);
        }

        private void subscribeToEvents() {
            bcPcc.subscribeCalculatedCadenceEvent(new AntPlusBikeCadencePcc.ICalculatedCadenceReceiver() {
                @Override
                public void onNewCalculatedCadence(long estimatedTimeStamp, EnumSet<EventFlag> eventFlags, BigDecimal calculatedCadence) {
                    Log.v(TAG, "Cadence value: "+calculatedCadence.intValue());
                    Log.v(TAG, "ANTDeviceNumber: "+bcPcc.getAntDeviceNumber());

                    //Set last received cadence value
                    lastCadence = calculatedCadence.intValue();
                    
                    // send broadcast about device status
                    Intent broadcastIntent = new Intent("com.fitstream.ANTDATA");
                    broadcastIntent.putExtra("bc_service_lastvalue", calculatedCadence.intValue());
//                    broadcastIntent.putExtra("bc_service_channel", bcPcc.getAntDeviceNumber());
                    sendBroadcast(broadcastIntent);
                }
            });
        }
    };

    private class AntDeviceChangeReceiver implements AntPluginPcc.IDeviceStateChangeReceiver {
        private AntSensorType type;

        public AntDeviceChangeReceiver(AntSensorType type) {
            this.type = type;
        }

        @Override
        public void onDeviceStateChange(DeviceState newDeviceState) {
            String extraName = "unknown";

            if( type == AntSensorType.CyclingSpeed ) {
                extraName = "bsd_service_status";
                Log.d(TAG, "Speed sensor onDeviceStateChange: "+newDeviceState);
            } else if ( type == AntSensorType.CyclingCadence ) {
                extraName = "bc_service_status";
                Log.d(TAG, "Cadence sensor onDeviceStateChange: "+newDeviceState);
            } else if( type == AntSensorType.HeartRate ) {
                extraName = "hr_service_status";
                Log.d(TAG, "HeartRate sensor onDeviceStateChange: "+newDeviceState);
            }

            // send broadcast about device status
            Intent broadcastIntent = new Intent("com.fitstream.ANTDATA");
            broadcastIntent.putExtra(extraName, newDeviceState.name());
            sendBroadcast(broadcastIntent);

            // If ant device is dead
            if ( newDeviceState == DeviceState.DEAD ) {
                bsdPcc = null;
            }
        }
    }

    private AntPluginPcc.IDeviceStateChangeReceiver mBCDeviceStateChangeReceiver = new AntDeviceChangeReceiver(AntSensorType.CyclingCadence);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        Intent notificationIntent = new Intent(this, AntPlusService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT_IMPORTANCE, CHANNEL_DEFAULT_IMPORTANCE, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DEFAULT_IMPORTANCE);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);

            //Build a notification
            Notification notification =
                    new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                    .setContentTitle("FitStream")
                    .setContentText("Active")
                    .setContentIntent(pendingIntent)
                    .setTicker("FitStream")
                    .build();
            startForeground(ONGOING_NOTIFICATION_ID, notification);
        } else {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                    .setContentTitle("FitStream")
                    .setContentText("Active")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(ONGOING_NOTIFICATION_ID, notification);
        }

        return START_NOT_STICKY;
    }

    //initialise ant plus functionality
    private void initAntPlus() {
        //Release old access if it exists
        if (bsdReleaseHandle != null ) {
            bsdReleaseHandle.close();
        }
        if( bcReleaseHandle != null ) {
            bcReleaseHandle.close();
        }
        if (hrReleaseHandle != null ) {
            hrReleaseHandle.close();
        }

        Log.d(TAG, "Requesting ANT+ access...");

        //Start cadence sensor search
        bcReleaseHandle = AntPlusBikeCadencePcc.requestAccess(
                this,
                0,
                0,
                false,
                mBCResultReceiver,
                mBCDeviceStateChangeReceiver
        );

        //Send initial state for UI
        Intent initialAntDeviceSearchAction = new Intent("com.fitstream.ANTDATA");
        initialAntDeviceSearchAction.putExtra("bc_service_status", "SEARCHING");
        sendBroadcast(initialAntDeviceSearchAction);
    }
}
