package me.iscle.ferrisfyer;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import me.iscle.ferrisfyer.model.WebSocketCapsule;
import me.iscle.ferrisfyer.model.api.AuthenticationRequest;
import me.iscle.ferrisfyer.model.api.AuthenticationResponse;
import me.iscle.ferrisfyer.network.FerrisfyerService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class ServerManager implements IDeviceControl {
    private static final String TAG = "ServerManager";

    private OkHttpClient client;
    private FerrisfyerService service;
    private WebSocket webSocket;
    private WebSocketCallback webSocketCallback;
    private boolean webSocketOpened;

    public ServerManager() {
        this.client = new OkHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ferrisfyer.selepdf.com/")
                .client(this.client)
                .build();

        this.service = retrofit.create(FerrisfyerService.class);
    }

    public void openWebSocket(WebSocketCallback callback) {
        this.webSocketCallback = callback;
        openWebSocket();
    }

    public void openWebSocket() {
        Request request = new Request.Builder()
                .url("wss://ferrisfyer.selepdf.com")
                .build();

        this.webSocket = client.newWebSocket(request, webSocketListener);
    }

    public void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Close requested by client.");
            webSocket = null;
        }
    }

    private final WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            Log.d(TAG, "WebSocket opened!");

            webSocketOpened = true;

            if (webSocketCallback != null) {
                webSocketCallback.onConnect();
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            Log.d(TAG, "New message: " + text);

            WebSocketCapsule capsule = WebSocketCapsule.fromJson(text);
            switch (WebSocketCapsule.Command.fromString(capsule.command)) {

            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            Log.d(TAG, "Closing. code: " + code + ", reason: " + reason);
            webSocketOpened = false;
            if (webSocketCallback != null) {
                webSocketCallback.onDisconnect();
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @org.jetbrains.annotations.Nullable Response response) {
            Log.d(TAG, "Failed: " + t.getMessage());
            webSocketOpened = false;
            if (webSocketCallback != null) {
                webSocketCallback.onError(t.getMessage());
            }
        }
    };

    public void stopRemoteControl() {
        if (webSocket != null) {
            webSocket.close(1000, "stopRemoteControl() called");
            webSocket = null;
        }
    }

    @Override
    public void startMotor(byte percent) {
        //WebSocketCapsule capsule = new WebSocketCapsule("SET_MOTOR_SPEED", new SetMotorSpeed(name, percent));
        //webSocket.send(capsule.toJson());
    }

    @Override
    public void startMotor(byte percent1, byte percent2) {
        //WebSocketCapsule capsule = new WebSocketCapsule("SET_DUAL_MOTOR_SPEED", new byte[] {percent1, percent2});
        //webSocket.send(capsule.toJson());
    }

    @Override
    public void stopMotor() {
        //WebSocketCapsule capsule = new WebSocketCapsule("STOP_MOTOR", new SetMotorSpeed(name, (byte) 0));
        //webSocket.send(capsule.toJson());
    }

    public void send(String data) {
        webSocket.send(data);
    }

    public void login(CharSequence username, CharSequence password, AuthenticationCallback callback) {
        service.login(new AuthenticationRequest(username, password)).enqueue(new Callback<AuthenticationResponse>() {
            @Override
            public void onResponse(Call<AuthenticationResponse> call, retrofit2.Response<AuthenticationResponse> response) {
                if (response.isSuccessful()) {
                    callback.onAuthenticationSuccess();
                } else {
                    callback.onAuthenticationError("Login failed! Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthenticationResponse> call, Throwable t) {
                callback.onAuthenticationError("Login failed! " + t.getMessage());
            }
        });
    }

    public void register(CharSequence username, CharSequence password, AuthenticationCallback callback) {
        service.register(new AuthenticationRequest(username, password)).enqueue(new Callback<AuthenticationResponse>() {
            @Override
            public void onResponse(Call<AuthenticationResponse> call, retrofit2.Response<AuthenticationResponse> response) {
                if (response.isSuccessful()) {
                    callback.onAuthenticationSuccess();
                } else {
                    callback.onAuthenticationError("Register failed! Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthenticationResponse> call, Throwable t) {
                callback.onAuthenticationError("Register failed! " + t.getMessage());
            }
        });
    }

    public interface WebSocketCallback {
        void onConnect();
        void onDisconnect();
        void onError(String error);
    }

    public interface AuthenticationCallback {
        void onAuthenticationSuccess();
        void onAuthenticationError(String error);
    }
}
