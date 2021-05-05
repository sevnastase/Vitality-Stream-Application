package com.videostreamtest.ui.phone.helpers;

import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.enums.CommunicationType;

import java.util.List;

public class ProductHelper {
    private static final String TAG = ProductHelper.class.getSimpleName();

    public static CommunicationType getCommunicationType(final String productCommunicationType) {
        for (CommunicationType communicationType : CommunicationType.values()) {
            if (productCommunicationType.equalsIgnoreCase(communicationType.name())) {
                return communicationType;
            }
        }
        return CommunicationType.RPM;
    }
}
