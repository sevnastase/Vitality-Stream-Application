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
    }

    public static class SharedPreferences {
        public static final String STATE_DOWNLOADS_COMPLETED = "com.videostreamtest.STATE_DOWNLOADS_COMPLETED";
    }

    public static class ApkUpdate {
        public static final String ACTION_UPDATE = "com.videostreamtest.ACTION_UPDATE";
        public static final String EXTRA_PACKAGE_NAMES = "com.videostreamtest.EXTRA_PACKAGE_NAME";
        public static final String PRAXTOUR_LAUNCHER_PACKAGE_NAME = "com.remoteupdatemanager";
    }
}
