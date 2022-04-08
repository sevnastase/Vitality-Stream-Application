package com.videostreamtest.ui.phone.productview.layoutmanager;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FocusFixFrameLayout extends FrameLayout {
    public FocusFixFrameLayout(@NonNull Context context) {
        super(context);
    }

    public FocusFixFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusFixFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FocusFixFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void clearFocus() {
        if(this.getParent() != null) {
            super.clearFocus();
        }
    }
}
