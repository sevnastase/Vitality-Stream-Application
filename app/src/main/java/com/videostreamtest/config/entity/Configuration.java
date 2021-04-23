package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "configuration_table")
public class Configuration {
    @PrimaryKey
    @NonNull
    private int uid;
    @ColumnInfo(name = "account_token", defaultValue = "unauthorized")
    private String accountToken;

    //Managed/Updated by PraxCloud
    @ColumnInfo(name = "local_play", defaultValue = "false")
    private boolean localPlay;
    @ColumnInfo(name = "boot_on_start", defaultValue = "false")
    private boolean bootOnStart;
    @ColumnInfo(name = "communication_device", defaultValue = "BLE")
    private String communicationDevice;
    @ColumnInfo(name = "update_praxcloud", defaultValue = "false")
    private boolean updatePraxCloud;
    @ColumnInfo(name = "praxcloud_mediaserver_url", defaultValue = "")
    private String praxCloudMediaServerUrl;

    //Managed by App
    @ColumnInfo(name = "is_current", defaultValue = "false")
    private boolean isCurrent;
    @ColumnInfo(name = "current_product", defaultValue = "None")
    private String currentProduct;
    @ColumnInfo(name = "current_profile", defaultValue = "None")
    private String currentLoadedProfile;
    @ColumnInfo(name = "product_count", defaultValue = "0")
    private int productCount;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public boolean isLocalPlay() {
        return localPlay;
    }

    public void setLocalPlay(boolean localPlay) {
        this.localPlay = localPlay;
    }

    public boolean isBootOnStart() {
        return bootOnStart;
    }

    public void setBootOnStart(boolean bootOnStart) {
        this.bootOnStart = bootOnStart;
    }

    public boolean isUpdatePraxCloud() {
        return updatePraxCloud;
    }

    public void setUpdatePraxCloud(boolean updatePraxCloud) {
        this.updatePraxCloud = updatePraxCloud;
    }

    public String getPraxCloudMediaServerUrl() {
        return praxCloudMediaServerUrl;
    }

    public void setPraxCloudMediaServerUrl(String praxCloudMediaServerUrl) {
        this.praxCloudMediaServerUrl = praxCloudMediaServerUrl;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    public String getCommunicationDevice() {
        return communicationDevice;
    }

    public void setCommunicationDevice(String communicationDevice) {
        this.communicationDevice = communicationDevice;
    }

    public String getCurrentProduct() {
        return currentProduct;
    }

    public void setCurrentProduct(String currentProduct) {
        this.currentProduct = currentProduct;
    }

    public String getCurrentLoadedProfile() {
        return currentLoadedProfile;
    }

    public void setCurrentLoadedProfile(String currentLoadedProfile) {
        this.currentLoadedProfile = currentLoadedProfile;
    }

    public String getAccountToken() {
        return accountToken;
    }

    public void setAccountToken(String accountToken) {
        this.accountToken = accountToken;
    }

    public int getProductCount() {
        return productCount;
    }

    public void setProductCount(int productCount) {
        this.productCount = productCount;
    }
}
