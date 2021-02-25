package me.iscle.ferrisfyer;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import me.iscle.ferrisfyer.model.WebSocketCapsule;
import me.iscle.ferrisfyer.model.websocket.AuthenticationResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketManager implements IDeviceControl {
    private static final String TAG = "WebSocketManager";

    private WebSocketCallback callback;
    private WebSocket webSocket;
    private boolean opened;

    public void openWebSocket(WebSocketCallback callback) {
        this.callback = callback;
        openWebSocket();
    }

    public void openWebSocket() {
        Request request = new Request.Builder()
                .url("wss://ferrisfyer.selepdf.com")
                .build();

        this.webSocket = new OkHttpClient().newWebSocket(request, webSocketListener);
    }

    public void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Close requested by client.");
            webSocket = null;
        }
    }

    public boolean isOpened() {
        return opened;
    }

    private final WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            Log.d(TAG, "WebSocket opened!");

            opened = true;

            if (callback != null) {
                callback.onConnect();
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            Log.d(TAG, "New message: " + text);

            WebSocketCapsule capsule = WebSocketCapsule.fromJson(text);
            switch (WebSocketCapsule.Command.fromString(capsule.command)) {
                case REGISTER_RESPONSE:
                case LOGIN_RESPONSE:
                    if (callback != null) {
                        AuthenticationResponse response = capsule.getData(AuthenticationResponse.class);
                        callback.onAuthenticateResponse(response.status == 0, response.message);
                    }
                    break;
            }

        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            Log.d(TAG, "Closing. code: " + code + ", reason: " + reason);
            opened = false;
            if (callback != null) {
                callback.onDisconnect();
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @org.jetbrains.annotations.Nullable Response response) {
            Log.d(TAG, "Failed: " + t.getMessage());
            opened = false;
            if (callback != null) {
                callback.onError(t.getMessage());
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

    @Override
    public void onBright(byte b, byte[] data) {
        throw new RuntimeException("Unimplemented!");
    }

    @Override
    public void onLight(byte percent) {
        throw new RuntimeException("Unimplemented!");
    }

    @Override
    public void onLightEnd() {
        throw new RuntimeException("Unimplemented!");
    }

    public void send(String data) {
        webSocket.send(data);
    }

    public interface WebSocketCallback {
        void onConnect();
        void onDisconnect();
        void onAuthenticateResponse(boolean success, String message);
        void onError(String error);
    }
}
