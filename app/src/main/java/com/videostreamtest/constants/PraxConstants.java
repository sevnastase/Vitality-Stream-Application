package com.videostreamtest.constants;

public class PraxConstants {
    public static class IntentExtra {
        public static final String EXTRA_ACCOUNT_TOKEN = "com.videostreamtest.EXTRA_ACCOUNT_TOKEN";
        /**
         * Only Praxtour Launcher is supposed to send an intent extra with this name, set to true.
         */
        public static final String EXTRA_FROM_LAUNCHER = "com.videostreamtest.EXTRA_FROM_LAUNCHER";
        /**
         * Only the download fragments are supposed to send an intent with this name, set to true.
         */
        public static final String EXTRA_FROM_DOWNLOADS = "com.videostreamtest.EXTRA_FROM_DOWNLOADS";
        public static final String EXTRA_LOGOUT = "com.videostreamtest.EXTRA_LOGOUT";
        public static final String EXTRA_LAUNCHER_UPDATE_CHECKED = "com.videostreamtest.EXTRA_FROM_UPDATE_ACTIVITY";
    }

    public static class SharedPreferences {
        public static final String STATE_DOWNLOADS_COMPLETED = "com.videostreamtest.STATE_DOWNLOADS_COMPLETED";
    }

    public static class ApkUpdate {
        public static final String EVENT_INSTALL_COMPLETE = "com.videostreamtest.EVENT_INSTALL_COMPLETE";
        public static final String PRAXTOUR_LAUNCHER_PACKAGE_NAME = "com.praxtourlauncher";
    }
    public static class DefaultValues {
        public static final String NO_APIKEY = "unauthorized";
    }
}
