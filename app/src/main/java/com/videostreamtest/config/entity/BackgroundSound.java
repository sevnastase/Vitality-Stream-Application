package com.videostreamtest.config.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "backgroundsound_table")
public class BackgroundSound {
    @PrimaryKey
    @ColumnInfo(name="bg_sound_id")
    private Integer bgSoundId;

    @ColumnInfo(name="sound_id")
    private Integer soundId;

    @ColumnInfo(name="sound_number")
    private Integer soundNumber;

    @ColumnInfo(name="movie_id")
    private Integer movieId;

    @ColumnInfo(name="framenumber")
    private Integer framenumber;

    @ColumnInfo(name="sound_url")
    private String soundUrl;

    public Integer getBgSoundId() {
        return bgSoundId;
    }

    public void setBgSoundId(Integer bgSoundId) {
        this.bgSoundId = bgSoundId;
    }

    public Integer getSoundId() {
        return soundId;
    }

    public void setSoundId(Integer soundId) {
        this.soundId = soundId;
    }

    public Integer getSoundNumber() {
        return soundNumber;
    }

    public void setSoundNumber(Integer soundNumber) {
        this.soundNumber = soundNumber;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Integer getFramenumber() {
        return framenumber;
    }

    public void setFramenumber(Integer framenumber) {
        this.framenumber = framenumber;
    }

    public String getSoundUrl() {
        return soundUrl;
    }

    public void setSoundUrl(String soundUrl) {
        this.soundUrl = soundUrl;
    }

}
