package com.videostreamtest.ui.phone.profiles;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class ProfileViewModel extends AndroidViewModel {

    private final MutableLiveData<String> profileName = new MutableLiveData<>();
    private final MutableLiveData<String> imgPath = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<String> getProfileName() {
        return profileName;
    }

    public MutableLiveData<String> getImgPath() {
        return imgPath;
    }
}
