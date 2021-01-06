package com.videostreamtest.utils;

import java.util.HashMap;

public class RpmVectorLookupTable {

    //Lookuptable based on (Key, Value) as in (RPM, playbackspeed)
    final static HashMap<Integer, Float> lookupTable = new HashMap<>();

    public final static float getPlaybackspeed(int rpm) {
        RpmVectorLookupTable.calculateLookupTable();
        return lookupTable.get(rpm);
    }

    //Now calculated for a movie recorded at an average speed of 25 kmh
    private final static void calculateLookupTable() {
        final float pi = 3.14f;
        final float kmh = 25.0f;
        final float radiusWheelMeters = 1.0f;

        final float normalPlaybackspeedRpm = (25 / (3*pi*radiusWheelMeters)) * kmh;
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
