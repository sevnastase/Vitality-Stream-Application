package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profile_table")
public class Profile {
//    @PrimaryKey
//    @NonNull
//    private int uid;

    @PrimaryKey
    @ColumnInfo(name = "profile_id")
    @NonNull
    private Integer profileId;

    @ColumnInfo(name = "account_token")
    @NonNull
    private String accountToken;

    @ColumnInfo(name = "profile_img_path")
    @NonNull
    private String profileImgPath;

    @ColumnInfo(name = "profile_name")
    @NonNull
    private String profileName;

    @ColumnInfo(name = "profile_token")
    @NonNull
    private String profileKey;

    @ColumnInfo(name = "profile_blocked", defaultValue = "0")
    @NonNull
    private Integer blocked;

    @ColumnInfo(name = "profile_selected", defaultValue = "false")
    private boolean selected;

    @NonNull
    public String getAccountToken() {
        return accountToken;
    }

    public void setAccountToken(@NonNull String accountToken) {
        this.accountToken = accountToken;
    }

    @NonNull
    public Integer getProfileId() {
        return profileId;
    }

    public void setProfileId(@NonNull Integer profileId) {
        this.profileId = profileId;
    }

    @NonNull
    public String getProfileImgPath() {
        return profileImgPath;
    }

    public void setProfileImgPath(@NonNull String profileImgPath) {
        this.profileImgPath = profileImgPath;
    }

    @NonNull
    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(@NonNull String profileName) {
        this.profileName = profileName;
    }

    @NonNull
    public String getProfileKey() {
        return profileKey;
    }

    public void setProfileKey(@NonNull String profileKey) {
        this.profileKey = profileKey;
    }

    @NonNull
    public Integer getBlocked() {
        return blocked;
    }

    public void setBlocked(@NonNull Integer blocked) {
        this.blocked = blocked;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
