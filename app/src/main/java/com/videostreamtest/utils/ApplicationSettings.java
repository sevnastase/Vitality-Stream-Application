package com.videostreamtest.utils;

import com.videostreamtest.enums.CommunicationDevice;

public class ApplicationSettings {
    public final static boolean DEVELOPER_MODE = false;
    public final static CommunicationDevice SELECTED_COMMUNICATION_DEVICE = CommunicationDevice.BLE;
    public final static String COMMUNICATION_INTENT_FILTER = "com.fitstream.sensor.DATA";
}
