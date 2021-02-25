package me.iscle.ferrisfyer.model;

import com.google.gson.Gson;

import me.iscle.ferrisfyer.model.websocket.AuthenticationRequest;

public class WebSocketCapsule {
    private static final transient Gson gson = new Gson();

    public String command;
    public String data;

    public <T> T getData(Class<T> classOfT) {
        return gson.fromJson(data, classOfT);
    }

    public static String getRegisterJson(String username, String password) {
        WebSocketCapsule capsule = new WebSocketCapsule();
        capsule.command = Command.REGISTER_REQUEST.string;
        capsule.data = gson.toJson(new AuthenticationRequest(username, password));
        return gson.toJson(capsule);
    }

    public static String getLoginJson(String username, String password) {
        WebSocketCapsule capsule = new WebSocketCapsule();
        capsule.command = Command.LOGIN_REQUEST.string;
        capsule.data = gson.toJson(new AuthenticationRequest(username, password));
        return gson.toJson(capsule);
    }

    public static WebSocketCapsule fromJson(String json) {
        return gson.fromJson(json, WebSocketCapsule.class);
    }

    public enum Command {
        REGISTER_REQUEST("register_request"),
        REGISTER_RESPONSE("register_response"),
        LOGIN_REQUEST("login_request"),
        LOGIN_RESPONSE("login_response");

        public final String string;

        Command(String string) {
            this.string = string;
        }

        public static Command fromString(String string) {
            switch (string) {
                case "register_request":
                    return REGISTER_REQUEST;
                case "register_response":
                    return REGISTER_RESPONSE;
                case "login_request":
                    return LOGIN_REQUEST;
                case "login_response":
                    return LOGIN_RESPONSE;
            }

            return null;
        }
    }
}
