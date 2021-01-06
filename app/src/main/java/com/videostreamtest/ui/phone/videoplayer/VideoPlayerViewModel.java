package com.videostreamtest.ui.phone.videoplayer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class VideoPlayerViewModel extends AndroidViewModel {

    //Minimum Video you want to buffer while Playing
    private MutableLiveData<Integer> MIN_BUFFER_DURATION = new MutableLiveData<>();
    //Max Video you want to buffer during PlayBack
    private MutableLiveData<Integer>  MAX_BUFFER_DURATION = new MutableLiveData<>();
    //Min Video you want to buffer before start Playing it
    private MutableLiveData<Integer> MIN_PLAYBACK_START_BUFFER = new MutableLiveData<>();
    //Min video You want to buffer when user resumes video
    private MutableLiveData<Integer> MIN_PLAYBACK_RESUME_BUFFER = new MutableLiveData<>();

    public VideoPlayerViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Integer> getMinimalBufferDuration() {
        if (MIN_BUFFER_DURATION.getValue() == null) {
            MIN_BUFFER_DURATION.setValue(0);
        }
        return MIN_BUFFER_DURATION;
    }

}
