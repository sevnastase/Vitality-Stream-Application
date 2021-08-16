package com.videostreamtest.ui.phone.helpers;

public class BleHelper {
    public static String getRssiStrengthIndicator(final int rssi) {
        String indication = "No signal";
        int normalizedRssi = rssi * -1;
        if (normalizedRssi<=50) {
            indication = "Excellent";
        }
        if (normalizedRssi>50 && normalizedRssi <=60) {
            indication = "Very good";
        }
        if (normalizedRssi>60 && normalizedRssi <=70) {
            indication = "Good";
        }
        if (normalizedRssi>70 && normalizedRssi <=80) {
            indication = "Low";
        }
        if (normalizedRssi>80 && normalizedRssi <=90) {
            indication = "Very low";
        }
        return indication;
    }
}
