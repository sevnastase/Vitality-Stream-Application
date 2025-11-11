package com.videostreamtest.helpers;

public class DataHolder {
    private static DataHolder instance;

    private boolean motolifeConnected = false;

    public static DataHolder getInstance() {
        if (instance == null) {
            instance = new DataHolder();
        }

        return instance;
    }

    public void setMotolifeConnected(boolean value) {
        motolifeConnected = value;
    }

    public boolean isMotolifeConnected() {
        return motolifeConnected;
    }
}
