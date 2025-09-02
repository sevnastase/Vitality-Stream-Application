package com.videostreamtest.data.model.response;

import androidx.room.ColumnInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {
    private boolean localPlay = false;
    private boolean bootOnStart = false;
    private String communicationDevice = "BLE";
    private boolean updatePraxCloud = false;
    private String praxCloudMediaServerUrl;
    private String praxCloudMediaServerLocalUrl;
    @ColumnInfo(name = "account_type")
    private String accountType;

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

    public String getCommunicationDevice() {
        return communicationDevice;
    }

    public void setCommunicationDevice(String communicationDevice) {
        this.communicationDevice = communicationDevice;
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

    public String getPraxCloudMediaServerLocalUrl() {
        return praxCloudMediaServerLocalUrl;
    }

    public void setPraxCloudMediaServerLocalUrl(String praxCloudMediaServerLocalUrl) {
        this.praxCloudMediaServerLocalUrl = praxCloudMediaServerLocalUrl;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public static com.videostreamtest.config.entity.Configuration toDatabaseEntity(final Configuration configuration) {
        com.videostreamtest.config.entity.Configuration dbConfig = new com.videostreamtest.config.entity.Configuration();
        dbConfig.setPraxCloudMediaServerUrl(configuration.getPraxCloudMediaServerUrl());
        dbConfig.setPraxCloudMediaServerLocalUrl(configuration.getPraxCloudMediaServerLocalUrl());
        dbConfig.setUpdatePraxCloud(configuration.isUpdatePraxCloud());
        dbConfig.setLocalPlay(configuration.isLocalPlay());
        dbConfig.setBootOnStart(configuration.isBootOnStart());
        dbConfig.setCommunicationDevice(configuration.getCommunicationDevice());
        dbConfig.setAccountType(configuration.getAccountType());
        return dbConfig;
    }
}
