package com.videostreamtest.ui.phone.listeners;

import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

public class PraxFormOnEditorActionListener implements TextView.OnEditorActionListener {

    private Button callOnClickButton;

    public PraxFormOnEditorActionListener(final Button callOnClickButton) {
        this.callOnClickButton = callOnClickButton;
        Log.d(getClass().getSimpleName(), "OnEditorListener: "+this.callOnClickButton.hasOnClickListeners());
    }

    public PraxFormOnEditorActionListener() {
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean handled = false;
        Log.d(getClass().getSimpleName(), "ACTION RECEIVED: "+actionId);
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            Log.d(getClass().getSimpleName(), "ACTION_SEND RECEIVED");
            if (callOnClickButton!=null) {
                Log.d(getClass().getSimpleName(), "Button called.");
                handled = callOnClickButton.callOnClick();
            }
        }
        return handled;
    }
}
