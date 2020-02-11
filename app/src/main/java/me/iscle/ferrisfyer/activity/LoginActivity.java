package me.iscle.ferrisfyer.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import me.iscle.ferrisfyer.LoginCallback;
import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.ServerManager;

public class LoginActivity extends BaseAppCompatActivity {
    private EditText username;
    private EditText password;
    private CheckBox keepLoggedIn;
    private ProgressDialog progressDialog;

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setResult(RESULT_CANCELED);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        keepLoggedIn = findViewById(R.id.keep_logged_in);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        final Button loginButton = findViewById(R.id.login);
        loginButton.setOnClickListener(v -> doLogin());
        final Button registerButton = findViewById(R.id.register);
        registerButton.setOnClickListener(v -> doRegister());

        sharedPreferences = getSharedPreferences("me.iscle.ferrisfyer.LoginPreferences", Context.MODE_PRIVATE);
        username.setText(sharedPreferences.getString("username", null));
        password.setText(sharedPreferences.getString("password", null));
        keepLoggedIn.setChecked(sharedPreferences.getBoolean("keep_logged_in", false));

        if (sharedPreferences.getBoolean("keep_logged_in", false)) {
            doLogin();
        }
    }

    private void doLogin() {
        progressDialog.setTitle("Logging in...");
        progressDialog.show();
        ServerManager serverManager = ServerManager.getInstance();
        serverManager.login(username.getText().toString(), password.getText().toString(), new LoginCallback() {
            @Override
            public void onSuccess(String token) {
                onLoginSuccess();
            }

            @Override
            public void onError(String error) {
                onLoginError(error);
            }
        });
    }

    private void doRegister() {
        progressDialog.setTitle("Registering...");
        progressDialog.show();
        ServerManager serverManager = ServerManager.getInstance();
        serverManager.register(username.getText().toString(), password.getText().toString(), new LoginCallback() {
            @Override
            public void onSuccess(String token) {
                onLoginSuccess();
            }

            @Override
            public void onError(String error) {
                onLoginError(error);
            }
        });
    }

    private void onLoginSuccess() {
        progressDialog.dismiss();

        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("keep_logged_in", keepLoggedIn.isChecked());
        sharedPreferencesEditor.putString("username", username.getText().toString());
        if (keepLoggedIn.isChecked()) {
            sharedPreferencesEditor.putString("password", password.getText().toString());
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
}
