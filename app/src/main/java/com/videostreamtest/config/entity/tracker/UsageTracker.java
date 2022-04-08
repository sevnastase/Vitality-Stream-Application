package com.videostreamtest.config.entity.tracker;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usage_tracker_table")
public class UsageTracker {
    @PrimaryKey
    @ColumnInfo(name = "tracker_accountoken")
    @NonNull
    private String accounttoken;

    @ColumnInfo(name = "selected_product")
    @NonNull
    private Integer selectedProduct;

    @ColumnInfo(name = "selected_movie")
    @NonNull
    private Integer selectedMovie;

    @ColumnInfo(name = "selected_background_sound")
    @NonNull
    private Integer selectedBackgroundSound;

    @ColumnInfo(name = "selected_profile")
    @NonNull
    private Integer selectedProfile;

    @NonNull
    public String getAccounttoken() {
        return accounttoken;
    }

    public void setAccounttoken(@NonNull String accounttoken) {
        this.accounttoken = accounttoken;
    }

    @NonNull
    public Integer getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(@NonNull Integer selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    @NonNull
    public Integer getSelectedMovie() {
        return selectedMovie;
    }

    public void setSelectedMovie(@NonNull Integer selectedMovie) {
        this.selectedMovie = selectedMovie;
    }

    @NonNull
    public Integer getSelectedBackgroundSound() {
        return selectedBackgroundSound;
    }

    public void setSelectedBackgroundSound(@NonNull Integer selectedBackgroundSound) {
        this.selectedBackgroundSound = selectedBackgroundSound;
    }

    @NonNull
    public Integer getSelectedProfile() {
        return selectedProfile;
    }

    public void setSelectedProfile(@NonNull Integer selectedProfile) {
        this.selectedProfile = selectedProfile;
    }
}
