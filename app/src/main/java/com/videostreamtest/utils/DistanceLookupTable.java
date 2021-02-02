package com.videostreamtest.utils;

public class DistanceLookupTable {
    private final static float framesPerSecond = 30f;

    public static float getMeterPerSecond(final float totalMeters, final float movieLengthInSeconds) {
        final float meterPerFrame = getMeterPerFrame(totalMeters, movieLengthInSeconds);
        return meterPerFrame * framesPerSecond;
    }

    private static float getMeterPerFrame(final float totalMeters, final float movieLengthInSeconds){
        return totalMeters / getTotalFramesInMovie(movieLengthInSeconds);
    }

    private static float getTotalFramesInMovie(final float movieLengthInSeconds) {
        return movieLengthInSeconds*framesPerSecond;
    }
}
