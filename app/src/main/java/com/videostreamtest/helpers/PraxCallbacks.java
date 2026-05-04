package com.videostreamtest.helpers;

public class PraxCallbacks {
    public interface SpeedtestCallback {
        void onSuccess(final long ping, final long downloadSpeedKbps);
        void onWarning(final long ping, final long downloadSpeedKbps);
        void onFailure(final long ping, final long downloadSpeedKbps);
    }

    public interface WifiCallback {
        void onSuccess(long value);
        void onError(Exception e);
    }

    public interface OnFailureCallback {
        void run();
    }
}
