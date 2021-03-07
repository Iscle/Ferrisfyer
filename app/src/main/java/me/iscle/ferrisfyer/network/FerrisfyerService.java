package me.iscle.ferrisfyer.network;

import me.iscle.ferrisfyer.model.api.AuthenticationRequest;
import me.iscle.ferrisfyer.model.api.AuthenticationResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FerrisfyerService {
    @POST("auth/register")
    Call<AuthenticationResponse> register(@Body AuthenticationRequest authenticationRequest);

    @POST("auth/login")
    Call<AuthenticationResponse> login(@Body AuthenticationRequest authenticationRequest);
}
