package com.videostreamtest.utils;

import java.util.HashMap;

public class RpmVectorLookupTable {
    private final static float pi = 3.14f;
    private final static float radiusWheelMeters = 1.0f;
    private final static float defaultSpeedKmh = 25.0f;

    //Lookuptable based on (Key, Value) as in (RPM, playbackspeed)
    final static HashMap<Integer, Float> lookupTable = new HashMap<>();

    public final static float getPlaybackspeed(int rpm) {
        RpmVectorLookupTable.calculateLookupTable();
        return lookupTable.get(rpm);
    }

    public final static float getPlaybackspeed(int rpm, float recordedSpeedKmh) {
        RpmVectorLookupTable.calculateLookupTable(recordedSpeedKmh);
        return lookupTable.get(rpm);
    }

    public final static float getDistanceSingleRpm(){
        return (2*pi) * radiusWheelMeters;
    }

    public static float getPlayBackSpeedFromKmh(final int kmh) {
        return kmh / defaultSpeedKmh;
    }

    //Calculated for a movie recorded with a default average speed of 25 kmh
    private final static void calculateLookupTable() {
        calculateLookupTable(defaultSpeedKmh);
    }

    private final static void calculateLookupTable(final float recordedSpeedKmh) {
        final float normalPlaybackspeedRpm = (25 / (3*pi*radiusWheelMeters)) * recordedSpeedKmh;
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
