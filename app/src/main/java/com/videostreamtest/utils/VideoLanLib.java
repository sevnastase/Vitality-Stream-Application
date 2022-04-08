package com.videostreamtest.utils;

import android.content.Context;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;
import java.util.List;

public class VideoLanLib {
    private static VideoLanLib INSTANCE;
    private static LibVLC libVLC;
    private String info = "Initial info class";

    private VideoLanLib() {
    }

    public synchronized  static VideoLanLib getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new VideoLanLib();
        }

        return INSTANCE;
    }

    public static void setLibVlc(final LibVLC libVLC) {
        VideoLanLib.libVLC = libVLC;
    }

    public synchronized static LibVLC getLibVLC(final Context context) {
        if (VideoLanLib.libVLC == null) {
            VideoLanLib.libVLC = createLibVlc(context);
            return VideoLanLib.libVLC;
        }
        return VideoLanLib.libVLC;
    }

    public static void releaseResources() {
        if (VideoLanLib.libVLC != null) {
            VideoLanLib.libVLC.release();
        }
    }

    private synchronized static LibVLC createLibVlc(final Context context) {
        final List<String> args = new ArrayList<>();
//        args.add("-vvv");
        args.add("--sout-all");
        args.add("--aout=opensles");
//        args.add("--no-gnutls-system-trust"); //DISABLE WITH TLS TRUST ISSUES
        /*
        However this doesnt solve the casting problem, a certificate of google needs to be in order I guess
         */
//      args.add("--drop-late-frames");
        //LOCAL PLAY
        args.add("--file-caching=45000");
        args.add("--no-avcodec-hurry-up");//ATTEMPT TO SOLVE GREY SCREEN PROBLEM D67
        //STREAMING
        args.add("--network-caching=20000");

        final LibVLC libVLC = new LibVLC(context, args);
        return libVLC;
    }
}
