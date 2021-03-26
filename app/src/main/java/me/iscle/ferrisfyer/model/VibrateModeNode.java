package me.iscle.ferrisfyer.model;

import com.google.gson.annotations.SerializedName;

public class VibrateModeNode {
    @SerializedName("operationMode")
    private String operationMode;

    @SerializedName("value")
    private int value;


    public VibrateModeNode(String operationMode, int value) {
        this.operationMode = operationMode;
        this.value = value;
    }

    public String getOperationMode() {
        return operationMode;
    }

    public int getValue() {
        return value;
    }
}
