package com.videostreamtest.ui.phone.productview.layoutmanager;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;

public class PreCachingLayoutManager extends GridLayoutManager {
    private int defaultExtraLayoutSpace = 600;
    private int extraLayoutSpace = -1;

    public PreCachingLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public PreCachingLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    public PreCachingLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setExtraLayoutSpace(int extraLayoutSpace) {
        this.extraLayoutSpace = extraLayoutSpace;
    }

    public int getExtraLayoutSpace() {
        if (extraLayoutSpace > 0) {
            return extraLayoutSpace;
        } else {
            return defaultExtraLayoutSpace;
        }
    }
}
