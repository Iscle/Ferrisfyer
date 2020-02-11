package me.iscle.ferrisfyer.model;

import com.google.gson.Gson;

import me.iscle.ferrisfyer.ServerManager;

public class WebSocketCapsule {
    private static final transient Gson gson = new Gson();

    private String token;
    private String command;
    private String data;

    public WebSocketCapsule(String command, Object data) {
        this.token = ServerManager.getInstance().getToken();
        this.command = command;
        this.data = gson.toJson(data);
    }

    public static WebSocketCapsule fromJson(String json) {
        return gson.fromJson(json, WebSocketCapsule.class);
    }

    public String getCommand() {
        return command;
    }

    public <T> T getData(Class<T> type) {
        return gson.fromJson(data, type);
    }

    public void updateToken() {
        this.token = ServerManager.getInstance().getToken();
    }

    public String toJson() {
        return gson.toJson(this);
    }
}
