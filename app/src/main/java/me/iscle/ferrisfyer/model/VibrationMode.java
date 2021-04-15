package me.iscle.ferrisfyer.model;

import com.google.gson.annotations.SerializedName;

public class VibrationMode {
    @SerializedName("name")
    private String name;

    @SerializedName("pattern")
    private byte[] pattern;

    public VibrationMode(String name, byte[] pattern) {
        this.name = name;
        this.pattern = pattern;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getPattern() {
        return pattern;
    }

    public void setPattern(byte[] pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return name;
    }
}
