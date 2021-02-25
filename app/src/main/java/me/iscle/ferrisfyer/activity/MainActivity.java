package me.iscle.ferrisfyer.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import me.iscle.ferrisfyer.Ferrisfyer;
import me.iscle.ferrisfyer.DeviceControlActivity;
import me.iscle.ferrisfyer.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_REMOTE_LOGIN = 1;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.localDeviceButton.setOnClickListener(v -> {
            getFerrisfyer().setMode(Ferrisfyer.Mode.LOCAL);
            Intent mainActivityIntent = new Intent(MainActivity.this, DeviceControlActivity.class);
            startActivity(mainActivityIntent);
            finishAffinity();
        });

        binding.remoteDeviceButton.setOnClickListener(v -> {
            getFerrisfyer().setMode(Ferrisfyer.Mode.REMOTE);
            Intent loginActivityIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(loginActivityIntent, REQUEST_REMOTE_LOGIN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_REMOTE_LOGIN) {
            if (resultCode == RESULT_OK) {
                Intent i = new Intent(this, DeviceControlActivity.class);
                startActivity(i);
                finishAffinity();
            }
        }
    }
}
