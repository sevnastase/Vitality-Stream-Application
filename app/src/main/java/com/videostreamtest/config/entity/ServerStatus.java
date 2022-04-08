package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "serverstatus_table")
public class ServerStatus {
    @PrimaryKey
    @ColumnInfo(name = "serverstatus_id")
    @NonNull
    private Integer serverstatusId;

    @ColumnInfo(name = "server_online")
    private boolean serverOnline;

    @ColumnInfo(name = "server_last_online_timestamp")
    private Date serverLastOnline;

    @NonNull
    public Integer getServerstatusId() {
        return serverstatusId;
    }

    public void setServerstatusId(@NonNull Integer serverstatusId) {
        this.serverstatusId = serverstatusId;
    }

    public boolean isServerOnline() {
        return serverOnline;
    }

    public void setServerOnline(boolean serverOnline) {
        this.serverOnline = serverOnline;
    }

    public Date getServerLastOnline() {
        return serverLastOnline;
    }

    public void setServerLastOnline(Date serverLastOnline) {
        this.serverLastOnline = serverLastOnline;
    }
}
