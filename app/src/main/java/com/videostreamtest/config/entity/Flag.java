package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "flags_table")
public class Flag {
    @PrimaryKey
    @ColumnInfo(name = "flag_id")
    @NonNull
    private Integer id;

    @ColumnInfo(name = "country_iso")
    @NonNull
    private String flagCountryIso;

    @ColumnInfo(name = "country_name")
    @NonNull
    private String flagCountryName;

    @ColumnInfo(name = "flag_filesize")
    @NonNull
    private Integer flagFilesize;

    @ColumnInfo(name = "flag_url")
    @NonNull
    private String flagUrl;

    @NonNull
    public Integer getId() {
        return id;
    }

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    @NonNull
    public String getFlagCountryIso() {
        return flagCountryIso;
    }

    public void setFlagCountryIso(@NonNull String flagCountryIso) {
        this.flagCountryIso = flagCountryIso;
    }

    @NonNull
    public String getFlagCountryName() {
        return flagCountryName;
    }

    public void setFlagCountryName(@NonNull String flagCountryName) {
        this.flagCountryName = flagCountryName;
    }

    @NonNull
    public Integer getFlagFilesize() {
        return flagFilesize;
    }

    public void setFlagFilesize(@NonNull Integer flagFilesize) {
        this.flagFilesize = flagFilesize;
    }

    @NonNull
    public String getFlagUrl() {
        return flagUrl;
    }

    public void setFlagUrl(@NonNull String flagUrl) {
        this.flagUrl = flagUrl;
    }
}
