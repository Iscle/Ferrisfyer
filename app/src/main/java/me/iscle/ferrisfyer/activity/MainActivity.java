package me.iscle.ferrisfyer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_CHOOSE_BT_DEVICE = 65538;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, R.string.error_bluetooth_required, Toast.LENGTH_LONG).show();
                finish();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
