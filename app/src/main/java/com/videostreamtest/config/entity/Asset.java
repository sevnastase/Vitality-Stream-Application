package com.videostreamtest.config.entity;

public class Asset {
    private Integer id;
    private String assetUrl;

    private Long assetSizeBytes;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAssetUrl() {
        return assetUrl;
    }

    public void setAssetUrl(String assetUrl) {
        this.assetUrl = assetUrl;
    }

    public Long getAssetSizeBytes() {
        return assetSizeBytes;
    }

    public void setAssetSizeBytes(Long assetSizeBytes) {
        this.assetSizeBytes = assetSizeBytes;
    }
}
