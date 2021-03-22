package com.videostreamtest.ui.phone.splash;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.data.model.response.Product;

import java.util.List;

public class SplashViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Product>> productList;
    private final MutableLiveData<String> apiKey;

    public SplashViewModel(@NonNull Application application) {
        super(application);
        productList = new MutableLiveData<>();
        apiKey = new MutableLiveData<>();
    }

    public void setProductList(final List<Product> updatedProductList) {
        productList.setValue(updatedProductList);
    }

    public LiveData<List<Product>> getProductList() {
        return productList;
    }

    public void setApiKey(final String apikey) {
        apiKey.setValue(apikey);
    }

    public LiveData<String> getApiKey() {
        return apiKey;
    }
}
