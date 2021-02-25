package me.iscle.ferrisfyer.activity;

import android.app.ProgressDialog;
import android.os.Bundle;

import me.iscle.ferrisfyer.databinding.ActivityRegisterBinding;
import me.iscle.ferrisfyer.model.WebSocketCapsule;

public class RegisterActivity extends BaseActivity {
    private ActivityRegisterBinding binding;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setResult(RESULT_CANCELED);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Connecting to server");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        binding.register.setOnClickListener(v -> doRegister());
        binding.goToLoginTv.setOnClickListener(v -> goToLogin());
    }

    private void goToLogin() {
        finish();
    }

    private void doRegister() {
        // TODO: MIRAR QUE ELS DOS CAMPS DE PASSWORD SIGUIN IGUALS I SINO TIRAR ERROR

        progressDialog.setTitle("Registering...");
        progressDialog.show();
        getFerrisfyer().getWebSocketManager().send(WebSocketCapsule.getRegisterJson(binding.username.getText().toString(), binding.password.getText().toString()));
    }
}
