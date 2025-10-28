package com.videostreamtest.ui.phone.helpers;

import android.content.Context;

import com.videostreamtest.utils.ApplicationSettings;

import javax.annotation.Nullable;

public class AccountHelper {
    public static String getAccountToken(@Nullable final Context context) {
        if (context == null) return null;
        return context.getApplicationContext().getSharedPreferences("app",0).getString("apikey", "unauthorized");
    }

    @Nullable
    public static String getAccountType(@Nullable final Context context) {
        if (context == null) return null;
        return context.getApplicationContext().getSharedPreferences("app",0).getString("account-type", "undefined");
    }

    public static boolean isLocalPlay(final Context context) {
        String accountType = getAccountType(context);
        return "standalone".equalsIgnoreCase(accountType)
                || "hybrid".equalsIgnoreCase(accountType);
    }

    public static String getAccountMediaServerUrl(final Context context) {
        return context.getApplicationContext().getSharedPreferences("app",0).getString("media-server-url", ApplicationSettings.PRAXCLOUD_MEDIA_URL);
    }

    public static Boolean isAccountBootable(final Context context) {
        return context.getApplicationContext().getSharedPreferences("app",0).getBoolean("bootable", false);
    }
}
