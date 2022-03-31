package com.videostreamtest.ui.phone.productview.layoutmanager;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomGridLayoutManager extends GridLayoutManager {
    public CustomGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

//    @Override
//    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler, RecyclerView.State state) {
//        View nextFocus = super.onFocusSearchFailed(focused, focusDirection, recycler, state);
//
//        if (nextFocus == null) {
//            return null;
//        }
//
//        try {
//            int fromPos = getPosition(focused);
//            int nextPos = getNextViewPos(fromPos, focusDirection);
//
//            return findViewByPosition(nextPos);
//        } catch (ClassCastException classCastException) {
//            Log.e(getClass().getSimpleName(), classCastException.getLocalizedMessage());
//            return null;
//        }
//    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            Log.e(getClass().getSimpleName(), indexOutOfBoundsException.getLocalizedMessage());
        }
    }

//    protected int getNextViewPos(int fromPos, int direction) {
//        int offset = calcOffsetToNextView(direction);
//
//        if (hitBorder(fromPos, offset)) {
//            return fromPos;
//        }
//
//        return fromPos + offset;
//    }
//
//    protected int calcOffsetToNextView(int direction) {
//        int spanCount = getSpanCount();
//        int orientation = getOrientation();
//
//        if (orientation == RecyclerView.VERTICAL) {
//            switch (direction) {
//                case View.FOCUS_DOWN:
//                    return spanCount;
//                case View.FOCUS_UP:
//                    return -spanCount;
//                case View.FOCUS_RIGHT:
//                    return 1;
//                case View.FOCUS_LEFT:
//                    return -1;
//            }
//        } else if (orientation == RecyclerView.HORIZONTAL) {
//            switch (direction) {
//                case View.FOCUS_DOWN:
//                    return 1;
//                case View.FOCUS_UP:
//                    return -1;
//                case View.FOCUS_RIGHT:
//                    return spanCount;
//                case View.FOCUS_LEFT:
//                    return -spanCount;
//            }
//        }
//        return 0;
//    }
//
//    private boolean hitBorder(int from, int offset) {
//        int spanCount = getSpanCount();
//
//        if( Math.abs(offset) == 1 ) {
//            int spanIndex = from % spanCount;
//            int newSpanIndex = spanIndex + offset;
//            return newSpanIndex <  0 || newSpanIndex>= spanCount;
//        } else {
//            int newPos = from + offset;
//            return newPos < 0 && newPos >= spanCount;
//        }
//    }
}
