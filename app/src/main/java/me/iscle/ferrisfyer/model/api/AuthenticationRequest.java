package me.iscle.ferrisfyer.model.api;

public class AuthenticationRequest {
    String username;
    String password;

    public AuthenticationRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
