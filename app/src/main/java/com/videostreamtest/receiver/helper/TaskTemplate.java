package com.videostreamtest.receiver.helper;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Handler;

public abstract class TaskTemplate implements Runnable {
    protected final BroadcastReceiver.PendingResult pendingResult;
    protected final Intent intent;
    protected final Handler handler;

    protected TaskTemplate(BroadcastReceiver.PendingResult pendingResult, Intent intent, Handler handler) {
        this.pendingResult = pendingResult;
        this.intent = intent;
        this.handler = handler;
    }

    @Override
    public abstract void run();
}
