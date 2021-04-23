package com.videostreamtest.workers;

public interface ProgressCallBack {
    void callback(DownloadServiceWorker.CallbackByteChannel rbc, double progress);
}
