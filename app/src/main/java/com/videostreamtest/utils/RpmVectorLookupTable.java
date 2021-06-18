package com.videostreamtest.utils;

import java.util.HashMap;

public class RpmVectorLookupTable {
    private final static float pi = 3.14f;
    /**
     * 0.74 = 23,5 * 3.14 (radius in inches * pi)
     * 0.83 = 26 * 3.14 -> based on an average wheel size of 26 inches
     */
    private final static float circumferenceWheelInches = 0.83f;
    private final static float defaultSpeedKmh = 25.0f;

    //Lookuptable based on (Key, Value) as in (RPM, playbackspeed)
    final static HashMap<Integer, Float> lookupTable = new HashMap<>();

    public final static float getPlaybackspeed(int rpm) {
        if (lookupTable.size() ==0) {
            RpmVectorLookupTable.calculateLookupTable();
        }
        return lookupTable.get(rpm);
    }

    public final static float getPlaybackspeed(int rpm, float recordedSpeedKmh) {
        RpmVectorLookupTable.calculateLookupTable(recordedSpeedKmh);
        return lookupTable.get(rpm);
    }

    public final static float getDistanceSingleRpm(){
        return (2*pi) * circumferenceWheelInches;
    }

    public static float getPlayBackSpeedFromKmh(final int kmh) {
        return kmh / defaultSpeedKmh;
    }

    //Calculated for a movie recorded with a default average speed of 25 kmh
    private final static void calculateLookupTable() {
        calculateLookupTable(defaultSpeedKmh);
    }

    private final static void calculateLookupTable(final float recordedSpeedKmh) {
        final float normalPlaybackspeedRpm = (25 / (3*pi* circumferenceWheelInches)) * recordedSpeedKmh;
        final float playbackSpeedPerRpm = 1.0f / normalPlaybackspeedRpm;

        for ( int rpm = 0; rpm < 256; rpm++ ) {
            if ( rpm == 0 ) {
                lookupTable.put(0,0.1f);
            } else {
                lookupTable.put(rpm, rpm*playbackSpeedPerRpm);
            }
        }
    }
}
