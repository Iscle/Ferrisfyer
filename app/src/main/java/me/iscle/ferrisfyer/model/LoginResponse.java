package me.iscle.ferrisfyer.model;

public class LoginResponse {
    private int status;
    private String token;
    private String error;

    public int getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public String getError() {
        return error;
    }
}
