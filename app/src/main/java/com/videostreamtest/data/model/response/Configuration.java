package com.videostreamtest.data.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {
    private boolean localPlay = false;
    private boolean bootOnStart = false;
    private String communicationDevice = "BLE";
    private boolean updatePraxCloud = false;

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
}
