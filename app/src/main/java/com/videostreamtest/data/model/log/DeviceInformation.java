package com.videostreamtest.data.model.log;

public class DeviceInformation {
    private String objectType;
    private String release;
    private Integer sdk_int;
    private String manufacturer;
    private String hardware;
    private String device;
    private String board;
    private String brand;
    private String model;
    private String product;
    private Integer ramMemoryBytes;

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public Integer getSdk_int() {
        return sdk_int;
    }

    public void setSdk_int(Integer sdk_int) {
        this.sdk_int = sdk_int;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getHardware() {
        return hardware;
    }

    public void setHardware(String hardware) {
        this.hardware = hardware;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Integer getRamMemoryBytes() {
        return ramMemoryBytes;
    }

    public void setRamMemoryBytes(Integer ramMemoryBytes) {
        this.ramMemoryBytes = ramMemoryBytes;
    }
}
