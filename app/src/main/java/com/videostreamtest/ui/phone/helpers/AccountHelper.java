package com.videostreamtest.ui.phone.helpers;

import android.content.Context;

import com.videostreamtest.utils.ApplicationSettings;

public class AccountHelper {
    public static String getAccountToken(final Context context) {
        return context.getApplicationContext().getSharedPreferences("app",0).getString("apikey", "unauthorized");
    }

    public static String getAccountType(final Context context) {
        return context.getApplicationContext().getSharedPreferences("app",0).getString("account-type", "undefined");
    }

    public static boolean isLocalPlay(final Context context) {
        String accountType = getAccountType(context);
        return accountType.equalsIgnoreCase("standalone")
                || accountType.equalsIgnoreCase("hybrid");
    }

    public static String getAccountMediaServerUrl(final Context context) {
        return context.getApplicationContext().getSharedPreferences("app",0).getString("media-server-url", ApplicationSettings.PRAXCLOUD_MEDIA_URL);
    }

    public static Boolean isAccountBootable(final Context context) {
        return context.getApplicationContext().getSharedPreferences("app",0).getBoolean("bootable", false);
    }
}
