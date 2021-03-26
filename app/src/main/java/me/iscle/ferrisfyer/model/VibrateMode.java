package me.iscle.ferrisfyer.model;

import com.google.gson.annotations.SerializedName;

public class VibrateMode {
    @SerializedName("name")
    private String name;

    @SerializedName("modeActions")
    private VibrateModeNode[] modeActions;


    public VibrateMode(String name, VibrateModeNode[] modeActions) {
        this.name = name;
        this.modeActions = modeActions;
    }

    public String getName() {
        return name;
    }

    public VibrateModeNode[] getModeActions() {
        return modeActions;
    }
}
