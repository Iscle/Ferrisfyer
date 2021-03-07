package me.iscle.ferrisfyer.model.api;

public class AuthenticationRequest {
    CharSequence username;
    CharSequence password;

    public AuthenticationRequest(CharSequence username, CharSequence password) {
        this.username = username;
        this.password = password;
    }
}
