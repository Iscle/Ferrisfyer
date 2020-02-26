package me.iscle.ferrisfyer.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonParseException;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.model.Device;
import me.iscle.ferrisfyer.model.SetMotorSpeed;
import me.iscle.ferrisfyer.model.WebSocketCapsule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class RemoteControlActivity extends AppCompatActivity {
    private static final String TAG = "RemoteControlActivity";

    private long mBackPressed;
    private WebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);

        startRemoteControl();

        findViewById(R.id.button5).setOnClickListener(v -> webSocket.send(new WebSocketCapsule("SET_MOTOR_SPEED", new SetMotorSpeed("iscle", (byte) 100)).toJson()));
    }

    @Override
    protected void onDestroy() {
        stopRemoteControl();
        super.onDestroy();
    }

    private final WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            Log.d(TAG, "webSocketListener: onOpen");
            webSocket.send(new WebSocketCapsule("SET_LOCAL", false).toJson());
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            Log.d(TAG, "webSocketListener: onMessage: " + text);
            try {
                WebSocketCapsule wsc = WebSocketCapsule.fromJson(text);
                switch (wsc.getCommand()) {
                    case "SET_DATA":
                        Device device = wsc.getData(Device.class);
                        Log.d(TAG, "onMessage: " + device.toString());
                        break;
                }
            } catch (JsonParseException e) {
                Log.e(TAG, "webSocketListener: onMessage: ", e);
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            Log.d(TAG, "webSocketListener: onClosing");
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @org.jetbrains.annotations.Nullable Response response) {
            Log.d(TAG, "webSocketListener: onFailure: " + t.getMessage());
        }
    };

    public void startRemoteControl() {
        stopRemoteControl();

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url("wss://ferrisfyer.selepdf.com/")
                .build();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.remote_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.log_out) {
            SharedPreferences sharedPreferences = getSharedPreferences("me.iscle.ferrisfyer.LoginPreferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putBoolean("keep_logged_in", false);
            sharedPreferencesEditor.remove("password");
            sharedPreferencesEditor.apply();
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        if (mBackPressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(this, "Tap back again to exit", Toast.LENGTH_SHORT).show();
        }

        mBackPressed = System.currentTimeMillis();
    }
}
