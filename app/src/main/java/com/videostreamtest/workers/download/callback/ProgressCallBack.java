package com.videostreamtest.workers.download.callback;

public interface ProgressCallBack {
    void callback(CallbackByteChannel rbc, double progress);
}
