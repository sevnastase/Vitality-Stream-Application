package com.videostreamtest.utils;

import com.videostreamtest.enums.CommunicationDevice;

import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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

    /** The URL for the production REST API. */
    public static final String PRAXCLOUD_API_URL_PROD = "https://api.praxcloud.eu";
    /**
     * The URL for the test REST API.
     * <p>
     *     IMPORTANT: {@code localhost} would point to the device actually running the app.
     *     Here are the correct IPs for development/testing:
     *     <ul>
     *      <li> Running a VM on your PC/laptop: {@code 10.0.2.2}. </li>
     *      <li> Physical device: find the IPv4 address of your PC/laptop: {@code 192.168.x.x}. </li>
     *     </ul>
     *     <p> You can set the {@code port} to whichever you exposed for your test/dev backend. </p>
     * </p>
     */
    public static final String PRAXCLOUD_API_URL_TEST = "http://10.0.2.2:8081";

    public static final String PRAXCLOUD_API_URL = PRAXCLOUD_API_URL_TEST;
    public static final String PRAXCLOUD_MEDIA_URL = "https://media.praxcloud.eu";

    //STANDALONE
    public static final String DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER = "/Praxtour";
    public static final String DEFAULT_LOCAL_SOUND_STORAGE_FOLDER = "/Sound";
    public static final String DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER = "/Flags";
    public static final String DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER = "/Update";

    public static final long MINIMUM_DISK_SPACE_BYTES = 1024L*1024L*1024L*14L;

//    public static final BigDecimal SPEEDTEST_MINIMUM_SPEED = new BigDecimal(3072000);
    public static final BigDecimal SPEEDTEST_MINIMUM_SPEED = new BigDecimal(15000000);//25 Mbps

    public static final int ROUTEFILM_OVERVIEW_PAGESIZE = 12;
    public static final int NUMBER_OF_DOWNLOAD_RUNNERS = 4;
    public static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    public static final ThreadPoolExecutor THREAD_POOL_LOG_EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

    public static final int SCREENSAVER_TRIGGER_SECONDS = 30*60;
    public static boolean SCREENSAVER_ACTIVE = false;

    public static void setScreensaverActive(final boolean screensaverState) {
        SCREENSAVER_ACTIVE = screensaverState;
    }
}
