package com.videostreamtest.data.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SoundItem {
    private Integer soundNumber;
    private String soundUrl;
    private Integer background;

    public Integer getSoundNumber() {
        return soundNumber;
    }

    public void setSoundNumber(Integer soundNumber) {
        this.soundNumber = soundNumber;
    }

    public String getSoundUrl() {
        return soundUrl;
    }

    public void setSoundUrl(String soundUrl) {
        this.soundUrl = soundUrl;
    }

    public Integer getBackground() {
        return background;
    }

    public void setBackground(Integer background) {
        this.background = background;
    }
}
