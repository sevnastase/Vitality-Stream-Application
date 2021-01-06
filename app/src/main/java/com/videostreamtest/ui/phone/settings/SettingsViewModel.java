package com.videostreamtest.ui.phone.settings;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SettingsViewModel extends AndroidViewModel {
    private SharedPreferences myPreferences;

    //Account Data
    private MutableLiveData<String> apiKey;

    //Ant Sensor Data
    private MutableLiveData<Boolean> antServiceSwitch;

    //Network Data
    private MutableLiveData<String> pingDescription;
    private MutableLiveData<String> pingValue;

    public SettingsViewModel(@NonNull Application application) {
        super(application);

        //Initiate private values
        apiKey = new MutableLiveData<>();
        antServiceSwitch = new MutableLiveData<>();
        pingDescription = new MutableLiveData<>();
        pingValue = new MutableLiveData<>();

        //Get app sharedPreferences
        myPreferences = getApplication().getSharedPreferences("app",0);

        //Load current shared preferences in private params
        antServiceSwitch.setValue(myPreferences.getBoolean("antServiceStatus", false));
        apiKey.setValue(myPreferences.getString("apiKey", null));
    }

    public LiveData<Boolean> getAntServiceSwitch() {
        if (antServiceSwitch.getValue() == null) {
            antServiceSwitch.setValue(false);
        }
        return antServiceSwitch;
    }

    public void setAntServiceSwitch(boolean antServiceStatus) {
        SharedPreferences.Editor editor = myPreferences.edit();
        editor.putBoolean("antServiceStatus", antServiceStatus);
        editor.commit();

        this.antServiceSwitch.setValue(antServiceStatus);
    }

    public LiveData<String> getPingDescription() {
        if(pingDescription.getValue() == null) {
            pingDescription.setValue("Host not available.");
        }
        return pingDescription;
    }

    public void setPingDescription(String pingDescription){
        this.pingDescription.setValue(pingDescription);
    }

    public LiveData<String> getPingValue() {
        if (pingValue.getValue() == null) {
            pingValue.setValue("0");
        }
        return pingValue;
    }

    public void setPingValue(String value) {
        pingValue.setValue(value);
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
