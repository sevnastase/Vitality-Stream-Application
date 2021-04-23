package com.videostreamtest.ui.phone.helpers;

import com.videostreamtest.enums.CommunicationDevice;

public class ConfigurationHelper {
    public static CommunicationDevice getCommunicationDevice(final String communicationDevice) {
        for(CommunicationDevice device : CommunicationDevice.values()){
            if(communicationDevice.equalsIgnoreCase(device.name())){
                return device;
            }
        }
        return CommunicationDevice.NONE;
    }
}
