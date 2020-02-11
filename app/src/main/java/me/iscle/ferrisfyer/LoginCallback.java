package me.iscle.ferrisfyer;

public interface LoginCallback {
    void onSuccess(String token);

    void onError(String error);
}
