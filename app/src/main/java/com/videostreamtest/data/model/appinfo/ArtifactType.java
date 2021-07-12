package com.videostreamtest.data.model.appinfo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ArtifactType {

    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("kind")
    @Expose
    private String kind;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

}