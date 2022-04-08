package com.videostreamtest.ui.phone.helpers;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;

public class SoundHelper {
    final static String TAG = SoundHelper.class.getSimpleName();

    public static boolean hasSystemSound(final Context context) {
        final AudioManager audioManager =  (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }

    public static void setSystemSoundDefault(final Context context) {
        final AudioManager audioManager =  (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }
}
