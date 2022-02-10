package com.videostreamtest.utils;

import com.videostreamtest.enums.CommunicationDevice;

import java.math.BigDecimal;

public class ApplicationSettings {
    public final static boolean DEVELOPER_MODE = false;
    public final static boolean START_ON_BOOT = true;
    public final static CommunicationDevice SELECTED_COMMUNICATION_DEVICE = CommunicationDevice.BLE;

    public final static String DEFAULT_BLE_DEVICE_KEY = "BLE_DEVICE_ADDRESS";
    public final static String DEFAULT_BLE_DEVICE_NAME_KEY = "BLE_DEVICE_NAME";
    public static final String DEFAULT_BLE_DEVICE_CONNECTION_STRENGTH_KEY = "BLE_DEVICE_CONNECTION_STRENGTH";

    public final static int DEFAULT_SOUND_VOLUME = 80;

    public final static float RECOMMENDED_DENSITY_DPI = 320;

    //INTERNAL (BUS) COMMUNICATION
    public final static String COMMUNICATION_INTENT_FILTER = "com.fitstream.sensor.DATA";

    public static final String PRAXCLOUD_URL = "https://api.praxcloud.eu:8443";

    //STANDALONE
    public static final String DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER = "/Praxtour";
    public static final String DEFAULT_LOCAL_SOUND_STORAGE_FOLDER = "/Sound";
    public static final String DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER = "/Flags";
    public static final String DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER = "/Update";

    public static final long MINIMUM_DISK_SPACE_BYTES = 1024L*1024L*1024L*64L;

//    public static final BigDecimal SPEEDTEST_MINIMUM_SPEED = new BigDecimal(3072000);
    public static final BigDecimal SPEEDTEST_MINIMUM_SPEED = new BigDecimal(30000000);//30 Mbps

    public static final int SCREENSAVER_TRIGGER_SECONDS = 30*60;
    public static boolean SCREENSAVER_ACTIVE = false;

    public static void setScreensaverActive(final boolean screensaverState) {
        SCREENSAVER_ACTIVE = screensaverState;
    }
}
