package me.iscle.ferrisfyer;

import android.content.Context;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;

import me.iscle.ferrisfyer.model.WebSocketCapsule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketManager implements IDeviceControl {
    private static final String TAG = "WebSocketManager";

    private WebSocket webSocket;
    private String name;
    private LocalBroadcastManager localBroadcastManager;
    private WebSocketCallback callback;

    public WebSocketManager(Context context, String name) {
        this.name = name;
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public void setCallback(WebSocketCallback callback) {
        this.callback = callback;
    }

    public void startRemoteControl() {
        stopRemoteControl();

        Request request = new Request.Builder()
                .url("wss://ferrisfyer.selepdf.com/")
                .build();

        OkHttpClient client = new OkHttpClient();
        webSocket = client.newWebSocket(request, webSocketListener);
        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    public void stopRemoteControl() {
        if (webSocket != null) {
            webSocket.close(1000, "stopRemoteControl() called");
            webSocket = null;
        }
    }

    private final WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            Log.d(TAG, "webSocketListener: onOpen()");
            if (callback != null) {
                callback.onConnect();
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            Log.d(TAG, "webSocketListener: onMessage: text = " + text);
            WebSocketCapsule capsule = WebSocketCapsule.fromJson(text);
            switch (capsule.getCommand()) {
                case "SET_MOTOR_SPEED":

                    break;
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            Log.d(TAG, "webSocketListener: onClosing: code = " + code + ", reason = " + reason);
            if (callback != null) {
                callback.onDisconnect();
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @org.jetbrains.annotations.Nullable Response response) {
            Log.d(TAG, "webSocketListener: onFailure: " + t.getMessage());
            if (callback != null) {
                callback.onError("onFailure()");
            }
        }
    };

    @Override
    public void startMotor(byte percent) {
        WebSocketCapsule capsule = new WebSocketCapsule("SET_MOTOR_SPEED", percent);
        webSocket.send(capsule.toJson());
    }

    @Override
    public void startMotor(byte percent1, byte percent2) {
        WebSocketCapsule capsule = new WebSocketCapsule("SET_DUAL_MOTOR_SPEED", new byte[] {percent1, percent2});
        webSocket.send(capsule.toJson());
    }

    @Override
    public void stopMotor() {
        WebSocketCapsule capsule = new WebSocketCapsule("STOP_MOTOR", null);
        webSocket.send(capsule.toJson());
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

    public interface WebSocketCallback {
        void onConnect();
        void onDisconnect();
        void onError(String error);
    }
}
