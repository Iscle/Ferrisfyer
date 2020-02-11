package me.iscle.ferrisfyer;

import me.iscle.ferrisfyer.model.LoginResponse;
import me.iscle.ferrisfyer.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerManager {
    private static ServerManager instance;

    private FerrisfyerService service;

    private String token;

    private ServerManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ferrisfyer.selepdf.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(FerrisfyerService.class);
    }

    public static ServerManager getInstance() {
        if (instance == null) {
            synchronized (ServerManager.class) {
                if (instance == null) {
                    instance = new ServerManager();
                }
            }
        }

        return instance;
    }

    public String getToken() {
        return token;
    }

    public void login(String username, String password, LoginCallback callback) {
        service.login(new User(username, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus() == 0) {
                        token = response.body().getToken();
                        if (callback != null) {
                            callback.onSuccess(response.body().getToken());
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(response.body().getError());
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Unexpected error!");
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    public void register(String username, String password, LoginCallback callback) {
        service.register(new User(username, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus() == 0) {
                        token = response.body().getToken();
                        if (callback != null) {
                            callback.onSuccess(response.body().getToken());
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(response.body().getError());
                        }
                    }
                } else {
                    if (callback != null) {
                        if (response.body().getError() != null) {
                            callback.onError(response.body().getError());
                        } else {
                            callback.onError("Unexpected error!");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }
}
