package com.videostreamtest.utils;

import com.videostreamtest.enums.CommunicationDevice;

public class ApplicationSettings {
    public final static boolean DEVELOPER_MODE = false;
    public final static boolean START_ON_BOOT = true;
    public final static CommunicationDevice SELECTED_COMMUNICATION_DEVICE = CommunicationDevice.BLE;

    public final static String COMMUNICATION_INTENT_FILTER = "com.fitstream.sensor.DATA";
    public static final String PRAXCLOUD_URL = "http://188.166.100.139:8080";
    public static final String DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER = "/Praxtour";
    public static final String DEFAULT_LOCAL_SOUND_STORAGE_FOLDER = "/Sound";
    public static final String DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER = "/Update";

    public static final int SCREENSAVER_TRIGGER_SECONDS = 30*60; //30*60;
    public static boolean SCREENSAVER_ACTIVE = false;

    public static void setScreensaverActive(final boolean screensaverState) {
        SCREENSAVER_ACTIVE = screensaverState;
    }
}
