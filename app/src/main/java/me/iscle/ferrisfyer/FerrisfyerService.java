package me.iscle.ferrisfyer;

import me.iscle.ferrisfyer.model.LoginResponse;
import me.iscle.ferrisfyer.model.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FerrisfyerService {
    @POST("login")
    Call<LoginResponse> login(@Body User user);

    @POST("register")
    Call<LoginResponse> register(@Body User user);
}
