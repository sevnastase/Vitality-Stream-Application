package com.videostreamtest.receiver.helper;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Handler;

public abstract class TaskTemplate implements Runnable {
    private final BroadcastReceiver.PendingResult pendingResult;
    private final Intent intent;
    private final Handler handler;

    private TaskTemplate(BroadcastReceiver.PendingResult pendingResult, Intent intent, Handler handler) {
        this.pendingResult = pendingResult;
        this.intent = intent;
        this.handler = handler;
    }

    @Override
    public abstract void run();
}
