package com.videostreamtest.service.wifi;

public interface WifiCallback {
    void onSuccess(long value);
    void onError(Exception e);
}
