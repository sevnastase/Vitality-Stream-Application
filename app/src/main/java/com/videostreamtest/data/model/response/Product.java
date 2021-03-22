package com.videostreamtest.data.model.response;

public class Product {
    private Integer id;
    private String productName;
    private String productLogoButtonPath;
    private Integer defaultSettingsId;
    private Integer blocked =0;
    private Integer supportStreaming =0;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductLogoButtonPath() {
        return productLogoButtonPath;
    }

    public void setProductLogoButtonPath(String productLogoButtonPath) {
        this.productLogoButtonPath = productLogoButtonPath;
    }

    public Integer getDefaultSettingsId() {
        return defaultSettingsId;
    }

    public void setDefaultSettingsId(Integer defaultSettingsId) {
        this.defaultSettingsId = defaultSettingsId;
    }

    public Integer getBlocked() {
        return blocked;
    }

    public void setBlocked(Integer blocked) {
        this.blocked = blocked;
    }

    public Integer getSupportStreaming() {
        return supportStreaming;
    }

    public void setSupportStreaming(Integer supportStreaming) {
        this.supportStreaming = supportStreaming;
    }
}
