package com.videostreamtest.ui.phone.catalog;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class CatalogViewModel extends AndroidViewModel {

    private SharedPreferences myPreferences;

    private MutableLiveData<String> apiKey;

    public CatalogViewModel(@NonNull Application application) {
        super(application);

        apiKey = new MutableLiveData<>();

        myPreferences = getApplication().getSharedPreferences("app",0);
        apiKey.setValue(myPreferences.getString("apiKey", null));
    }

    public LiveData<String> getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String key) {
        SharedPreferences.Editor editor = myPreferences.edit();
        editor.putString("apiKey", key);
        editor.commit();
        apiKey.setValue(key);
    }

}
