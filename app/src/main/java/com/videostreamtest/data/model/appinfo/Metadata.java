package com.videostreamtest.data.model.appinfo;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Metadata {

    @SerializedName("version")
    @Expose
    private Integer version;
    @SerializedName("artifactType")
    @Expose
    private ArtifactType artifactType;
    @SerializedName("applicationId")
    @Expose
    private String applicationId;
    @SerializedName("variantName")
    @Expose
    private String variantName;
    @SerializedName("elements")
    @Expose
    private List<Element> elements = null;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

}