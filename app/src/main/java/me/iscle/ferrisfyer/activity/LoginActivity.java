package me.iscle.ferrisfyer.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import me.iscle.ferrisfyer.WebSocketManager;
import me.iscle.ferrisfyer.databinding.ActivityLoginBinding;
import me.iscle.ferrisfyer.model.WebSocketCapsule;

public class LoginActivity extends BaseActivity {
    private ActivityLoginBinding binding;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setResult(RESULT_CANCELED);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Connecting to server");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        binding.login.setOnClickListener(v -> doLogin());
        binding.register.setOnClickListener(v -> doRegister());

        sharedPreferences = getSharedPreferences("me.iscle.ferrisfyer.LoginPreferences", Context.MODE_PRIVATE);
        binding.username.setText(sharedPreferences.getString("username", null));
        binding.password.setText(sharedPreferences.getString("password", null));
        binding.keepLoggedIn.setChecked(sharedPreferences.getBoolean("keep_logged_in", false));

        if (!getFerrisfyer().getWebSocketManager().isOpened()) {
            progressDialog.show();
            getFerrisfyer().getWebSocketManager().openWebSocket(webSocketCallback);
            return;
        }

        tryAutoLogin();
    }

    private void tryAutoLogin() {
        if (sharedPreferences.getBoolean("keep_logged_in", false)) {
            doLogin();
        }
    }

    private void doLogin() {
        progressDialog.setTitle("Logging in...");
        progressDialog.show();
        getFerrisfyer().getWebSocketManager().send(WebSocketCapsule.getLoginJson(binding.username.getText().toString(), binding.password.getText().toString()));
    }

    private void doRegister() {
        progressDialog.setTitle("Registering...");
        progressDialog.show();
        getFerrisfyer().getWebSocketManager().send(WebSocketCapsule.getRegisterJson(binding.username.getText().toString(), binding.password.getText().toString()));
    }

    private void onLoginSuccess() {
        progressDialog.dismiss();

        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("keep_logged_in", binding.keepLoggedIn.isChecked());
        sharedPreferencesEditor.putString("username", binding.username.getText().toString());
        if (binding.keepLoggedIn.isChecked()) {
            sharedPreferencesEditor.putString("password", binding.password.getText().toString());
        } else {
            sharedPreferencesEditor.remove("password");
        }
        sharedPreferencesEditor.apply();

        setResult(RESULT_OK);
        finish();
    }

    private void onLoginError(String errorString) {
        progressDialog.dismiss();

        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("keep_logged_in", false);
        sharedPreferencesEditor.apply();

        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private final WebSocketManager.WebSocketCallback webSocketCallback = new WebSocketManager.WebSocketCallback() {
        @Override
        public void onConnect() {
            runOnUiThread(() -> {
                progressDialog.dismiss();
                tryAutoLogin();
            });
        }

        @Override
        public void onDisconnect() {
            runOnUiThread(() -> {
                onLoginError("Disconnected from the server!");
            });
        }

        @Override
        public void onAuthenticateResponse(boolean success, String message) {
            runOnUiThread(() -> {
                if (success) {
                    onLoginSuccess();
                } else {
                    onLoginError(message);
                }
            });
        }

        @Override
        public void onError(String error) {
            runOnUiThread(() -> {
                onLoginError(error);
            });
        }
    };
}
