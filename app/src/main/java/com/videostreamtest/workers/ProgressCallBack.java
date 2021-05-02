package com.videostreamtest.workers;

public interface ProgressCallBack {
    void callback(CallbackByteChannel rbc, double progress);
}
