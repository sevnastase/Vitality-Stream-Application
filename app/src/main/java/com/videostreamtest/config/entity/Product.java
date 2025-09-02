package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "product_table")
public class Product {
    @PrimaryKey
    @NonNull
    private int uid;

    @ColumnInfo(name = "account_token")
    @NonNull
    private String accountToken;
    @ColumnInfo(name = "product_name")
    @NonNull
    private String productName;
    @ColumnInfo(name = "product_logo_path")
    @NonNull
    private String productLogoButtonPath;
    @ColumnInfo(name = "default_settings_id", defaultValue = "0")
    private Integer defaultSettingsId;
    @ColumnInfo(name = "product_blocked", defaultValue = "0")
    private Integer blocked;
    @ColumnInfo(name = "product_support_streaming", defaultValue = "0")
    private Integer supportStreaming;

    @ColumnInfo(name = "product_communication_type", defaultValue = "RPM")
    private String communicationType;

    @ColumnInfo(name = "product_type")
    private String productType;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getAccountToken() {
        return accountToken;
    }

    public void setAccountToken(String accountToken) {
        this.accountToken = accountToken;
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

    public String getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(String communicationType) {
        this.communicationType = communicationType;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }
}
