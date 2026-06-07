package com.videostreamtest.data.model.response;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.videostreamtest.helpers.AccountHelper;

import java.io.File;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    private Integer id;
    private String productName;
    private String productLogoButtonPath;
    private Integer defaultSettingsId;
    private Integer blocked =0;
    private Integer supportStreaming =0;
    private String communicationType = "RPM";
    private String productType;

    /** Added to use for LocalPlay accounts, instead of relying on Picasso caching. */
    private String productLogoLocalPath;

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

    public String getProductLogoLocalPath() {
        return productLogoLocalPath;
    }

    public void setProductLogoLocalPath(String productLogoLocalPath) {
        this.productLogoLocalPath = productLogoLocalPath;
    }

    public static Product fromProductEntity(final com.videostreamtest.config.entity.Product productEntity) {
        Product product = new Product();
        product.setId(productEntity.getUid());
        product.setCommunicationType(productEntity.getCommunicationType());
        product.setProductName(productEntity.getProductName());
        product.setProductLogoButtonPath(productEntity.getProductLogoButtonPath());
        product.setBlocked(productEntity.getBlocked());
        product.setSupportStreaming(productEntity.getSupportStreaming());
        product.setDefaultSettingsId(productEntity.getDefaultSettingsId());
        product.setProductType(productEntity.getProductType());
        product.setProductLogoLocalPath(productEntity.getProductLogoLocalPath());
        return product;
    }

    /** @return local path if the account is local play, cloud URL otherwise. */
    public Uri getAppropriateProductLogoPath(@NonNull Context context) {
        if (AccountHelper.isLocalPlay(context)) {
            return Uri.fromFile(new File(getProductLogoLocalPath()));
        } else {
            return Uri.parse(getProductLogoButtonPath());
        }
    }
}
