package com.videostreamtest.ui.phone.helpers;

import android.content.Context;

import com.videostreamtest.R;

public class BleHelper {
    public static String getRssiStrengthIndicator(final Context context, final int rssi) {
        String indication = context.getString(R.string.signal_strength_indicator_no_signal_value);
        int normalizedRssi = rssi * -1;
        if (normalizedRssi<=50) {
            indication = context.getString(R.string.signal_strength_indicator_excellent_signal_value);
        }
        if (normalizedRssi>50 && normalizedRssi <=60) {
            indication = context.getString(R.string.signal_strength_indicator_very_good_signal_value);
        }
        if (normalizedRssi>60 && normalizedRssi <=70) {
            indication = context.getString(R.string.signal_strength_indicator_good_signal_value);
        }
        if (normalizedRssi>70 && normalizedRssi <=80) {
            indication = context.getString(R.string.signal_strength_indicator_low_signal_value);
        }
        if (normalizedRssi>80 && normalizedRssi <=90) {
            indication = context.getString(R.string.signal_strength_indicator_very_low_signal_value);
        }
        return indication;
    }
}
