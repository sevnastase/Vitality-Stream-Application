package com.videostreamtest.service.ant;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

public class AntDeviceChangeReceiver extends Service implements AntPluginPcc.IDeviceStateChangeReceiver {
    private static final String TAG = AntPlusService.class.getSimpleName();

    private AntSensorType antSensorType;

    public AntDeviceChangeReceiver(AntSensorType antSensorType) {
        this.antSensorType = antSensorType;
    }

    @Override
    public void onDeviceStateChange(DeviceState newDeviceState) {
        String extraName = "unknown";

        if( antSensorType == AntSensorType.CyclingSpeed ) {
            extraName = "bsd_service_status";
            Log.d(TAG, "Speed sensor onDeviceStateChange: "+newDeviceState);
        } else if ( antSensorType == AntSensorType.CyclingCadence ) {
            extraName = "bc_service_status";
            Log.d(TAG, "Cadence sensor onDeviceStateChange: "+newDeviceState);
        } else if( antSensorType == AntSensorType.HeartRate ) {
            extraName = "hr_service_status";
            Log.d(TAG, "HeartRate sensor onDeviceStateChange: "+newDeviceState);
        }

        // send broadcast about device status
        Intent broadcastIntent = new Intent("com.fitstream.ANTDATA");
        broadcastIntent.putExtra(extraName, newDeviceState.name());
        sendBroadcast(broadcastIntent);

//        if ( newDeviceState == DeviceState.DEAD ) {
//            bsdPcc = null;
//        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
